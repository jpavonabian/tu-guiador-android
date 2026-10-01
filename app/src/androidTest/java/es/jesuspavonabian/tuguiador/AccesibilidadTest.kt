// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Jesús Pavón Abián

package es.jesuspavonabian.tuguiador

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import es.jesuspavonabian.tuguiador.ui.theme.TemaTuGuiador
import es.jesuspavonabian.tuguiador.vistas.ListaRecorridos
import es.jesuspavonabian.tuguiador.vistas.NuevoRecorrido
import es.jesuspavonabian.tuguiador.vistas.RecorridoDetalles
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

// Comprueba la semántica que lee TalkBack. No sustituye a la prueba manual con TalkBack real.
class AccesibilidadTest {
    @get:Rule
    val regla = createAndroidComposeRule<ComponentActivity>()

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var carpeta: File
    private lateinit var controlador: Controlador

    private fun texto(id: Int) = contexto.getString(id)

    @Before
    fun preparar() {
        carpeta = File(contexto.cacheDir, "prueba-${System.nanoTime()}").also { it.mkdirs() }
        controlador = Controlador(Almacen(carpeta), Dispatchers.Unconfined)
        controlador.añadirRecorrido(Recorrido(0, "Ruta A", "Descripción A", listOf("Primer paso", "Segundo paso")))
        controlador.añadirRecorrido(Recorrido(0, "Ruta B", "Descripción B", emptyList()))
    }

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    private fun mostrarLista() = regla.setContent {
        TemaTuGuiador {
            ListaRecorridos(controlador = controlador, onAbrir = {}, onAnadir = {}, onEditar = {})
        }
    }

    @Test
    fun lista_laCabeceraEsUnEncabezado() {
        mostrarLista()
        regla.onNodeWithText(texto(R.string.listado_de_recorridos))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun lista_cadaFilaEsUnNodoConAccionesEditarEliminarCompartir() {
        mostrarLista()
        val esperadas = listOf(texto(R.string.editar), texto(R.string.eliminar), texto(R.string.compartir))
        regla.onNodeWithText("Ruta A").assert(
            SemanticsMatcher("acciones personalizadas $esperadas") { nodo ->
                nodo.config.getOrNull(SemanticsActions.CustomActions)?.map { it.label } == esperadas
            },
        )
        regla.onNodeWithText("Ruta B").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun lista_losBotonesTienenAlMenos48dp() {
        mostrarLista()
        listOf(R.string.anadir_un_recorrido, R.string.importar_desde_portapapeles).forEach {
            regla.onNodeWithText(texto(it)).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
        regla.onNodeWithText("Ruta A").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun lista_eliminarPorAccionAccesibleAbreDialogoYBorra() {
        mostrarLista()
        val etiqueta = texto(R.string.eliminar)
        regla.runOnUiThread {
            val nodo = regla.onNodeWithText("Ruta A").fetchSemanticsNode()
            nodo.config[SemanticsActions.CustomActions].first { it.label == etiqueta }.action()
        }
        regla.onNodeWithText(texto(R.string.confirmar_eliminar_recorrido)).assertIsDisplayed()
        regla.onNode(hasText(etiqueta) and hasClickAction()).performClick()
        regla.onNodeWithText("Ruta A").assertDoesNotExist()
        regla.onNodeWithText("Ruta B").assertIsDisplayed()
    }

    @Test
    fun detalle_nombreEsEncabezadoYHayBotonCompartir() {
        regla.setContent {
            TemaTuGuiador { RecorridoDetalles(controlador = controlador, id = 0, onAtras = {}) }
        }
        regla.onNodeWithText("Ruta A").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        regla.onNodeWithText("Descripción A").assertIsDisplayed()
        regla.onNodeWithText("Primer paso").assertIsDisplayed()
        regla.onNodeWithText("Segundo paso").assertIsDisplayed()
        regla.onNodeWithText(texto(R.string.atras)).assertHeightIsAtLeast(48.dp)
        regla.onNodeWithText(texto(R.string.compartir)).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun formulario_cadaPasoTieneBotonEliminarConNombreUnico() {
        regla.setContent { TemaTuGuiador { NuevoRecorrido(controlador = controlador, onCerrar = {}) } }
        regla.onNodeWithText(texto(R.string.anadir_paso)).performClick()
        regla.onNodeWithText(texto(R.string.anadir_paso)).performClick()
        regla.onNodeWithContentDescription(contexto.getString(R.string.eliminar_paso, 1)).assertIsDisplayed()
        regla.onNodeWithContentDescription(contexto.getString(R.string.eliminar_paso, 2)).assertIsDisplayed()
        // Nombre, descripción y dos pasos.
        regla.onAllNodes(hasSetTextAction()).assertCountEquals(4)
    }

    @Test
    fun formulario_quitarUnPasoReindexaLosNombres() {
        regla.setContent { TemaTuGuiador { NuevoRecorrido(controlador = controlador, onCerrar = {}) } }
        repeat(3) { regla.onNodeWithText(texto(R.string.anadir_paso)).performClick() }
        regla.onNodeWithContentDescription(contexto.getString(R.string.eliminar_paso, 2)).performClick()
        regla.onNodeWithContentDescription(contexto.getString(R.string.eliminar_paso, 2)).assertIsDisplayed()
        regla.onNodeWithContentDescription(contexto.getString(R.string.eliminar_paso, 3)).assertDoesNotExist()
        regla.onAllNodes(hasSetTextAction()).assertCountEquals(4)
    }

    @Test
    fun formulario_atrasSinCambiosCierraDirectamente() {
        var cerrado = false
        regla.setContent { TemaTuGuiador { NuevoRecorrido(controlador = controlador, onCerrar = { cerrado = true }) } }
        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        regla.waitForIdle()
        assertTrue(cerrado)
    }

    @Test
    fun formulario_atrasConCambiosPideConfirmarYNoCierra() {
        var cerrado = false
        regla.setContent { TemaTuGuiador { NuevoRecorrido(controlador = controlador, onCerrar = { cerrado = true }) } }
        regla.onAllNodes(hasSetTextAction())[0].performTextInput("Algo")
        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        regla.waitForIdle()
        regla.onNodeWithText(texto(R.string.descartar_titulo)).assertIsDisplayed()
        assertFalse(cerrado)

        regla.onNodeWithText(texto(R.string.seguir_editando)).performClick()
        regla.onNodeWithText(texto(R.string.descartar_titulo)).assertDoesNotExist()
        assertFalse(cerrado)

        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        regla.waitForIdle()
        regla.onNodeWithText(texto(R.string.descartar)).performClick()
        assertTrue(cerrado)
    }

    @Test
    fun formulario_guardarAñadeElRecorridoTalCualComoEnIos() {
        regla.setContent { TemaTuGuiador { NuevoRecorrido(controlador = controlador, onCerrar = {}) } }
        regla.onAllNodes(hasSetTextAction())[0].performTextInput("Nueva")
        regla.onNodeWithText(texto(R.string.anadir_recorrido)).performClick()
        regla.waitForIdle()
        assertEquals(listOf("Ruta A", "Ruta B", "Nueva"), controlador.recorridos.value.map { it.nombre })
    }
}
