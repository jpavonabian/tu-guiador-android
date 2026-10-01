package es.jesuspavonabian.tuguiador.vistas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import es.jesuspavonabian.tuguiador.Controlador
import es.jesuspavonabian.tuguiador.R
import es.jesuspavonabian.tuguiador.Recorrido

@Composable
fun EditarRecorrido(
    controlador: Controlador,
    id: Int,
    onCerrar: () -> Unit,
) {
    // Se lee una sola vez: guardar reindexa los identificadores.
    val original = remember(id) { controlador.obtenerRecorridoPorID(id) }

    LaunchedEffect(original == null) {
        if (original == null) onCerrar()
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
