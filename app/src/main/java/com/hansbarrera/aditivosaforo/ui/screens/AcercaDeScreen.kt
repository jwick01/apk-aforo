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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.BuildConfig
import com.hansbarrera.aditivosaforo.R
import com.hansbarrera.aditivosaforo.ui.components.SectionCard

@Composable
fun AcercaDeScreen(appState: AppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.nav_acerca), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        SectionCard(stringResource(R.string.section_apariencia)) {
            Text(
                stringResource(R.string.desc_apariencia),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            val temaActual = appState.temaOscuro.value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = temaActual == null,
                    onClick = { appState.setTemaOscuro(null) },
                    label = { Text(stringResource(R.string.chip_seguir_sistema)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = temaActual == false,
                    onClick = { appState.setTemaOscuro(false) },
                    label = { Text(stringResource(R.string.chip_claro)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = temaActual == true,
                    onClick = { appState.setTemaOscuro(true) },
                    label = { Text(stringResource(R.string.chip_oscuro)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionCard(stringResource(R.string.app_name)) {
            Text(stringResource(R.string.label_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(
                stringResource(R.string.desc_app),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(stringResource(R.string.credit_hecho_por), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        SectionCard(stringResource(R.string.section_privacidad)) {
            Text(
                stringResource(R.string.desc_privacidad),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
