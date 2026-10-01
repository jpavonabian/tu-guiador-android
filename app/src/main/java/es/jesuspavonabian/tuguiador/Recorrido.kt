package es.jesuspavonabian.tuguiador

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Mismo formato que la app iOS: los recorridos se comparten como JSON entre ambas.
val jsonRecorridos = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class Recorrido(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val pasos: List<String>,
) {
    // Igualdad solo por nombre y descripción, como en iOS (no la de data class).
    override fun equals(other: Any?): Boolean =
        other is Recorrido && nombre == other.nombre && descripcion == other.descripcion

    override fun hashCode(): Int = 31 * nombre.hashCode() + descripcion.hashCode()
}
