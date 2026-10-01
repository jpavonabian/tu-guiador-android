// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Jesús Pavón Abián

package es.jesuspavonabian.tuguiador

import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RecorridoTest {
    private fun leerFixture(): String =
        javaClass.getResourceAsStream("/recorridos_ios.json")!!.readBytes().toString(Charsets.UTF_8)

    @Test
    fun igualdadYHashSoloPorNombreYDescripcion() {
        val a = Recorrido(0, "Ruta", "Desc", listOf("uno"))
        val b = Recorrido(7, "Ruta", "Desc", listOf("otro", "más"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, Recorrido(0, "Ruta", "Otra", listOf("uno")))
        assertNotEquals(a, Recorrido(0, "Otra", "Desc", listOf("uno")))
    }

    @Test
    fun decodificaJsonEstiloIos() {
        val lista = jsonRecorridos.decodeFromString(ListSerializer(Recorrido.serializer()), leerFixture())
        assertEquals(2, lista.size)
        assertEquals("Salida por la puerta principal/lateral", lista[0].descripcion)
        assertEquals(listOf("Sal por la puerta", "Gira a la derecha", "Cuenta tres cruces"), lista[0].pasos)
        assertEquals(emptyList<String>(), lista[1].pasos)
        assertEquals(1, lista[1].id)
    }

    @Test
    fun idaYVueltaConservaTodosLosCampos() {
        val original = listOf(Recorrido(0, "Ñandú ✓", "Con \"comillas\" y\nsalto", listOf("a", "")))
        val serializador = ListSerializer(Recorrido.serializer())
        val vuelta = jsonRecorridos.decodeFromString(serializador, jsonRecorridos.encodeToString(serializador, original))
        assertEquals(original, vuelta)
        assertEquals(original[0].id, vuelta[0].id)
        assertEquals(original[0].pasos, vuelta[0].pasos)
    }

    @Test
    fun ignoraCamposDesconocidos() {
        val json = """{"id":3,"nombre":"N","descripcion":"D","pasos":["x"],"campoFuturo":{"a":1}}"""
        val r = jsonRecorridos.decodeFromString(Recorrido.serializer(), json)
        assertEquals(3, r.id)
        assertEquals(listOf("x"), r.pasos)
    }

    @Test
    fun unRecorridoSeCodificaComoObjetoConLosNombresDeIos() {
        val json = jsonRecorridos.encodeToString(Recorrido.serializer(), Recorrido(2, "N", "D", listOf("p")))
        assertEquals("""{"id":2,"nombre":"N","descripcion":"D","pasos":["p"]}""", json)
    }
}
