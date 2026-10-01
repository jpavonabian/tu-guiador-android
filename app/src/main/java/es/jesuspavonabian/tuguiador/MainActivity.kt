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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.jesuspavonabian.tuguiador.ui.theme.TemaTuGuiador
import es.jesuspavonabian.tuguiador.vistas.ListaRecorridos
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
                            )
                        }
                        composable(
                            route = "detalle/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType }),
                        ) { entrada ->
                            RecorridoDetalles(
                                controlador = controlador,
                                id = entrada.arguments!!.getInt("id"),
                                onAtras = { navegador.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
