package com.hansbarrera.aditivosaforo

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hansbarrera.aditivosaforo.ui.screens.AcercaDeScreen
import com.hansbarrera.aditivosaforo.ui.screens.AditivoScreen
import com.hansbarrera.aditivosaforo.ui.screens.PotenciometroScreen
import com.hansbarrera.aditivosaforo.ui.screens.RendimientoScreen
import com.hansbarrera.aditivosaforo.ui.screens.VerificacionScreen
import com.hansbarrera.aditivosaforo.ui.theme.AditivosAforoTheme

private data class Destino(val ruta: String, val etiqueta: String, val emoji: String)

private val destinos = listOf(
    Destino("rendimiento", "Rendimiento", "⚙"),
    Destino("aditivo", "Aditivo", "🧪"),
    Destino("verificacion", "Verificación", "📊"),
    Destino("potenciometro", "Potenciómetro", "🎛"),
    Destino("acerca", "Acerca de", "ℹ")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AditivosAforoTheme {
                Surface(modifier = Modifier) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    val appState = remember { AppState() }

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
                        label = { Text(destino.etiqueta) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "rendimiento",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("rendimiento") { RendimientoScreen(appState) }
            composable("aditivo") { AditivoScreen(appState) }
            composable("verificacion") { VerificacionScreen(appState) }
            composable("potenciometro") { PotenciometroScreen(appState) }
            composable("acerca") { AcercaDeScreen() }
        }
    }
}
