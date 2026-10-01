package es.jesuspavonabian.tuguiador

import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// Único punto de lectura y escritura de recorridos.json.
class Almacen(private val directorio: File) {
    private val archivo = File(directorio, "recorridos.json")

    sealed interface Resultado {
        data class Lista(val recorridos: List<Recorrido>) : Resultado

        // El archivo existe pero no se pudo leer o no es válido. Se deja intacto y se guarda una copia.
        data object Ilegible : Resultado
    }

    fun cargar(): Resultado {
        if (!archivo.exists()) return Resultado.Lista(emptyList())
        return try {
            val texto = archivo.readText(Charsets.UTF_8)
            Resultado.Lista(jsonRecorridos.decodeFromString(ListSerializer(Recorrido.serializer()), texto))
        } catch (e: Exception) {
            guardarCopiaCorrupta()
            Resultado.Ilegible
        }
    }

    // Escribe a un temporal y lo renombra, para no dejar un JSON a medias si falla.
    @Throws(IOException::class)
    fun guardar(recorridos: List<Recorrido>) {
        val temporal = File(directorio, "recorridos.json.tmp")
        temporal.writeText(
            jsonRecorridos.encodeToString(ListSerializer(Recorrido.serializer()), recorridos),
            Charsets.UTF_8,
        )
        Files.move(
            temporal.toPath(),
            archivo.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun guardarCopiaCorrupta() {
        try {
            var copia = File(directorio, "recorridos.json.corrupto")
            var n = 1
            while (copia.exists()) {
                copia = File(directorio, "recorridos.json.corrupto.$n")
                n++
            }
            archivo.copyTo(copia)
        } catch (e: Exception) {
            // Si ni la copia se puede hacer, el original sigue intacto.
        }
    }
}
