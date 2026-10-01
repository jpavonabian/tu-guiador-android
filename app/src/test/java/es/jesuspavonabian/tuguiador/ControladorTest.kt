package es.jesuspavonabian.tuguiador

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ControladorTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private fun nuevo() = Controlador(Almacen(carpeta.root), Dispatchers.Unconfined)

    private fun enDisco(): List<Recorrido> =
        (Almacen(carpeta.root).cargar() as Almacen.Resultado.Lista).recorridos

    @Test
    fun añadirAsignaIdentificadoresPorIndiceYGuarda() {
        val c = nuevo()
        c.añadirRecorrido(Recorrido(99, "A", "a", listOf("1")))
        c.añadirRecorrido(Recorrido(99, "B", "b", emptyList()))
        assertEquals(listOf(0, 1), c.recorridos.value.map { it.id })
        assertEquals(listOf(0, 1), enDisco().map { it.id })
        assertEquals(listOf("A", "B"), enDisco().map { it.nombre })
    }

    @Test
    fun eliminarReindexa() {
        val c = nuevo()
        listOf("A", "B", "C").forEach { c.añadirRecorrido(Recorrido(0, it, "", emptyList())) }
        c.eliminarRecorrido(c.obtenerRecorridoPorID(1)!!)
        assertEquals(listOf("A", "C"), c.recorridos.value.map { it.nombre })
        assertEquals(listOf(0, 1), enDisco().map { it.id })
    }

    @Test
    fun editarSoloLosPasosTambienSeGuarda() {
        val c = nuevo()
        c.añadirRecorrido(Recorrido(0, "A", "a", listOf("viejo")))
        val original = c.obtenerRecorridoPorID(0)!!
        c.editarRecorrido(original, original.copy(pasos = listOf("nuevo")))
        assertEquals(listOf("nuevo"), enDisco()[0].pasos)
    }

    @Test
    fun importarNuevoLoAñade() = runBlocking {
        val c = nuevo()
        val r = c.importarRecorrido("""{"id":5,"nombre":"N","descripcion":"D","pasos":["x"]}""")
        assertNotNull(r)
        assertEquals(1, c.recorridos.value.size)
        assertEquals(0, c.recorridos.value[0].id)
        assertEquals(1, enDisco().size)
    }

    @Test
    fun importarDuplicadoNoDuplicaYCuentaComoExito() = runBlocking {
        val c = nuevo()
        c.añadirRecorrido(Recorrido(0, "N", "D", listOf("original")))
        val r = c.importarRecorrido("""{"id":0,"nombre":"N","descripcion":"D","pasos":["otro"]}""")
        assertNotNull(r)
        assertEquals(listOf("original"), r!!.pasos)
        assertEquals(1, c.recorridos.value.size)
    }

    @Test
    fun importarJsonInvalidoDevuelveNull() = runBlocking {
        val c = nuevo()
        assertNull(c.importarRecorrido("esto no es json"))
        assertNull(c.importarRecorrido(""))
        assertNull(c.importarRecorrido("""[{"id":0,"nombre":"N","descripcion":"D","pasos":[]}]"""))
        assertEquals(0, c.recorridos.value.size)
    }

    @Test
    fun obtenerJsonDeUnRecorridoEsUnObjeto() {
        val json = nuevo().obtenerJSONRecorrido(Recorrido(0, "N", "D", listOf("p")))
        assertEquals("""{"id":0,"nombre":"N","descripcion":"D","pasos":["p"]}""", json)
    }

    @Test
    fun archivoCorruptoNoSeSobrescribeHastaQueElUsuarioCambieAlgo() {
        val basura = "[{no es json"
        File(carpeta.root, "recorridos.json").writeText(basura)
        val c = nuevo()
        assertEquals(0, c.recorridos.value.size)
        assertEquals(basura, File(carpeta.root, "recorridos.json").readText())
        assertEquals(basura, File(carpeta.root, "recorridos.json.corrupto").readText())
        c.añadirRecorrido(Recorrido(0, "A", "a", emptyList()))
        assertEquals(1, enDisco().size)
        assertEquals(basura, File(carpeta.root, "recorridos.json.corrupto").readText())
    }

    @Test
    fun cargaLosRecorridosExistentes() {
        Almacen(carpeta.root).guardar(listOf(Recorrido(0, "A", "a", listOf("1"))))
        assertEquals(listOf("A"), nuevo().recorridos.value.map { it.nombre })
    }
}
