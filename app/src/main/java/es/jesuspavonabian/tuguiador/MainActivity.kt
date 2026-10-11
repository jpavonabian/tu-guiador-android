// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Jesús Pavón Abián

package es.jesuspavonabian.tuguiador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.jesuspavonabian.tuguiador.ui.theme.TemaTuGuiador
import es.jesuspavonabian.tuguiador.vistas.EditarRecorrido
import es.jesuspavonabian.tuguiador.vistas.ListaRecorridos
import es.jesuspavonabian.tuguiador.vistas.NuevoRecorrido
import es.jesuspavonabian.tuguiador.vistas.RecorridoDetalles

class MainActivity : ComponentActivity() {
    private val controlador: Controlador by viewModels {
        viewModelFactory { initializer { Controlador(Almacen(filesDir)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TemaTuGuiador {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navegador = rememberNavController()
                    NavHost(navController = navegador, startDestination = "lista") {
                        composable("lista") {
                            ListaRecorridos(
                                controlador = controlador,
                                onAbrir = { navegador.navigate("detalle/${it.id}") },
                                onAnadir = { navegador.navigate("nuevo") },
                                onEditar = { navegador.navigate("editar/${it.id}") },
                            )
                        }
                        composable("nuevo") { entrada ->
                            NuevoRecorrido(
                                controlador = controlador,
                                onCerrar = { navegador.salirDe(entrada) },
                            )
                        }
                        composable(
                            route = "editar/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType }),
                        ) { entrada ->
                            EditarRecorrido(
                                controlador = controlador,
                                id = entrada.arguments!!.getInt("id"),
                                onCerrar = { navegador.salirDe(entrada) },
                            )
                        }
                        composable(
                            route = "detalle/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType }),
                        ) { entrada ->
                            RecorridoDetalles(
                                controlador = controlador,
                                id = entrada.arguments!!.getInt("id"),
                                onAtras = { navegador.salirDe(entrada) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// Un segundo toque (o una segunda llamada) sobre una pantalla que ya se está cerrando no debe
// sacar también la pantalla de debajo: dejaría la pila vacía y la app en blanco.
private fun NavController.salirDe(entrada: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entrada.id) popBackStack()
}
