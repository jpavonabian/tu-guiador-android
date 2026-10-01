package es.jesuspavonabian.tuguiador.vistas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.jesuspavonabian.tuguiador.Controlador
import es.jesuspavonabian.tuguiador.R

@Composable
fun RecorridoDetalles(
    controlador: Controlador,
    id: Int,
    onAtras: () -> Unit,
) {
    val recorridos by controlador.recorridos.collectAsStateWithLifecycle()
    val recorrido = recorridos.firstOrNull { it.id == id }
    val contexto = LocalContext.current

    // Si el recorrido ya no existe, se vuelve a la lista.
    LaunchedEffect(recorrido == null) {
        if (recorrido == null) onAtras()
    }
    if (recorrido == null) return

    Scaffold(modifier = Modifier.fillMaxSize()) { margen ->
        // Todo dentro del scroll: con fuente grande o en horizontal, un botón fijo abajo
        // dejaría casi sin sitio al contenido. El orden de lectura no cambia.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(margen)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onAtras,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.atras))
            }
            Text(
                text = recorrido.nombre,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { heading() },
            )
            Text(
                text = recorrido.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            recorrido.pasos.forEach { paso ->
                Text(
                    text = paso,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Button(
                onClick = { compartirRecorrido(contexto, controlador.obtenerJSONRecorrido(recorrido)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.compartir))
            }
        }
    }
}
