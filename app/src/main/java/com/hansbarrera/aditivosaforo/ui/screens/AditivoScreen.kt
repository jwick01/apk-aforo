package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.R
import com.hansbarrera.aditivosaforo.calc.Formulas
import com.hansbarrera.aditivosaforo.data.Presets
import com.hansbarrera.aditivosaforo.ui.components.NumberField
import com.hansbarrera.aditivosaforo.ui.components.ResultRow
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import com.hansbarrera.aditivosaforo.ui.components.formatNumber
import com.hansbarrera.aditivosaforo.ui.components.toDoubleOrZero

@Composable
fun AditivoScreen(appState: AppState) {
    val resetKey = appState.formResetTrigger.value
    var rendimiento by rememberSaveable(resetKey) { mutableStateOf("") }
    var dosisCemento by rememberSaveable(resetKey) { mutableStateOf("400") }
    var porcentajeAditivo by rememberSaveable(resetKey) { mutableStateOf("0.08") }
    var densidadAditivo by rememberSaveable(resetKey) { mutableStateOf(Presets.DENSIDAD_ADITIVO_DEFAULT.toString()) }

    // Completa automáticamente el rendimiento si ya fue calculado en otra pestaña.
    LaunchedEffect(appState.rendimientoM3Hr.value) {
        val guardadoAuto = appState.rendimientoM3Hr.value
        if (rendimiento.isBlank() && guardadoAuto != null) {
            rendimiento = formatNumber(guardadoAuto, 6)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.title_aditivo), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        SectionCard(stringResource(R.string.section_datos)) {
            NumberField(stringResource(R.string.rendimiento_bomba_label), rendimiento, { rendimiento = it }, unit = "m3/hr", step = 0.1, decimals = 2)

            val guardado = appState.rendimientoM3Hr.value
            if (guardado != null) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                OutlinedButton(
                    onClick = { rendimiento = formatNumber(guardado, 6) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.usar_rendimiento_guardado, formatNumber(guardado)))
                }
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_dosis_cemento), dosisCemento, { dosisCemento = it }, unit = "kg/m3", step = 5.0, decimals = 0)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.porcentaje_aditivo_label), porcentajeAditivo, { porcentajeAditivo = it }, unit = stringResource(R.string.unit_fraccion_008), step = 0.005, decimals = 3)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_densidad_aditivo), densidadAditivo, { densidadAditivo = it }, unit = "kg/lt", step = 0.05, decimals = 2)
        }

        val resultado = remember(rendimiento, dosisCemento, porcentajeAditivo, densidadAditivo) {
            Formulas.dosisAditivo(
                rendimientoM3Hr = rendimiento.toDoubleOrZero(),
                dosisCementoKg = dosisCemento.toDoubleOrZero(),
                porcentajeAditivo = porcentajeAditivo.toDoubleOrZero(),
                densidadAditivo = densidadAditivo.toDoubleOrZero()
            )
        }

        // Registra automáticamente los datos ingresados, para que queden disponibles
        // en el informe aunque no se presione el botón "Usar estos resultados...".
        LaunchedEffect(dosisCemento, porcentajeAditivo, densidadAditivo, resultado) {
            appState.registrarDatos(mapOf(
                "Aditivo - Dosis de cemento (kg/m3)" to dosisCemento,
                "Aditivo - Porcentaje de aditivo" to porcentajeAditivo,
                "Aditivo - Densidad del aditivo (kg/lt)" to densidadAditivo,
                "Aditivo - Kilos de aditivo requerido (kg/min)" to formatNumber(resultado.kilosAditivoKgMin, 1),
                "Aditivo - Litros de aditivo (lts/min)" to formatNumber(resultado.litrosAditivoLtsMin, 1)
            ))
        }

        SectionCard(stringResource(R.string.result_title)) {
            ResultRow(stringResource(R.string.result_kilos_aditivo), resultado.kilosAditivoKgMin, "kg/min")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ResultRow(stringResource(R.string.result_litros_aditivo), resultado.litrosAditivoLtsMin, "lts/min")

            Spacer(modifier = Modifier.padding(top = 8.dp))
            Button(
                onClick = {
                    appState.caudalAditivoLtsMin.value = resultado.litrosAditivoLtsMin
                    appState.porcentajeAditivoCalculado.value = porcentajeAditivo.toDoubleOrZero()
                    appState.registrarDatos(mapOf(
                        "Aditivo - Dosis de cemento (kg/m3)" to dosisCemento,
                        "Aditivo - Porcentaje de aditivo" to porcentajeAditivo,
                        "Aditivo - Densidad del aditivo (kg/lt)" to densidadAditivo,
                        "Aditivo - Kilos de aditivo requerido (kg/min)" to formatNumber(resultado.kilosAditivoKgMin, 1),
                        "Aditivo - Litros de aditivo (lts/min)" to formatNumber(resultado.litrosAditivoLtsMin, 1)
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_usar_resultados_verificacion))
            }
        }
    }
}
