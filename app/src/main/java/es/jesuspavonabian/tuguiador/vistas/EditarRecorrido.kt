// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Jesús Pavón Abián

package es.jesuspavonabian.tuguiador.vistas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jesuspavonabian.tuguiador.Controlador
import es.jesuspavonabian.tuguiador.R
import es.jesuspavonabian.tuguiador.Recorrido

@Composable
fun EditarRecorrido(
    controlador: Controlador,
    id: Int,
    onCerrar: () -> Unit,
) {
    val cargado by controlador.cargado.collectAsStateWithLifecycle()

    // Se lee una sola vez, ya cargado el archivo: guardar reindexa los identificadores.
    val original = remember(id, cargado) { controlador.obtenerRecorridoPorID(id) }

    // Mientras no se haya cargado el archivo, no se sabe si el recorrido existe.
    LaunchedEffect(cargado, original == null) {
        if (cargado && original == null) onCerrar()
    }
    if (original == null) return

    FormularioRecorrido(
        inicial = original,
        textoGuardar = stringResource(R.string.guardar_recorrido),
        onGuardar = { nombre, descripcion, pasos ->
            // Como en iOS, editar recorta espacios y descarta pasos vacíos.
            val editado = Recorrido(
                id = original.id,
                nombre = nombre.trim(),
                descripcion = descripcion.trim(),
                pasos = pasos.filter { it.trim().isNotEmpty() },
            )
            controlador.editarRecorrido(original, editado)
            onCerrar()
        },
        onCerrar = onCerrar,
    )
}
