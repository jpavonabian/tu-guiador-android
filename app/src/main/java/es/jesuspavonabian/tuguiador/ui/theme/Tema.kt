// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Jesús Pavón Abián

package es.jesuspavonabian.tuguiador.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun TemaTuGuiador(
    modoOscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Colores dinámicos desde Android 12; esquema por defecto en Android 10 y 11.
    val esquema = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val contexto = LocalContext.current
            if (modoOscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
        }
        modoOscuro -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = esquema, content = content)
}
