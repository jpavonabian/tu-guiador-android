// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Jesús Pavón Abián

package es.jesuspavonabian.tuguiador.vistas

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import es.jesuspavonabian.tuguiador.Controlador
import es.jesuspavonabian.tuguiador.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccionesImportacion(
    val importarPortapapeles: () -> Unit,
    val importarArchivo: () -> Unit,
    val exportarTodos: () -> Unit,
)

// Registra el portapapeles y el selector de archivos, y muestra el diálogo con el resultado
// (éxito o error, como en iOS). Los botones los pone la pantalla que lo usa.
@Composable
fun rememberAccionesImportacion(controlador: Controlador): AccionesImportacion {
    val contexto = LocalContext.current
    val ambito = rememberCoroutineScope()
    var titulo by rememberSaveable { mutableStateOf<Int?>(null) }
    var mensaje by rememberSaveable { mutableStateOf<Int?>(null) }

    fun avisar(tituloRes: Int, mensajeRes: Int) {
        titulo = tituloRes
        mensaje = mensajeRes
    }

    val selectorImportar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            ambito.launch {
                val texto = leerTexto(contexto, uri)
                if (texto != null && controlador.importarArchivo(texto)) {
                    avisar(R.string.exito_importar_titulo, R.string.exito_importar_archivo_mensaje)
                } else {
                    avisar(R.string.error_importar_titulo, R.string.error_importar_archivo_mensaje)
                }
            }
        }
    }

    val selectorExportar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            ambito.launch {
                if (escribirTexto(contexto, uri, controlador.obtenerJSONTodos())) {
                    avisar(R.string.exito_exportar_titulo, R.string.exito_exportar_mensaje)
                } else {
                    avisar(R.string.error_exportar_titulo, R.string.error_exportar_mensaje)
                }
            }
        }
    }

    val acciones = remember(controlador) {
        AccionesImportacion(
            importarPortapapeles = {
                ambito.launch {
                    val json = textoDelPortapapeles(contexto)
                    // Portapapeles vacío cuenta como error, igual que en iOS.
                    if (json != null && controlador.importarRecorrido(json) != null) {
                        avisar(R.string.exito_importar_titulo, R.string.exito_importar_mensaje)
                    } else {
                        avisar(R.string.error_importar_titulo, R.string.error_importar_mensaje)
                    }
                }
            },
            importarArchivo = { selectorImportar.launch(arrayOf("*/*")) },
            exportarTodos = { selectorExportar.launch("recorridos.json") },
        )
    }

    val tituloActual = titulo
    val mensajeActual = mensaje
    if (tituloActual != null && mensajeActual != null) {
        AlertDialog(
            onDismissRequest = {
                titulo = null
                mensaje = null
            },
            title = { Text(stringResource(tituloActual)) },
            text = { Text(stringResource(mensajeActual)) },
            confirmButton = {
                TextButton(onClick = {
                    titulo = null
                    mensaje = null
                }) {
                    Text(stringResource(R.string.aceptar))
                }
            },
        )
    }
    return acciones
}

private fun textoDelPortapapeles(contexto: Context): String? {
    val portapapeles = contexto.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = portapapeles.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(contexto)?.toString()
}

private suspend fun leerTexto(contexto: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        contexto.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (e: Exception) {
        null
    }
}

private suspend fun escribirTexto(contexto: Context, uri: Uri, texto: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val salida = contexto.contentResolver.openOutputStream(uri, "wt") ?: return@withContext false
            salida.use { it.write(texto.toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: Exception) {
            false
        }
    }
