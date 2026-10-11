// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Jesús Pavón Abián

package es.jesuspavonabian.tuguiador.vistas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.jesuspavonabian.tuguiador.Controlador
import es.jesuspavonabian.tuguiador.R
import es.jesuspavonabian.tuguiador.Recorrido

@Composable
fun NuevoRecorrido(
    controlador: Controlador,
    onCerrar: () -> Unit,
) {
    FormularioRecorrido(
        inicial = null,
        textoGuardar = stringResource(R.string.anadir_recorrido),
        onGuardar = { nombre, descripcion, pasos ->
            controlador.añadirRecorrido(Recorrido(0, nombre, descripcion, pasos))
            onCerrar()
        },
        onCerrar = onCerrar,
    )
}

// Formulario común de Nuevo y Editar. Atrás equivale a "Cerrar" y pide confirmar si hay cambios.
@Composable
internal fun FormularioRecorrido(
    inicial: Recorrido?,
    textoGuardar: String,
    onGuardar: (String, String, List<String>) -> Unit,
    onCerrar: () -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf(inicial?.nombre ?: "") }
    var descripcion by rememberSaveable { mutableStateOf(inicial?.descripcion ?: "") }
    // Siempre se sustituye por una lista nueva (ArrayList, que Android sabe guardar al girar).
    var pasos by rememberSaveable { mutableStateOf<List<String>>(ArrayList(inicial?.pasos ?: emptyList())) }
    var confirmarDescartar by rememberSaveable { mutableStateOf(false) }
    var pasoAEnfocar by remember { mutableStateOf<Int?>(null) }
    // Un segundo toque en Guardar no debe guardar otra vez (duplicaría el recorrido).
    var guardado by remember { mutableStateOf(false) }
    val botonAnadirPaso = remember { FocusRequester() }

    val hayCambios = nombre != (inicial?.nombre ?: "") ||
        descripcion != (inicial?.descripcion ?: "") ||
        pasos != (inicial?.pasos ?: emptyList<String>())

    fun cerrar() {
        if (hayCambios) confirmarDescartar = true else onCerrar()
    }

    BackHandler { cerrar() }

    Scaffold(modifier = Modifier.fillMaxSize()) { margen ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(margen)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = { cerrar() }) {
                Text(stringResource(R.string.cerrar))
            }
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(stringResource(R.string.nombre_del_recorrido)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text(stringResource(R.string.descripcion_del_recorrido)) },
                modifier = Modifier.fillMaxWidth(),
            )
            pasos.forEachIndexed { indice, paso ->
                val enfocador = remember { FocusRequester() }
                LaunchedEffect(pasoAEnfocar) {
                    if (pasoAEnfocar == indice) {
                        enfocador.requestFocus()
                        pasoAEnfocar = null
                    }
                }
                val descripcionEliminar = stringResource(R.string.eliminar_paso, indice + 1)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    OutlinedTextField(
                        value = paso,
                        onValueChange = { nuevo ->
                            pasos = pasos.mapIndexed { i, anterior -> if (i == indice) nuevo else anterior }
                        },
                        label = { Text(stringResource(R.string.paso_numero, indice + 1)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(enfocador),
                    )
                    TextButton(
                        onClick = {
                            pasos = pasos.filterIndexed { i, _ -> i != indice }
                            // El foco queda en el paso anterior, o en el siguiente si era el primero.
                            if (pasos.isEmpty()) {
                                botonAnadirPaso.requestFocus()
                            } else {
                                pasoAEnfocar = maxOf(indice - 1, 0)
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = descripcionEliminar },
                    ) {
                        Text(stringResource(R.string.eliminar))
                    }
                }
            }
            TextButton(
                onClick = {
                    pasos = pasos + ""
                    pasoAEnfocar = pasos.size - 1
                },
                modifier = Modifier.focusRequester(botonAnadirPaso),
            ) {
                Text(stringResource(R.string.anadir_paso))
            }
            Button(
                onClick = {
                    if (!guardado) {
                        guardado = true
                        onGuardar(nombre, descripcion, pasos)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(textoGuardar)
            }
        }
    }

    if (confirmarDescartar) {
        AlertDialog(
            onDismissRequest = { confirmarDescartar = false },
            title = { Text(stringResource(R.string.descartar_titulo)) },
            text = { Text(stringResource(R.string.descartar_mensaje)) },
            confirmButton = {
                TextButton(onClick = onCerrar) {
                    Text(stringResource(R.string.descartar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDescartar = false }) {
                    Text(stringResource(R.string.seguir_editando))
                }
            },
        )
    }
}
