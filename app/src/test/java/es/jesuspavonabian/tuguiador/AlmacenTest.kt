package es.jesuspavonabian.tuguiador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AlmacenTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private fun archivo() = File(carpeta.root, "recorridos.json")

    @Test
    fun sinArchivoDevuelveListaVacia() {
        val resultado = Almacen(carpeta.root).cargar()
        assertEquals(Almacen.Resultado.Lista(emptyList()), resultado)
    }

    @Test
    fun guardarYCargar() {
        val almacen = Almacen(carpeta.root)
        val lista = listOf(Recorrido(0, "A", "B", listOf("1", "2")))
        almacen.guardar(lista)
        val cargado = (almacen.cargar() as Almacen.Resultado.Lista).recorridos
        assertEquals(lista, cargado)
        assertEquals(listOf("1", "2"), cargado[0].pasos)
        assertFalse(File(carpeta.root, "recorridos.json.tmp").exists())
    }

    @Test
    fun archivoCorruptoQuedaIntactoYSeCopia() {
        val basura = "[{esto no es json"
        archivo().writeText(basura)
        val resultado = Almacen(carpeta.root).cargar()
        assertEquals(Almacen.Resultado.Ilegible, resultado)
        assertEquals(basura, archivo().readText())
        assertEquals(basura, File(carpeta.root, "recorridos.json.corrupto").readText())
    }

    @Test
    fun unaSegundaCorrupcionNoPisaLaCopiaAnterior() {
        archivo().writeText("primero")
        Almacen(carpeta.root).cargar()
        archivo().writeText("segundo")
        Almacen(carpeta.root).cargar()
        assertEquals("primero", File(carpeta.root, "recorridos.json.corrupto").readText())
        assertEquals("segundo", File(carpeta.root, "recorridos.json.corrupto.1").readText())
    }

    @Test
    fun cargaElFormatoDeIos() {
        val ios = javaClass.getResourceAsStream("/recorridos_ios.json")!!.readBytes()
        archivo().writeBytes(ios)
        val lista = (Almacen(carpeta.root).cargar() as Almacen.Resultado.Lista).recorridos
        assertEquals(2, lista.size)
        assertTrue(lista[0].nombre.isNotEmpty())
    }
}
