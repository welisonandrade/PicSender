package com.example.picsender.ui.theme

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

// ---------- ESQUEMA ESCURO (recomendado para o app) ----------
private val DarkColorScheme = darkColorScheme(
    primary = PSGreen,
    onPrimary = PSBlack,     // texto/ícone sobre o botão verde
    secondary = PSGreen,
    onSecondary = PSBlack,
    background = PSBlack,    // plano de fundo geral
    onBackground = PSWhite,
    surface = PSBlack,       // superfícies (cards, Scaffold)
    onSurface = PSWhite,
    outline = PSGray
)

// ---------- ESQUEMA CLARO (se o usuário forçar tema claro) ----------
private val LightColorScheme = lightColorScheme(
    primary = PSGreen,
    onPrimary = PSBlack,
    secondary = PSGreen,
    onSecondary = PSBlack,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    outline = Color(0xFFCCCCCC)
)

/**
 * PicSenderTheme:
 * - Usa a paleta preto+verde por padrão (dynamicColor = false).
 * - Se quiser testar Material You, mude dynamicColor para true.
 * - darkTheme: se não passar, segue o sistema do aparelho.
 */
@Composable
fun PicSenderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // <- FORÇAMOS NOSSA PALETA POR PADRÃO
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Material You (opcional). Vai ignorar parte da nossa paleta.
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // use o Typography do template
        content = content
    )
}
