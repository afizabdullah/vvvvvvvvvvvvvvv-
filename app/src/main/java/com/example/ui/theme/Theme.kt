package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFFCF6679),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFA0A0A0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce look
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = AppDarkColorScheme, typography = Typography, content = content)
}
