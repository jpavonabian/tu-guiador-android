package es.jesuspavonabian.tuguiador.vistas

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jesuspavonabian.tuguiador.Controlador
import es.jesuspavonabian.tuguiador.R
import es.jesuspavonabian.tuguiador.Recorrido

fun compartirRecorrido(contexto: Context, json: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, json)
    }
    contexto.startActivity(Intent.createChooser(envio, contexto.getString(R.string.compartir_recorrido)))
}

@Composable
fun ListaRecorridos(
    controlador: Controlador,
    onAbrir: (Recorrido) -> Unit,
    onAnadir: () -> Unit,
    onEditar: (Recorrido) -> Unit,
) {
    val recorridos by controlador.recorridos.collectAsStateWithLifecycle()
    var idAEliminar by rememberSaveable { mutableStateOf<Int?>(null) }
    val acciones = rememberAccionesImportacion(controlador)

    Scaffold(modifier = Modifier.fillMaxSize()) { margen ->
        if (recorridos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margen)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.no_hay_recorridos),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.anadir_o_importar_recorrido),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onAnadir) {
                    Text(stringResource(R.string.anadir_un_recorrido))
                }
                Button(onClick = acciones.importarPortapapeles) {
                    Text(stringResource(R.string.importar_desde_portapapeles))
                }
                Button(onClick = acciones.importarArchivo) {
                    Text(stringResource(R.string.importar_archivo))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(margen),
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.listado_de_recorridos),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(R.string.mantener_pulsado_para_opciones),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onAnadir) {
                            Text(stringResource(R.string.anadir_un_recorrido))
                        }
                        Button(onClick = acciones.importarPortapapeles) {
                            Text(stringResource(R.string.importar_desde_portapapeles))
                        }
                    }
                }
                items(recorridos, key = { it.id }) { recorrido ->
                    FilaRecorrido(
                        recorrido = recorrido,
                        json = controlador.obtenerJSONRecorrido(recorrido),
                        onAbrir = { onAbrir(recorrido) },
                        onEditar = { onEditar(recorrido) },
                        onEliminar = { idAEliminar = recorrido.id },
                    )
                }
                item {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = acciones.exportarTodos) {
                            Text(stringResource(R.string.exportar_todos))
                        }
                        Button(onClick = acciones.importarArchivo) {
                            Text(stringResource(R.string.importar_archivo))
                        }
                    }
                }
            }
        }
    }

    val aEliminar = recorridos.firstOrNull { it.id == idAEliminar }
    if (aEliminar != null) {
        AlertDialog(
            onDismissRequest = { idAEliminar = null },
            title = { Text(stringResource(R.string.eliminar_recorrido)) },
            text = { Text(stringResource(R.string.confirmar_eliminar_recorrido)) },
            confirmButton = {
                TextButton(onClick = {
                    controlador.eliminarRecorrido(aEliminar)
                    idAEliminar = null
                }) {
                    Text(stringResource(R.string.eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { idAEliminar = null }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilaRecorrido(
    recorrido: Recorrido,
    json: String,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
) {
    val contexto = LocalContext.current
    var menuAbierto by remember { mutableStateOf(false) }
    val etiquetaEditar = stringResource(R.string.editar)
    val etiquetaEliminar = stringResource(R.string.eliminar)
    val etiquetaCompartir = stringResource(R.string.compartir)

    Box {
        Text(
            text = recorrido.nombre,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onAbrir,
                    onLongClickLabel = stringResource(R.string.ver_opciones),
                    onLongClick = { menuAbierto = true },
                )
                .semantics(mergeDescendants = true) {
                    customActions = listOf(
                        CustomAccessibilityAction(etiquetaEditar) {
                            onEditar()
                            true
                        },
                        CustomAccessibilityAction(etiquetaEliminar) {
                            onEliminar()
                            true
                        },
                        CustomAccessibilityAction(etiquetaCompartir) {
                            compartirRecorrido(contexto, json)
                            true
                        },
                    )
                }
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
            DropdownMenuItem(
                text = { Text(etiquetaEditar) },
                onClick = {
                    menuAbierto = false
                    onEditar()
                },
            )
            DropdownMenuItem(
                text = { Text(etiquetaCompartir) },
                onClick = {
                    menuAbierto = false
                    compartirRecorrido(contexto, json)
                },
            )
            DropdownMenuItem(
                text = { Text(etiquetaEliminar) },
                onClick = {
                    menuAbierto = false
                    onEliminar()
                },
            )
        }
    }
}
