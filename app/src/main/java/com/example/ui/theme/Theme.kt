package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BentoColorScheme =
  darkColorScheme(
    primary = BentoLavenderPrimary,
    onPrimary = BentoPurpleDeep,
    primaryContainer = BentoPurpleDeep,
    onPrimaryContainer = BentoLavenderPrimary,
    secondary = BentoLavenderPrimary,
    onSecondary = BentoPurpleDeep,
    secondaryContainer = BentoBadgeGray,
    onSecondaryContainer = BentoTextPrimary,
    tertiary = BentoGreenActive,
    onTertiary = BentoPurpleDeep,
    background = BentoBackground,
    onBackground = BentoTextPrimary,
    surface = BentoCardSurface,
    onSurface = BentoTextPrimary,
    surfaceVariant = BentoNavSurface,
    onSurfaceVariant = BentoTextSecondary,
    outline = BentoBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = BentoColorScheme,
    typography = Typography,
    content = content
  )
}

