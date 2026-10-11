// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Jesús Pavón Abián

package es.jesuspavonabian.tuguiador

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException

class Controlador(
    private val almacen: Almacen,
    private val despachador: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _recorridos = MutableStateFlow<List<Recorrido>>(emptyList())
    val recorridos: StateFlow<List<Recorrido>> = _recorridos

    // Pasa a true cuando termina la carga inicial. Hasta entonces la lista vacía no significa
    // que un recorrido no exista (p. ej. al restaurar la app tras morir el proceso).
    private val _cargado = MutableStateFlow(false)
    val cargado: StateFlow<Boolean> = _cargado

    private val ambito = CoroutineScope(SupervisorJob() + despachador)

    // Un solo cambio a la vez, y la carga va siempre la primera (el Mutex es justo):
    // así nunca se guarda nada encima de un archivo que aún no se ha leído.
    private val cerrojo = Mutex()

    init {
        ambito.launch {
            cerrojo.withLock {
                when (val resultado = almacen.cargar()) {
                    is Almacen.Resultado.Lista -> _recorridos.value = resultado.recorridos
                    // Lista vacía sin tocar el archivo hasta que el usuario cambie algo.
                    Almacen.Resultado.Ilegible -> _recorridos.value = emptyList()
                }
                _cargado.value = true
            }
        }
    }

    fun añadirRecorrido(nuevoRecorrido: Recorrido) {
        ambito.launch { modificar { it + nuevoRecorrido } }
    }

    fun eliminarRecorrido(recorrido: Recorrido) {
        ambito.launch { modificar { lista -> lista.filter { it.id != recorrido.id } } }
    }

    fun editarRecorrido(recorridoAEditar: Recorrido, nuevoRecorrido: Recorrido) {
        ambito.launch {
            modificar { lista ->
                lista.map { if (it.id == recorridoAEditar.id) nuevoRecorrido else it }
            }
        }
    }

    fun obtenerRecorridoPorID(id: Int): Recorrido? = _recorridos.value.firstOrNull { it.id == id }

    fun obtenerJSONRecorrido(recorrido: Recorrido): String =
        jsonRecorridos.encodeToString(Recorrido.serializer(), recorrido)

    // Devuelve el recorrido (el ya existente si hay uno con mismo nombre y descripción)
    // o null si el JSON no es válido.
    suspend fun importarRecorrido(json: String): Recorrido? {
        val importado = try {
            jsonRecorridos.decodeFromString(Recorrido.serializer(), json)
        } catch (e: Exception) {
            return null
        }
        return withContext(despachador) {
            cerrojo.withLock {
                val existente = _recorridos.value.firstOrNull { it == importado }
                existente ?: run {
                    aplicar(_recorridos.value + importado)
                    _recorridos.value.last()
                }
            }
        }
    }

    fun obtenerJSONTodos(): String =
        jsonRecorridos.encodeToString(ListSerializer(Recorrido.serializer()), _recorridos.value)

    // Importa un archivo de recorridos: un array (como recorridos.json) o un solo recorrido.
    // Añade los que no existan ya (mismo nombre y descripción). Si no es válido, no toca nada.
    suspend fun importarArchivo(json: String): Boolean {
        val importados = try {
            jsonRecorridos.decodeFromString(ListSerializer(Recorrido.serializer()), json)
        } catch (e: Exception) {
            return importarRecorrido(json) != null
        }
        withContext(despachador) {
            cerrojo.withLock {
                val actuales = _recorridos.value
                val nuevos = importados.filter { it !in actuales }.distinct()
                if (nuevos.isNotEmpty()) aplicar(actuales + nuevos)
            }
        }
        return true
    }

    fun asignarIdentificadoresUnicos(lista: List<Recorrido>): List<Recorrido> =
        lista.mapIndexed { indice, recorrido -> recorrido.copy(id = indice) }

    private suspend fun modificar(cambio: (List<Recorrido>) -> List<Recorrido>) {
        cerrojo.withLock { aplicar(cambio(_recorridos.value)) }
    }

    // Se llama con el cerrojo tomado.
    private fun aplicar(lista: List<Recorrido>) {
        val nueva = asignarIdentificadoresUnicos(lista)
        _recorridos.value = nueva
        try {
            almacen.guardar(nueva)
        } catch (e: IOException) {
            // Como en iOS, un fallo al guardar no se notifica (pendiente de decidir).
        }
    }

    override fun onCleared() {
        ambito.cancel()
    }
}
