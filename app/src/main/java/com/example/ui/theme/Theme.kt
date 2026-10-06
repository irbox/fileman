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

enum class DarkThemeStyle(val displayName: String) {
    SLATE("Deep Slate"),
    AMOLED("AMOLED Pure Black"),
    CYBER("Midnight Cyber")
}

enum class AccentChoice(val displayName: String, val primary: Color, val secondary: Color) {
    CYAN("Electric Cyan", LibreCyanLight, LibreCyan),
    EMERALD("Libre Emerald", LibreEmeraldLight, LibreEmerald),
    INDIGO("Neon Indigo", LibreIndigoLight, LibreIndigo),
    AMBER("Amber Gold", LibreAmberLight, LibreAmber),
    ROSE("Sakura Rose", LibreRoseLight, LibreRose),
    VIOLET("Royal Violet", LibreVioletLight, LibreViolet),
    SUNSET("Sunset Blaze", LibreSunsetLight, LibreSunset),
    TEAL("Deep Teal", LibreTealLight, LibreTeal)
}

fun getLightColorScheme(accent: AccentChoice) = lightColorScheme(
    primary = accent.secondary,
    onPrimary = Color.White,
    primaryContainer = accent.primary.copy(alpha = 0.25f),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = accent.primary,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

fun getDarkColorScheme(style: DarkThemeStyle, accent: AccentChoice) = when (style) {
    DarkThemeStyle.SLATE -> darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.Black,
        primaryContainer = accent.secondary,
        onPrimaryContainer = Color.White,
        secondary = accent.secondary,
        onSecondary = Color.White,
        background = Slate900,
        surface = Slate800,
        surfaceVariant = Slate700,
        onBackground = Slate100,
        onSurface = Slate100,
        onSurfaceVariant = Slate300,
        outline = Slate600
    )
    DarkThemeStyle.AMOLED -> darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.Black,
        primaryContainer = accent.secondary,
        onPrimaryContainer = Color.White,
        secondary = accent.secondary,
        onSecondary = Color.White,
        background = AmoledBlack,
        surface = AmoledSurface,
        surfaceVariant = AmoledSurfaceVariant,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFB0B0B0),
        outline = Color(0xFF2C2C2C)
    )
    DarkThemeStyle.CYBER -> darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.Black,
        primaryContainer = CyberVariant,
        onPrimaryContainer = Color.White,
        secondary = accent.secondary,
        onSecondary = Color.White,
        background = CyberNavy,
        surface = CyberSurface,
        surfaceVariant = CyberVariant,
        onBackground = Color(0xFFE2E8F0),
        onSurface = Color(0xFFE2E8F0),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF475569)
    )
}

val LibreLightColorScheme = lightColorScheme(
    primary = LibreCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = LibreIndigo,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun LibreFilesTheme(
    darkTheme: Boolean = true,
    darkThemeStyle: DarkThemeStyle = DarkThemeStyle.SLATE,
    accentChoice: AccentChoice = AccentChoice.CYAN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(darkThemeStyle, accentChoice)
        else -> getLightColorScheme(accentChoice)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias for template references
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    LibreFilesTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
