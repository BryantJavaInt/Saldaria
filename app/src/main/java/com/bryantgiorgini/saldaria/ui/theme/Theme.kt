package com.bryantgiorgini.saldaria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class SaldariaCustomColors(
    val capital: Color,
    val interest: Color,
    val activeFill: Color
)

val LocalSaldariaCustomColors = staticCompositionLocalOf {
    SaldariaCustomColors(
        capital = Color.Unspecified,
        interest = Color.Unspecified,
        activeFill = Color.Unspecified
    )
}

val MaterialTheme.customColors: SaldariaCustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSaldariaCustomColors.current

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = DarkSecondary,
    outline = DarkLines,
    outlineVariant = DarkLines,
    primary = DarkCapital,
    onPrimary = DarkBackground,
    secondaryContainer = DarkActiveFill,
    onSecondaryContainer = DarkText,
    surfaceVariant = DarkSurface,
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightInk,
    onSurface = LightInk,
    onSurfaceVariant = LightSecondary,
    outline = LightLines,
    outlineVariant = LightLines,
    primary = LightInk,
    onPrimary = Color.White,
    secondaryContainer = LightActiveFill,
    onSecondaryContainer = LightInk,
    surfaceVariant = LightSurface,
)

val SaldariaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp)
)

@Composable
fun SaldariaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val customColors = if (darkTheme) {
        SaldariaCustomColors(
            capital = DarkCapital,
            interest = DarkInterest,
            activeFill = DarkActiveFill
        )
    } else {
        SaldariaCustomColors(
            capital = LightCapital,
            interest = LightInterest,
            activeFill = LightActiveFill
        )
    }

    CompositionLocalProvider(LocalSaldariaCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaldariaTypography,
            shapes = SaldariaShapes,
            content = content
        )
    }
}