package com.hansbarrera.aditivosaforo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.hansbarrera.aditivosaforo.calc.FilaPotenciometro
import com.hansbarrera.aditivosaforo.calc.Formulas
import com.hansbarrera.aditivosaforo.data.Presets
import com.hansbarrera.aditivosaforo.ui.components.NumberField
import com.hansbarrera.aditivosaforo.ui.components.PotenciometroCurveChart
import com.hansbarrera.aditivosaforo.ui.components.ResultRow
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import com.hansbarrera.aditivosaforo.ui.components.formatNumber
import com.hansbarrera.aditivosaforo.ui.components.toDoubleOrZero

@Composable
fun PotenciometroScreen(appState: AppState) {
    val resetKey = appState.formResetTrigger.value
    var rendimiento by rememberSaveable(resetKey) { mutableStateOf("") }
    var porcentajeAcelerante by rememberSaveable(resetKey) { mutableStateOf("0.08") }
    var objetivoManual by rememberSaveable(resetKey) { mutableStateOf("") }

    // Completa automáticamente el rendimiento si ya fue calculado en otra pestaña.
    LaunchedEffect(appState.rendimientoM3Hr.value) {
        val guardadoAuto = appState.rendimientoM3Hr.value
        if (rendimiento.isBlank() && guardadoAuto != null) {
            rendimiento = formatNumber(guardadoAuto, 6)
        }
    }

    val filasPotenciometro = remember(resetKey) {
        Presets.tablaPotenciometroDefault.map {
            mutableStateOf(it.first.toString()) to mutableStateOf(it.second.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.title_potenciometro), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 12.dp))

        SectionCard(stringResource(R.string.section_acelerante_requerido)) {
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
            NumberField(stringResource(R.string.label_cemento_por_m3), appState.cementoKgM3.value, { appState.cementoKgM3.value = it }, unit = "kg/m3", step = 5.0, decimals = 0)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_porcentaje_acelerante_requerido), porcentajeAcelerante, { porcentajeAcelerante = it }, unit = stringResource(R.string.unit_fraccion_008), step = 0.005, decimals = 3)
        }

        val aceleranteCalculado = remember(rendimiento, appState.cementoKgM3.value, porcentajeAcelerante) {
            Formulas.aceleranteRequeridoKgMin(
                rendimientoM3Hr = rendimiento.toDoubleOrZero(),
                cementoKgM3 = appState.cementoKgM3.value.toDoubleOrZero(),
                porcentajeAceleranteRequerido = porcentajeAcelerante.toDoubleOrZero()
            )
        }

        // Registra automáticamente los datos ingresados, para que queden disponibles
        // en el informe aunque no se presione el botón "Usar este valor como objetivo...".
        LaunchedEffect(appState.cementoKgM3.value, porcentajeAcelerante, aceleranteCalculado) {
            appState.registrarDatos(mapOf(
                "Potenciómetro - Cantidad de cemento (kg/m3)" to appState.cementoKgM3.value,
                "Potenciómetro - Porcentaje de acelerante requerido" to porcentajeAcelerante,
                "Potenciómetro - Acelerante requerido (kg/min)" to formatNumber(aceleranteCalculado, 1)
            ))
        }

        SectionCard(stringResource(R.string.result_title)) {
            ResultRow(stringResource(R.string.result_acelerante_requerido), aceleranteCalculado, "kg/min")
            Spacer(modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = {
                    objetivoManual = formatNumber(aceleranteCalculado, 6)
                    appState.registrarDatos(mapOf(
                        "Potenciómetro - Cantidad de cemento (kg/m3)" to appState.cementoKgM3.value,
                        "Potenciómetro - Porcentaje de acelerante requerido" to porcentajeAcelerante,
                        "Potenciómetro - Acelerante requerido (kg/min)" to formatNumber(aceleranteCalculado, 1)
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_usar_objetivo_potenciometro))
            }
        }

        SectionCard(stringResource(R.string.section_tabla_calibracion)) {
            Text(
                stringResource(R.string.desc_tabla_calibracion),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.header_potenciometro), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.header_acelerante_kgmin), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.padding(top = 4.dp))

            filasPotenciometro.forEach { (pot, ace) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    NumberField("", pot.value, { pot.value = it }, modifier = Modifier.weight(1f))
                    NumberField("", ace.value, { ace.value = it }, modifier = Modifier.weight(1f))
                }
            }
        }

        SectionCard(stringResource(R.string.section_posicion_interpolada)) {
            NumberField(stringResource(R.string.label_acelerante_objetivo), objetivoManual, { objetivoManual = it }, unit = "kg/min", step = 0.1, decimals = 2)

            val tabla = remember(filasPotenciometro.map { it.first.value to it.second.value }) {
                filasPotenciometro.map { (pot, ace) ->
                    FilaPotenciometro(
                        potenciometro = pot.value.toDoubleOrZero(),
                        aceleranteKgMin = ace.value.toDoubleOrZero()
                    )
                }
            }

            val posicion = remember(tabla, objetivoManual) {
                Formulas.potenciometroInterpolado(tabla, objetivoManual.toDoubleOrZero())
            }

            val tablaTexto = remember(tabla) {
                tabla.filter { it.aceleranteKgMin > 0.0 }
                    .sortedBy { it.potenciometro }
                    .joinToString("; ") { "${formatNumber(it.potenciometro, 1)} -> ${formatNumber(it.aceleranteKgMin, 1)} kg/min" }
            }

            // Registra automáticamente la tabla, el objetivo y la posición interpolada,
            // para que queden disponibles en el informe aunque no se presione el botón.
            LaunchedEffect(tablaTexto, objetivoManual, posicion) {
                val datos = mutableMapOf(
                    "Potenciómetro - Tabla de calibración" to tablaTexto,
                    "Potenciómetro - Acelerante objetivo (kg/min)" to objetivoManual
                )
                if (posicion != null) {
                    datos["Potenciómetro - Posición interpolada"] = formatNumber(posicion, 1)
                }
                appState.registrarDatos(datos)
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))
            if (posicion != null) {
                ResultRow(stringResource(R.string.posicion_potenciometro_label), posicion, "", decimals = 1)
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedButton(
                    onClick = {
                        appState.registrarDatos(mapOf(
                            "Potenciómetro - Tabla de calibración" to tablaTexto,
                            "Potenciómetro - Acelerante objetivo (kg/min)" to objetivoManual,
                            "Potenciómetro - Posición interpolada" to formatNumber(posicion, 1)
                        ))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_usar_posicion_aforo))
                }
            } else {
                Text(
                    stringResource(R.string.msg_min_dos_filas),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.padding(top = 12.dp))
            PotenciometroCurveChart(
                tabla = tabla,
                objetivo = objetivoManual.toDoubleOrZero().takeIf { it > 0.0 },
                posicion = posicion
            )
        }
    }
}
