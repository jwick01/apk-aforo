package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.R
import com.hansbarrera.aditivosaforo.data.AforoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pantalla de inicio: lanza el aforo guiado, permite continuar el borrador
 * pendiente y da acceso rápido a las calculadoras y a los ajustes.
 */
@Composable
fun HomeScreen(
    appState: AppState,
    onNuevoGuiado: () -> Unit,
    onContinuarBorrador: () -> Unit,
    onNavigate: (String) -> Unit,
    onAcerca: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AforoRepository(context) }
    // Se reevalúa cada vez que se vuelve a Inicio (el destino se recompone) y
    // cuando cambian los triggers de reset/edición.
    val hayBorrador by produceState(
        initialValue = false,
        appState.formResetTrigger.value,
        appState.editarRegistroTrigger.value
    ) {
        value = withContext(Dispatchers.IO) { repository.loadDraft() != null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAcerca) {
                Text("⚙", style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(modifier = Modifier.padding(top = 16.dp))

        Button(
            onClick = onNuevoGuiado,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                stringResource(R.string.btn_nuevo_aforo_guiado),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (hayBorrador) {
            OutlinedButton(
                onClick = onContinuarBorrador,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.btn_continuar_borrador),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.padding(top = 20.dp))
        Text(stringResource(R.string.label_calculadoras), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.padding(top = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalculadoraBoton("⚙", R.string.nav_rendimiento, Modifier.weight(1f)) { onNavigate("rendimiento") }
            CalculadoraBoton("🧪", R.string.nav_aditivo, Modifier.weight(1f)) { onNavigate("aditivo") }
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalculadoraBoton("🎛", R.string.nav_potenciometro, Modifier.weight(1f)) { onNavigate("potenciometro") }
            CalculadoraBoton("📊", R.string.nav_verificacion, Modifier.weight(1f)) { onNavigate("verificacion") }
        }
    }
}

@Composable
private fun CalculadoraBoton(emoji: String, etiqueta: Int, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.padding(top = 4.dp))
            Text(stringResource(etiqueta), style = MaterialTheme.typography.labelLarge)
        }
    }
}
