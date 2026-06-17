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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFF7F2FA)
                    ) {
                        AccountCheckerScreen()
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

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("مدقق المحترف", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D1B20)) },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color(0xFF49454F))
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color(0xFF49454F))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF3EDF7)
                    ),
                    modifier = Modifier.background(Color(0xFFF3EDF7))
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF3EDF7),
                    contentColor = Color(0xFF6750A4),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF6750A4)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("المدقق", color = if (selectedTab == 0) Color(0xFF6750A4) else Color(0xFF1D1B20)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("أدوات ذكية", color = if (selectedTab == 1) Color(0xFF6750A4) else Color(0xFF1D1B20)) }
                    )
                }
            }
        },
        bottomBar = {
            if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3EDF7))
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            .background(Color(0xFFE8DEF8), CircleShape)
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start", tint = Color(0xFF1D192B))
                    }
                    Text("بدء", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D192B))
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
                    Text(if (state.isPaused) "►" else "II", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Text(if (state.isPaused) "متابعة" else "إيقاف مؤقت", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF49454F))
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
                    Text("■", fontSize = 24.sp, color = Color(0xFF49454F), modifier = Modifier.padding(bottom = 2.dp))
                    Text("إيقاف", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF49454F))
                }
            } // end Row
            } // end if
        },
        containerColor = Color(0xFFF7F2FA)
    ) { paddingValues ->
        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            // Counters Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatItem(
                    label = "ناجح", value = state.stats.hit,
                    bg = Color(0xFFF0FDF4), labelColor = Color(0xFF15803D), valColor = Color(0xFF14532D),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "فاشل", value = state.stats.bad,
                    bg = Color(0xFFFEF2F2), labelColor = Color(0xFFB91C1C), valColor = Color(0xFF7F1D1D),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "إعادة", value = state.stats.retry,
                    bg = Color(0xFFFFFBEB), labelColor = Color(0xFFB45309), valColor = Color(0xFF78350F),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "مجهول", value = state.stats.unknown,
                    bg = Color(0xFFF9FAFB), labelColor = Color(0xFF6B7280), valColor = Color(0xFF1F2937),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "الإجمالي", value = state.stats.total,
                    bg = Color(0xFFEFF6FF), labelColor = Color(0xFF1D4ED8), valColor = Color(0xFF1E3A8A),
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
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF1D1B20)),
                            singleLine = true,
                            enabled = !state.isRunning,
                            cursorBrush = SolidColor(Color(0xFF6750A4)),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF79747E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEADDFF))
                                .clickable(enabled = state.loginUrl.isNotEmpty() && !state.isRunning) {
                                    coroutineScope.launch {
                                        val result = viewModel.testConnection()
                                        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text("اختبار", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF21005D))
                        }
                    }
                    // Floating Label
                    Text(
                        "رابط تسجيل الدخول المستهدف",
                        fontSize = 12.sp,
                        color = Color(0xFF6750A4),
                        modifier = Modifier
                            .absoluteOffset(x = 12.dp, y = (-8).dp)
                            .background(Color(0xFFF7F2FA))
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
                            .border(1.dp, Color(0xFF79747E), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !state.isRunning) { fileLauncher.launch(arrayOf("text/plain")) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.selectedFileUri == null) "اختيار ملف TXT" else state.selectedFileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D1B20),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    var sepExpanded by remember { mutableStateOf(false) }
                    val separators = listOf(":", ";", "Tab", "|", "#")

                    ExposedDropdownMenuBox(
                        expanded = sepExpanded,
                        onExpandedChange = { if (!state.isRunning) sepExpanded = !sepExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF79747E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .menuAnchor(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الفاصل: ( ${state.separator} )", fontSize = 14.sp, color = Color(0xFF1D1B20))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color(0xFF49454F))
                        }
                        ExposedDropdownMenu(
                            expanded = sepExpanded,
                            onDismissRequest = { sepExpanded = false }
                        ) {
                            separators.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        viewModel.updateSeparator(selectionOption)
                                        sepExpanded = false
                                    }
                                )
                            }
                        }
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
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF79747E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التأخير", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                        BasicTextField(
                            value = state.delaySeconds.toString(),
                            onValueChange = { 
                                val parsed = it.toIntOrNull()
                                if (parsed != null) viewModel.updateDelay(parsed)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF1D1B20)),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            enabled = !state.isRunning
                        )
                        Text("ثانية", fontSize = 12.sp, color = Color.LightGray)
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
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF79747E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .menuAnchor(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عدد الخيوط", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                            Text(state.threadCount.toString(), fontSize = 14.sp, color = Color(0xFF1D1B20), modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color(0xFF49454F))
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
                    .background(Color(0xFF1C1B1F))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D2C31))
                        .border(width = 1.dp, color = Color(0xFF49454F))
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
                    }
                }

                // Logs list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
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
                }
            } // end Live Log Container Column
            } // end outer Column
        } else {
            SmartToolsScreen(viewModel, paddingValues)
        }
    } // end Scaffold trailing lambda
} // end AccountCheckerScreen

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
        
        Text("المولد الذكي للكروت", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
        
        OutlinedTextField(
            value = genState.exampleCards,
            onValueChange = viewModel::updateExampleCards,
            label = { Text("أدخل 3 أمثلة للكروت (كل كرت في سطر)") },
            modifier = Modifier.fillMaxWidth().background(Color.White),
            minLines = 4
        )
        
        OutlinedTextField(
            value = genState.count,
            onValueChange = viewModel::updateGenCount,
            label = { Text("عدد الكروت") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().background(Color.White)
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
            modifier = Modifier.fillMaxWidth().background(Color.White),
            minLines = 4
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        Text("الفاحص التلقائي مع متصفح", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
        
        OutlinedTextField(
            value = webState.cardsToCheck,
            onValueChange = viewModel::updateWebCards,
            label = { Text("الكروت المراد فحصها") },
            modifier = Modifier.fillMaxWidth().background(Color.White),
            minLines = 4,
            enabled = !webState.isChecking
        )
        
        OutlinedTextField(
            value = webState.checkUrl,
            onValueChange = viewModel::updateWebUrl,
            label = { Text("رابط الفحص (مثال: http://site/check)") },
            modifier = Modifier.fillMaxWidth().background(Color.White),
            singleLine = true,
            enabled = !webState.isChecking
        )
        
        OutlinedTextField(
            value = webState.delaySeconds,
            onValueChange = viewModel::updateWebDelay,
            label = { Text("التأخير (ثواني)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().background(Color.White),
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
            Text("الحالي: ${if (webState.currentCard.isNotEmpty()) webState.currentCard else "---"}", fontSize = 14.sp)
            Text("العدد: ${webState.currentIndex} / ${webState.totalCount}", fontSize = 14.sp)
        }
        
        var webViewRef1 by remember { mutableStateOf<WebView?>(null) }
        var webViewRef2 by remember { mutableStateOf<WebView?>(null) }
        
        Row(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text("صفحة 1 (تسجيل الدخول)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            webViewRef1 = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF6750A4), RoundedCornerShape(8.dp))
                )
            }
            
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text("صفحة 2 (التحقق)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            webViewRef2 = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF6750A4), RoundedCornerShape(8.dp))
                )
            }
        }
        
        LaunchedEffect(webState.currentCard, webState.isChecking) {
            if (webState.isChecking && webState.currentCard.isNotEmpty() && webState.currentCard != "تم الانتهاء من الفحص.") {
                val base = webState.checkUrl
                val url1 = if (base.contains("?")) "$base&username=${webState.currentCard}" else "$base?username=${webState.currentCard}"
                val url2 = if (base.contains("?")) "$base&username=${webState.currentCard}&mode=check" else "$base?username=${webState.currentCard}&mode=check"
                webViewRef1?.loadUrl(url1)
                webViewRef2?.loadUrl(url2)
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

