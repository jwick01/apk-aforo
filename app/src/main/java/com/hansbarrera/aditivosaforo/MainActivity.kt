package com.hansbarrera.aditivosaforo

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import java.util.Locale
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hansbarrera.aditivosaforo.ui.screens.AcercaDeScreen
import com.hansbarrera.aditivosaforo.ui.screens.AditivoScreen
import com.hansbarrera.aditivosaforo.ui.screens.AforoScreen
import com.hansbarrera.aditivosaforo.ui.screens.GuiadoScreen
import com.hansbarrera.aditivosaforo.ui.screens.HistorialScreen
import com.hansbarrera.aditivosaforo.ui.screens.HomeScreen
import com.hansbarrera.aditivosaforo.ui.screens.PotenciometroScreen
import com.hansbarrera.aditivosaforo.ui.screens.RendimientoScreen
import com.hansbarrera.aditivosaforo.ui.screens.VerificacionScreen
import com.hansbarrera.aditivosaforo.ui.theme.AditivosAforoTheme

private data class Destino(val ruta: String, val etiqueta: Int, val emoji: String)

// Barra inferior: Inicio, formulario libre e historial. Las calculadoras, el
// asistente guiado y "Acerca de" se abren desde la pantalla de Inicio.
private val destinos = listOf(
    Destino("inicio", R.string.nav_inicio, "🏠"),
    Destino("aforo", R.string.nav_aforo, "📋"),
    Destino("historial", R.string.nav_historial, "🗂")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val appState = remember { AppState(context) }
    val darkTheme = appState.temaOscuro.value ?: isSystemInDarkTheme()

    val idioma = appState.idioma.value
    val contextoLocalizado = remember(idioma) {
        if (idioma == null) {
            context
        } else {
            val config = Configuration(context.resources.configuration)
            config.setLocale(Locale(idioma))
            context.createConfigurationContext(config)
        }
    }

    CompositionLocalProvider(LocalContext provides contextoLocalizado) {
        AditivosAforoTheme(darkTheme = darkTheme) {
            Surface(modifier = Modifier) {
                AppNavigation(appState)
            }
        }
    }
}

@Composable
private fun AppNavigation(appState: AppState) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentEntry?.destination?.route

                destinos.forEach { destino ->
                    NavigationBarItem(
                        selected = currentRoute == destino.ruta,
                        onClick = {
                            navController.navigate(destino.ruta) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(destino.emoji) },
                        label = { Text(stringResource(destino.etiqueta)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Navegación a un ítem de la barra: conserva/restaura el estado de cada pestaña.
        fun navegarABarra(ruta: String) {
            navController.navigate(ruta) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        NavHost(
            navController = navController,
            startDestination = "inicio",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("inicio") {
                HomeScreen(
                    appState = appState,
                    onNuevoGuiado = { navController.navigate("guiado") },
                    onContinuarBorrador = { navegarABarra("aforo") },
                    onNavigate = { ruta -> navController.navigate(ruta) },
                    onAcerca = { navController.navigate("acerca") }
                )
            }
            composable("aforo") { AforoScreen(appState) }
            composable("historial") {
                HistorialScreen(appState, onEditarRegistro = { navegarABarra("aforo") })
            }
            composable("guiado") {
                GuiadoScreen(appState, onFinalizar = {
                    navController.popBackStack("inicio", inclusive = false)
                })
            }
            composable("rendimiento") { RendimientoScreen(appState) }
            composable("aditivo") { AditivoScreen(appState) }
            composable("verificacion") { VerificacionScreen(appState) }
            composable("potenciometro") { PotenciometroScreen(appState) }
            composable("acerca") { AcercaDeScreen(appState) }
        }
    }
}
