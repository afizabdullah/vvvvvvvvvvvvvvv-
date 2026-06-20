package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CheckerViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            val isDarkTheme = isSystemInDarkTheme()
            val primColor = try { Color(android.graphics.Color.parseColor(state.primaryColorHex)) } catch (e: Exception) { Color(0xFF6750A4) }
            val colorScheme = if (isDarkTheme) darkColorScheme(primary = primColor) else lightColorScheme(primary = primColor)
            
            MaterialTheme(colorScheme = colorScheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            AccountCheckerScreen(viewModel)
                        }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCheckerScreen(viewModel: CheckerViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            var name = "ملف غير معروف"
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
            viewModel.fileSelected(it, name)
        }
    }

    var currentScreen by remember { mutableStateOf("checker") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    "مدقق المحترف", 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(24.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                NavigationDrawerItem(
                    label = { Text("المدقق") },
                    selected = currentScreen == "checker",
                    onClick = { 
                        currentScreen = "checker"
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("قسم WiFi") },
                    selected = currentScreen == "smart",
                    onClick = { 
                        currentScreen = "smart"
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("مدقق growfollows.com") },
                    selected = currentScreen == "growfollows",
                    onClick = { 
                        currentScreen = "growfollows"
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("مدقق www.repeat.gg") },
                    selected = currentScreen == "repeat",
                    onClick = { 
                        currentScreen = "repeat"
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("مدقق OTP") },
                    selected = currentScreen == "otp",
                    onClick = { 
                        currentScreen = "otp"
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("الإعدادات") },
                    selected = currentScreen == "settings",
                    onClick = { 
                        currentScreen = "settings"
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("حول المطور") },
                    selected = currentScreen == "about",
                    onClick = { 
                        currentScreen = "about"
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("مدقق المحترف", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                        actions = {
                            IconButton(onClick = { }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    )
                }
            },
            bottomBar = {
                if (currentScreen == "checker") {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // START
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !state.isRunning && state.selectedFileUri != null && state.loginUrl.isNotEmpty()) {
                            viewModel.startChecking(context.contentResolver)
                        }
                        .alpha(if (!state.isRunning && state.selectedFileUri != null && state.loginUrl.isNotEmpty()) 1f else 0.4f)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF4CAF50), CircleShape)
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start", tint = Color.White)
                    }
                    Text("بدء", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C), modifier = Modifier.padding(top = 4.dp))
                }

                // PAUSE/RESUME
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = state.isRunning) {
                            if (state.isPaused) viewModel.resumeChecking() else viewModel.pauseChecking()
                        }
                        .alpha(if (state.isRunning) 1f else 0.4f)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF9800), CircleShape)
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (state.isPaused) "►" else "II", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(if (state.isPaused) "متابعة" else "إيقاف مؤقت", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00), modifier = Modifier.padding(top = 4.dp))
                }

                // STOP
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = state.isRunning) {
                            viewModel.stopChecking()
                        }
                        .alpha(if (state.isRunning) 1f else 0.4f)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF44336), CircleShape)
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("■", fontSize = 18.sp, color = Color.White)
                    }
                    Text("إيقاف", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), modifier = Modifier.padding(top = 4.dp))
                }
            } // end Row
            } // end if
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (currentScreen == "checker") {
            var delayInputText by remember(state.delaySeconds) { mutableStateOf(state.delaySeconds.toString()) }
            var separatorInputText by remember(state.separator) { mutableStateOf(state.separator) }
            var isFullScreenLog by remember { mutableStateOf(false) }
            
            BackHandler(enabled = isFullScreenLog) {
                isFullScreenLog = false
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            
            if (!isFullScreenLog) {
            // Counters Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatItem(
                    label = "ناجح", value = state.stats.hit,
                    bg = Color(0xFF1B5E20).copy(alpha = 0.3f), labelColor = Color(0xFF4CAF50), valColor = Color(0xFF81C784),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "فاشل", value = state.stats.bad,
                    bg = Color(0xFFB71C1C).copy(alpha = 0.3f), labelColor = Color(0xFFF44336), valColor = Color(0xFFE57373),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "إعادة", value = state.stats.retry,
                    bg = Color(0xFFE65100).copy(alpha = 0.3f), labelColor = Color(0xFFFF9800), valColor = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "مجهول", value = state.stats.unknown,
                    bg = Color(0xFF424242).copy(alpha = 0.5f), labelColor = Color(0xFFBDBDBD), valColor = Color(0xFFE0E0E0),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "الإجمالي", value = state.stats.total,
                    bg = Color(0xFF0D47A1).copy(alpha = 0.3f), labelColor = Color(0xFF2196F3), valColor = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f)
                )
            }

            // Configuration Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Target Login URL
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = state.loginUrl,
                            onValueChange = viewModel::updateUrl,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            enabled = !state.isRunning,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(enabled = state.loginUrl.isNotEmpty() && !state.isRunning) {
                                    coroutineScope.launch {
                                        val result = viewModel.testConnection()
                                        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text("اختبار", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    // Floating Label
                    Text(
                        "رابط الموقِع المستهدف",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .absoluteOffset(x = 12.dp, y = (-8).dp)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 4.dp)
                    )
                }

                // Pick TXT & Separator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !state.isRunning) { fileLauncher.launch(arrayOf("text/plain")) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.selectedFileUri == null) "اختيار ملف TXT" else state.selectedFileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الفاصل:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 4.dp))
                        BasicTextField(
                            value = separatorInputText,
                            onValueChange = {
                                separatorInputText = it
                                viewModel.updateSeparator(it)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            enabled = !state.isRunning
                        )
                    }
                }

                // Delay & Threads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Delay Input
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التأخير", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                        BasicTextField(
                            value = delayInputText,
                            onValueChange = { 
                                delayInputText = it
                                val parsed = it.toIntOrNull()
                                if (parsed != null) viewModel.updateDelay(parsed)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            enabled = !state.isRunning
                        )
                        Text("ثانية", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Threads Input
                    var threadsExpanded by remember { mutableStateOf(false) }
                    val threadOptions = (1..20).toList()

                    ExposedDropdownMenuBox(
                        expanded = threadsExpanded,
                        onExpandedChange = { if (!state.isRunning) threadsExpanded = !threadsExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .menuAnchor(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عدد الخيوط", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                            Text(state.threadCount.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ExposedDropdownMenu(
                            expanded = threadsExpanded,
                            onDismissRequest = { threadsExpanded = false }
                        ) {
                            threadOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption.toString()) },
                                    onClick = {
                                        viewModel.updateThreadCount(selectionOption)
                                        threadsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            } // end isFullScreenLog

            // Live Log Container
            val listState = rememberLazyListState()
            LaunchedEffect(state.logs.size) {
                if (state.logs.isNotEmpty()) {
                    listState.animateScrollToItem(state.logs.size - 1)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "سجل العمليات الحي",
                        fontSize = 10.sp,
                        color = Color(0xFFE6E1E5),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (state.isPaused) Color(0xFFFF9800) else Color(0xFF4CAF50), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (state.isPaused) "متوقف مؤقتا" else "قيد التشغيل",
                                fontSize = 10.sp,
                                color = if (state.isPaused) Color(0xFFFF9800) else Color(0xFF4CAF50)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("جاهز", fontSize = 10.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { isFullScreenLog = !isFullScreenLog },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullScreenLog) Icons.Filled.Close else Icons.Filled.KeyboardArrowUp,
                                contentDescription = if (isFullScreenLog) "تصغير" else "تكبير",
                                tint = Color(0xFFE6E1E5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Live Logs and Live View Tabs
                var logTab by remember { mutableIntStateOf(1) } // Default to browser view
                
                TabRow(
                    selectedTabIndex = logTab,
                    containerColor = Color(0xFF1C1B1F),
                    contentColor = Color(0xFFD0BCFF),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[logTab]),
                            color = Color(0xFFD0BCFF)
                        )
                    }
                ) {
                    Tab(
                        selected = logTab == 0,
                        onClick = { logTab = 0 },
                        text = { Text("سجل العمليات", fontSize = 12.sp, color = if (logTab == 0) Color(0xFFD0BCFF) else Color(0xFF938F99)) }
                    )
                    Tab(
                        selected = logTab == 1,
                        onClick = { logTab = 1 },
                        text = { Text("المتصفح الحي", fontSize = 12.sp, color = if (logTab == 1) Color(0xFFD0BCFF) else Color(0xFF938F99)) }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Logs list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .alpha(if (logTab == 0) 1f else 0f)
                    ) {
                        items(state.logs, key = { it.id }) { log ->
                            val color = when (log.status) {
                                "SUCCESS" -> Color(0xFF4CAF50)
                                "FAILED" -> Color(0xFFF44336)
                                "ERROR" -> Color(0xFFE91E63)
                                "RETRY" -> Color(0xFFFF9800)
                                "INFO" -> Color(0xFF2196F3)
                                else -> Color.Gray
                            }
                            val actionText = when(log.status) {
                                "SUCCESS" -> "HIT"
                                "FAILED" -> "BAD"
                                "ERROR" -> "ERR"
                                "RETRY" -> "RET"
                                "INFO" -> "SYS"
                                else -> "UNK"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "[${log.timestamp}]",
                                    color = Color(0xFF938F99),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(72.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$actionText:",
                                    color = color,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(36.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = log.info,
                                    color = Color(0xFFE6E1E5),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (state.isRunning && !state.isPaused) {
                            item {
                                Text(
                                    text = "في انتظار الدفعة التالية...",
                                    color = Color(0xFF938F99).copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } // end LazyColumn

                    // Live View WebView
                    var webViewRef by remember { mutableStateOf<WebView?>(null) }
                    
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.setSupportZoom(true)
                                setBackgroundColor(0xFF1C1B1F.toInt())
                                webViewRef = this
                            }
                        },
                        update = { view ->
                            val isDesktop = state.isDesktopMode
                            if (isDesktop) {
                                view.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
                            } else {
                                view.settings.userAgentString = android.webkit.WebSettings.getDefaultUserAgent(context)
                            }
                            view.settings.useWideViewPort = isDesktop
                            view.settings.loadWithOverviewMode = isDesktop

                            view.webViewClient = object : WebViewClient() {
                                override fun onPageFinished(v: WebView, url: String) {
                                    super.onPageFinished(v, url)
                                    val safeUsername = state.currentAccountUsername.replace("'", "\\'")
                                    val safePassword = state.currentAccountPassword.replace("'", "\\'")
                                    val js = """
                                        javascript:(function(){
                                            var inputs = document.getElementsByTagName('input');
                                            for(var i=0; i<inputs.length; i++){
                                                var input = inputs[i];
                                                var n = (input.name || '').toLowerCase();
                                                var t = (input.type || '').toLowerCase();
                                                var id = (input.id || '').toLowerCase();
                                                if(t==='email' || n.indexOf('user')!==-1 || n.indexOf('email')!==-1 || id.indexOf('user')!==-1 || id.indexOf('email')!==-1 || n.indexOf('login')!==-1 || id.indexOf('login')!==-1) {
                                                    if(input.value === '') { input.value = '$safeUsername'; }
                                                }
                                                if(t==='password' || n.indexOf('pass')!==-1 || id.indexOf('pass')!==-1) {
                                                    if(input.value === '') { input.value = '$safePassword'; }
                                                }
                                            }
                                        })();
                                    """.trimIndent()
                                    v.evaluateJavascript(js) {
                                        viewModel.updateState { copy(pageLoadedTrigger = System.currentTimeMillis()) }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .alpha(if (logTab == 1) 1f else 0f)
                    )
                    
                    LaunchedEffect(state.currentIndex) {
                        if (state.isRunning && state.loginUrl.isNotEmpty()) {
                            webViewRef?.loadUrl(state.loginUrl)
                        }
                    }
                    
                    LaunchedEffect(state.testUrlTrigger) {
                        if (state.testUrlTrigger > 0L && state.loginUrl.isNotEmpty()) {
                            webViewRef?.loadUrl(state.loginUrl)
                            logTab = 1 // Switch to the browser tab
                        }
                    }
                }
            } // end Live Log Container Column
            } // end outer Column
        } else if (currentScreen == "smart") {
            SmartToolsScreen(viewModel, paddingValues)
        } else if (currentScreen == "growfollows") {
            GrowFollowsScreen(viewModel, paddingValues)
        } else if (currentScreen == "repeat") {
            RepeatScreen(viewModel, paddingValues)
        } else if (currentScreen == "otp") {
            OtpScreen(viewModel, paddingValues)
        } else if (currentScreen == "settings") {
            SettingsScreen(viewModel, paddingValues)
        } else if (currentScreen == "about") {
            AboutScreen(paddingValues)
        }
    } // end Scaffold trailing lambda
    } // end ModalNavigationDrawer
} // end AccountCheckerScreen

@Composable
fun OtpScreen(viewModel: CheckerViewModel, paddingValues: PaddingValues) {
    val state by viewModel.otpState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("مدقق OTP", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        // Info Cards Layer
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatItem("ناجح", state.hits, Color(0xFF1B5E20).copy(alpha = 0.3f), Color(0xFF4CAF50), Color(0xFF81C784), Modifier.weight(1f))
            StatItem("مرفوض", state.bad, Color(0xFFB71C1C).copy(alpha = 0.3f), Color(0xFFF44336), Color(0xFFE57373), Modifier.weight(1f))
            StatItem("إعادة(OTP)", state.retry, Color(0xFFE65100).copy(alpha = 0.3f), Color(0xFFFF9800), Color(0xFFFFB74D), Modifier.weight(1f))
            StatItem("مجهول", state.unknown, Color(0xFF424242).copy(alpha = 0.5f), Color(0xFFBDBDBD), Color(0xFFE0E0E0), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.accountList,
            onValueChange = { viewModel.updateOtpAccountList(it) },
            label = { Text("البطاقات (CC|MM|YY|CVV)") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            maxLines = 10,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.threadCount,
                onValueChange = { viewModel.updateOtpThreads(it) },
                label = { Text("عدد البوتات (Threads)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (state.isRunning) viewModel.stopOtpChecking() else viewModel.startOtpChecking() 
                },
                modifier = Modifier.height(64.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            ) {
                Text(if (state.isRunning) "إيقاف" else "بدء الفحص")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Text("إعدادات تيليجرام (اختياري)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.botToken,
            onValueChange = { viewModel.updateOtpBotToken(it) },
            label = { Text("Bot Token") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.chatId,
            onValueChange = { viewModel.updateOtpChatId(it) },
            label = { Text("Chat ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("الحالة: ${state.statusText}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Logs
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(state.logs.size) { index ->
                    val log = state.logs[index]
                    val color = when {
                        log.contains("PASSED") -> Color(0xFF4CAF50)
                        log.contains("DECLINED") || log.contains("BAD") -> Color(0xFFF44336)
                        log.contains("OTP") || log.contains("RETRY") -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    }
                    Text(log, fontSize = 12.sp, color = color, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun RepeatScreen(viewModel: CheckerViewModel, paddingValues: PaddingValues) {
    val state by viewModel.repeatState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("مدقق www.repeat.gg", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        // Info Cards Layer
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatItem("ناجح", state.hits, Color(0xFF1B5E20).copy(alpha = 0.3f), Color(0xFF4CAF50), Color(0xFF81C784), Modifier.weight(1f))
            StatItem("فاشل", state.bad, Color(0xFFB71C1C).copy(alpha = 0.3f), Color(0xFFF44336), Color(0xFFE57373), Modifier.weight(1f))
            StatItem("إعادة", state.retry, Color(0xFFE65100).copy(alpha = 0.3f), Color(0xFFFF9800), Color(0xFFFFB74D), Modifier.weight(1f))
            StatItem("مجهول", state.unknown, Color(0xFF424242).copy(alpha = 0.5f), Color(0xFFBDBDBD), Color(0xFFE0E0E0), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.accountList,
            onValueChange = { viewModel.updateRepeatAccountList(it) },
            label = { Text("الحسابات (EMAIL:PASS)") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            maxLines = 10,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.threadCount,
                onValueChange = { viewModel.updateRepeatThreads(it) },
                label = { Text("عدد البوتات (Threads)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (state.isRunning) viewModel.stopRepeatChecking() else viewModel.startRepeatChecking() 
                },
                modifier = Modifier.height(64.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            ) {
                Text(if (state.isRunning) "إيقاف" else "بدء الفحص")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("الحالة: ${state.statusText}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Logs
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(state.logs.size) { index ->
                    val log = state.logs[index]
                    val color = when {
                        log.contains("HIT") -> Color(0xFF4CAF50)
                        log.contains("BAD") -> Color(0xFFF44336)
                        log.contains("RETRY") -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    }
                    Text(log, fontSize = 12.sp, color = color, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun GrowFollowsScreen(viewModel: CheckerViewModel, paddingValues: PaddingValues) {
    val state by viewModel.growState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("مدقق Growfollows.com", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        // Info Cards Layer
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatItem("ناجح", state.hits, Color(0xFF1B5E20).copy(alpha = 0.3f), Color(0xFF4CAF50), Color(0xFF81C784), Modifier.weight(1f))
            StatItem("فاشل", state.bad, Color(0xFFB71C1C).copy(alpha = 0.3f), Color(0xFFF44336), Color(0xFFE57373), Modifier.weight(1f))
            StatItem("إعادة", state.retry, Color(0xFFE65100).copy(alpha = 0.3f), Color(0xFFFF9800), Color(0xFFFFB74D), Modifier.weight(1f))
            StatItem("مجهول", state.unknown, Color(0xFF424242).copy(alpha = 0.5f), Color(0xFFBDBDBD), Color(0xFFE0E0E0), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.accountList,
            onValueChange = { viewModel.updateGrowAccountList(it) },
            label = { Text("الحسابات (USER:PASS)") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            maxLines = 10,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.threadCount,
                onValueChange = { viewModel.updateGrowThreads(it) },
                label = { Text("عدد البوتات (Threads)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (state.isRunning) viewModel.stopGrowChecking() else viewModel.startGrowChecking() 
                },
                modifier = Modifier.height(64.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            ) {
                Text(if (state.isRunning) "إيقاف" else "بدء الفحص")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("إعدادات تيليجرام (اختياري)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.botToken,
            onValueChange = { viewModel.updateGrowBotToken(it) },
            label = { Text("Bot Token") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.chatId,
            onValueChange = { viewModel.updateGrowChatId(it) },
            label = { Text("Chat ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("الحالة: ${state.statusText}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Logs
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(state.logs.size) { index ->
                    val log = state.logs[index]
                    val color = when {
                        log.contains("HIT") -> Color(0xFF4CAF50)
                        log.contains("BAD") -> Color(0xFFF44336)
                        log.contains("RETRY") -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    }
                    Text(log, fontSize = 12.sp, color = color, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: CheckerViewModel, paddingValues: PaddingValues) {
    val state by viewModel.state.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("الإعدادات", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))
        
        // Desktop Mode Switch
        Row(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.updateState { copy(isDesktopMode = !isDesktopMode) } }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("موقع مصمم للكمبيوتر (Desktop Mode)", fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = state.isDesktopMode,
                onCheckedChange = { viewModel.updateState { copy(isDesktopMode = it) } }
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Theme Colors
        Text("لون التطبيق", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val colors = listOf("#6750A4", "#0088cc", "#25D366", "#E91E63", "#FF9800", "#F44336")
            colors.forEach { colorHex ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                        .clickable { viewModel.updateState { copy(primaryColorHex = colorHex) } }
                        .border(
                            width = if (state.primaryColorHex == colorHex) 4.dp else 0.dp,
                            color = if (state.primaryColorHex == colorHex) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun AboutScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = "Developer",
            modifier = Modifier.size(72.dp),
            tint = Color(0xFF6750A4)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "المطور",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "حافظ العزي",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "تطبيق مدقق المحترف مع التوليد الذكي والأدوات المتقدمة.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/967783799137"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White)
            ) {
                Text("WhatsApp")
            }
            
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/CYBBEEAGLE"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088cc), contentColor = Color.White)
            ) {
                Text("Telegram")
            }
        }
    }
}

@Composable
fun WifiInfoCard() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var ssid by remember { mutableStateOf("جاري التحقق...") }
    var gatewayUrl by remember { mutableStateOf("") }
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val info = wifiManager.connectionInfo
        ssid = info.ssid ?: "غير متصل / غير معروف"
        if (ssid == "<unknown ssid>") {
            ssid = "تتطلب صلاحية الموقع (Location) لتحديد اسم الشبكة"
        }
        
        val dhcp = wifiManager.dhcpInfo
        if (dhcp != null) {
            val gatewayInt = dhcp.gateway
            if (gatewayInt != 0) {
                val ip = "${gatewayInt and 0xFF}.${gatewayInt shr 8 and 0xFF}.${gatewayInt shr 16 and 0xFF}.${gatewayInt shr 24 and 0xFF}"
                gatewayUrl = "http://$ip"
            } else {
                gatewayUrl = "غير متوفر"
            }
        }
    }
    
    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("معلومات شبكة WiFi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("الشبكة الحالية: $ssid", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            if (gatewayUrl.isNotEmpty() && gatewayUrl != "غير متوفر") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("رابط صفحة الراوتر/الشبكة:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(gatewayUrl, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString(gatewayUrl))
                        Toast.makeText(context, "تم النسخ!", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("نسخ", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SmartToolsScreen(viewModel: CheckerViewModel, paddingValues: PaddingValues) {
    val genState by viewModel.genState.collectAsState()
    val webState by viewModel.webState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
        
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        WifiInfoCard()
        
        Text("المولد الذكي للكروت", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        
        OutlinedTextField(
            value = genState.exampleCards,
            onValueChange = viewModel::updateExampleCards,
            label = { Text("أدخل 3 أمثلة للكروت (كل كرت في سطر)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            minLines = 4
        )
        
        OutlinedTextField(
            value = genState.count,
            onValueChange = viewModel::updateGenCount,
            label = { Text("عدد الكروت") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        if (genState.errorMessage != null) {
            Text(genState.errorMessage!!, color = Color.Red, fontSize = 12.sp)
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = viewModel::generateCards, modifier = Modifier.weight(1f)) {
                Text("توليد")
            }
            Button(onClick = {
                clipboardManager.setText(AnnotatedString(genState.generatedCards))
                Toast.makeText(context, "تم النسخ!", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.weight(1f)) {
                Text("نسخ")
            }
        }
        
        Button(onClick = { 
            viewModel.setWebCheckerCards(genState.generatedCards)
            coroutineScope.launch {
                scrollState.animateScrollTo(1000)
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("نقل للفاحص")
        }
        
        OutlinedTextField(
            value = genState.generatedCards,
            onValueChange = {},
            readOnly = true,
            label = { Text("النتائج") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            minLines = 4
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
        
        Text("الفاحص التلقائي مع متصفح", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        
        OutlinedTextField(
            value = webState.cardsToCheck,
            onValueChange = viewModel::updateWebCards,
            label = { Text("الكروت المراد فحصها") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            minLines = 4,
            enabled = !webState.isChecking
        )
        
        OutlinedTextField(
            value = webState.checkUrl,
            onValueChange = viewModel::updateWebUrl,
            label = { Text("رابط الفحص (مثال: http://site/check)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true,
            enabled = !webState.isChecking
        )
        
        OutlinedTextField(
            value = webState.delaySeconds,
            onValueChange = viewModel::updateWebDelay,
            label = { Text("التأخير (ثواني)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            enabled = !webState.isChecking
        )
        
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = viewModel::startWebChecking, enabled = !webState.isChecking, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                Text("بدء")
            }
            Button(onClick = viewModel::stopWebChecking, enabled = webState.isChecking, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("إيقاف")
            }
        }
        
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("الحالي: ${if (webState.currentCard.isNotEmpty()) webState.currentCard else "---"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("العدد: ${webState.currentIndex} / ${webState.totalCount}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        
        var webViewRef by remember { mutableStateOf<WebView?>(null) }
        
        Column(
            modifier = Modifier.fillMaxWidth().height(300.dp)
        ) {
            Text("صفحة ويب تسجيل الدخول والفحص", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        webViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            )
        }
        
        LaunchedEffect(webState.currentCard, webState.isChecking) {
            if (webState.isChecking && webState.currentCard.isNotEmpty() && webState.currentCard != "تم الانتهاء من الفحص.") {
                val base = webState.checkUrl
                val url = if (base.contains("?")) "$base&username=${webState.currentCard}" else "$base?username=${webState.currentCard}"
                webViewRef?.loadUrl(url)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatItem(label: String, value: Int, bg: Color, labelColor: Color, valColor: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(vertical = 4.dp)
    ) {
        Text(text = label.uppercase(), fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Bold)
        Text(text = value.toString(), fontSize = 18.sp, color = valColor, fontWeight = FontWeight.Bold)
    }
}

