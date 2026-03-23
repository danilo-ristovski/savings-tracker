package com.savings.tracker.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary teal
val md_theme_light_primary = Color(0xFF00796B)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFB2DFDB)
val md_theme_light_onPrimaryContainer = Color(0xFF00251E)

// Secondary amber
val md_theme_light_secondary = Color(0xFFFFA000)
val md_theme_light_onSecondary = Color(0xFF000000)
val md_theme_light_secondaryContainer = Color(0xFFFFECB3)
val md_theme_light_onSecondaryContainer = Color(0xFF2E1500)

// Tertiary
val md_theme_light_tertiary = Color(0xFF4DB6AC)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFB2DFDB)
val md_theme_light_onTertiaryContainer = Color(0xFF00251E)

// Error
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)

// Background / Surface
val md_theme_light_background = Color(0xFFFAFDFB)
val md_theme_light_onBackground = Color(0xFF191C1B)
val md_theme_light_surface = Color(0xFFFAFDFB)
val md_theme_light_onSurface = Color(0xFF191C1B)
val md_theme_light_surfaceVariant = Color(0xFFDBE5E1)
val md_theme_light_onSurfaceVariant = Color(0xFF3F4946)
val md_theme_light_outline = Color(0xFF6F7976)
val md_theme_light_outlineVariant = Color(0xFFBFC9C5)
val md_theme_light_inverseSurface = Color(0xFF2E3130)
val md_theme_light_inverseOnSurface = Color(0xFFEFF1EF)
val md_theme_light_inversePrimary = Color(0xFF80CBC4)
val md_theme_light_surfaceTint = Color(0xFF00796B)

// Dark theme primary teal
val md_theme_dark_primary = Color(0xFF80CBC4)
val md_theme_dark_onPrimary = Color(0xFF003731)
val md_theme_dark_primaryContainer = Color(0xFF005048)
val md_theme_dark_onPrimaryContainer = Color(0xFFB2DFDB)

// Dark secondary amber
val md_theme_dark_secondary = Color(0xFFFFCC80)
val md_theme_dark_onSecondary = Color(0xFF4A2800)
val md_theme_dark_secondaryContainer = Color(0xFF6A3B00)
val md_theme_dark_onSecondaryContainer = Color(0xFFFFECB3)

// Dark tertiary
val md_theme_dark_tertiary = Color(0xFF80CBC4)
val md_theme_dark_onTertiary = Color(0xFF003731)
val md_theme_dark_tertiaryContainer = Color(0xFF005048)
val md_theme_dark_onTertiaryContainer = Color(0xFFB2DFDB)

// Dark error
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

// Dark background / surface
val md_theme_dark_background = Color(0xFF191C1B)
val md_theme_dark_onBackground = Color(0xFFE1E3E1)
val md_theme_dark_surface = Color(0xFF191C1B)
val md_theme_dark_onSurface = Color(0xFFE1E3E1)
val md_theme_dark_surfaceVariant = Color(0xFF3F4946)
val md_theme_dark_onSurfaceVariant = Color(0xFFBFC9C5)
val md_theme_dark_outline = Color(0xFF899390)
val md_theme_dark_outlineVariant = Color(0xFF3F4946)
val md_theme_dark_inverseSurface = Color(0xFFE1E3E1)
val md_theme_dark_inverseOnSurface = Color(0xFF2E3130)
val md_theme_dark_inversePrimary = Color(0xFF00796B)
val md_theme_dark_surfaceTint = Color(0xFF80CBC4)

// Savings-specific semantic colors
val savingsGreen = Color(0xFF2E7D32)
val withdrawalRed = Color(0xFFC62828)
val feeOrange = Color(0xFFEF6C00)

val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
)

val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
)
