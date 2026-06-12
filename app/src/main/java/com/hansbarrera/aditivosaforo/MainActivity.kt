package com.hansbarrera.aditivosaforo

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hansbarrera.aditivosaforo.ui.screens.AcercaDeScreen
import com.hansbarrera.aditivosaforo.ui.screens.AditivoScreen
import com.hansbarrera.aditivosaforo.ui.screens.AforoScreen
import com.hansbarrera.aditivosaforo.ui.screens.PotenciometroScreen
import com.hansbarrera.aditivosaforo.ui.screens.RendimientoScreen
import com.hansbarrera.aditivosaforo.ui.screens.VerificacionScreen
import com.hansbarrera.aditivosaforo.ui.theme.AditivosAforoTheme

private data class Destino(val ruta: String, val etiqueta: Int, val emoji: String)

private val destinos = listOf(
    Destino("rendimiento", R.string.nav_rendimiento, "⚙"),
    Destino("aditivo", R.string.nav_aditivo, "🧪"),
    Destino("verificacion", R.string.nav_verificacion, "📊"),
    Destino("potenciometro", R.string.nav_potenciometro, "🎛"),
    Destino("aforo", R.string.nav_aforo, "📋"),
    Destino("acerca", R.string.nav_acerca, "ℹ")
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

    AditivosAforoTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier) {
            AppNavigation(appState)
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
        NavHost(
            navController = navController,
            startDestination = "rendimiento",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("rendimiento") { RendimientoScreen(appState) }
            composable("aditivo") { AditivoScreen(appState) }
            composable("verificacion") { VerificacionScreen(appState) }
            composable("potenciometro") { PotenciometroScreen(appState) }
            composable("aforo") { AforoScreen(appState) }
            composable("acerca") { AcercaDeScreen(appState) }
        }
    }
}
