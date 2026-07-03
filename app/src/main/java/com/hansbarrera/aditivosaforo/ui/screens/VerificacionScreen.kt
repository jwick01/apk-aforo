package com.hansbarrera.aditivosaforo.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hansbarrera.aditivosaforo.AppState
import com.hansbarrera.aditivosaforo.R
import com.hansbarrera.aditivosaforo.calc.Formulas
import com.hansbarrera.aditivosaforo.calc.NivelVeredicto
import com.hansbarrera.aditivosaforo.calc.UnidadPantalla
import com.hansbarrera.aditivosaforo.data.Presets
import com.hansbarrera.aditivosaforo.ui.components.DeviationGauge
import com.hansbarrera.aditivosaforo.ui.components.NumberField
import com.hansbarrera.aditivosaforo.ui.components.ResultRow
import com.hansbarrera.aditivosaforo.ui.components.SectionCard
import com.hansbarrera.aditivosaforo.ui.components.formatNumber
import com.hansbarrera.aditivosaforo.ui.components.toDoubleOrZero
import com.hansbarrera.aditivosaforo.ui.theme.VerdictBad
import com.hansbarrera.aditivosaforo.ui.theme.VerdictOk
import com.hansbarrera.aditivosaforo.ui.theme.VerdictWarn
import java.util.Locale
import kotlinx.coroutines.delay

// Códigos estables (independientes del idioma) con los que se registran los modos en el informe.
private const val MODO_PANTALLA_HORMIGON = "pantalla-hormigon"
private const val MODO_PANTALLA_ADITIVO = "pantalla-aditivo"
private const val MODO_TEORICO_HORMIGON = "teorico-hormigon"
private const val MODO_TEORICO_ADITIVO = "teorico-aditivo"

@Composable
fun VerificacionScreen(appState: AppState) {
    val resetKey = appState.formResetTrigger.value
    val editKey = appState.editarRegistroTrigger.value
    val datos = appState.datosInforme.value

    var modoDidactico by rememberSaveable { mutableStateOf(false) }

    // Cronómetro: ancla en elapsedRealtime (monótono) + acumulado, para que siga
    // corriendo aunque se cambie de pestaña, se rote la pantalla o se recree la Activity.
    var cronoCorriendo by rememberSaveable { mutableStateOf(false) }
    var cronoAnclaMs by rememberSaveable { mutableStateOf(0L) }
    var cronoAcumuladoMs by rememberSaveable { mutableStateOf(0L) }
    var cronoAhoraMs by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(cronoCorriendo) {
        while (cronoCorriendo) {
            cronoAhoraMs = SystemClock.elapsedRealtime()
            delay(100)
        }
    }
    val cronoSegundos = (cronoAcumuladoMs +
        if (cronoCorriendo) (cronoAhoraMs - cronoAnclaMs).coerceAtLeast(0L) else 0L) / 1000.0

    // A · Caudal real de hormigón.
    var volumenRecipiente by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Volumen recipiente (m3)"] ?: "0.116")
    }
    var tiempoLlenado by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Tiempo llenado hormigón (s)"] ?: "")
    }

    // Conversor pantalla → kilos.
    var valorPantalla by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Valor de pantalla aditivo"] ?: "")
    }
    var unidadPantallaCodigo by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Unidad de pantalla"] ?: "lts/min")
    }
    var densidadAditivo by rememberSaveable(resetKey, editKey) {
        mutableStateOf(
            datos["Verificación - Densidad aditivo (kg/lt)"]
                ?: datos["Aditivo - Densidad del aditivo (kg/lt)"]
                ?: Presets.DENSIDAD_ADITIVO_DEFAULT.toString()
        )
    }

    // B · Caudal real de aditivo.
    var medicionCodigo by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Modo medición aditivo"] ?: "volumen")
    }
    var cantidadAditivo by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Cantidad aditivo recogida"] ?: "")
    }
    var tiempoRecoleccion by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Tiempo recolección aditivo (s)"] ?: "60")
    }

    // Veredicto. Las dos claves de pantalla se mantienen idénticas a la versión
    // anterior de la app para que los registros viejos sigan restaurándose al editar.
    var modoVeredicto by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Modo veredicto"] ?: MODO_PANTALLA_HORMIGON)
    }
    var pantallaHormigon by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Rendimiento según display (m3/hr)"] ?: "")
    }
    var pantallaAditivo by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Aditivo según display (lts/min)"] ?: "")
    }
    var tolerancia by rememberSaveable(resetKey, editKey) {
        mutableStateOf(datos["Verificación - Tolerancia (%)"] ?: "5")
    }

    // Valores derivados (NaN = sin dato; formatNumber lo muestra como "—").
    val vRecipiente = volumenRecipiente.toDoubleOrZero()
    val tLlenado = tiempoLlenado.toDoubleOrZero()
    val caudalRealHormigon = if (vRecipiente > 0.0 && tLlenado > 0.0) {
        Formulas.rendimientoPorTiempoLlenado(vRecipiente, tLlenado)
    } else {
        Double.NaN
    }

    val unidadPantalla = when (unidadPantallaCodigo) {
        "lts/hr" -> UnidadPantalla.LTS_HR
        "lts/seg" -> UnidadPantalla.LTS_SEG
        else -> UnidadPantalla.LTS_MIN
    }
    val densidad = densidadAditivo.toDoubleOrZero()
    val conversion = Formulas.conversionPantalla(valorPantalla.toDoubleOrZero(), unidadPantalla, densidad)
    val tRecoleccion = tiempoRecoleccion.toDoubleOrZero()
    val kilosEsperados = Formulas.kilosEsperados(conversion.kgSeg, tRecoleccion)

    val medidoEnKg = medicionCodigo == "peso"
    val caudalRealAditivo = Formulas.caudalRealAditivoLtsMin(
        cantidad = cantidadAditivo.toDoubleOrZero(),
        tiempoSeg = tRecoleccion,
        medidoEnKg = medidoEnKg,
        densidadKgLt = densidad
    )

    val teoricoHormigon = appState.rendimientoM3Hr.value
    val teoricoAditivo = appState.caudalAditivoLtsMin.value

    val referencia = when (modoVeredicto) {
        MODO_PANTALLA_ADITIVO -> caudalRealAditivo
        MODO_TEORICO_HORMIGON -> teoricoHormigon ?: Double.NaN
        MODO_TEORICO_ADITIVO -> teoricoAditivo ?: Double.NaN
        else -> caudalRealHormigon
    }
    val comparado = when (modoVeredicto) {
        MODO_PANTALLA_ADITIVO -> pantallaAditivo.toDoubleOrZero().takeIf { it > 0.0 } ?: Double.NaN
        MODO_TEORICO_HORMIGON -> caudalRealHormigon
        MODO_TEORICO_ADITIVO -> caudalRealAditivo
        else -> pantallaHormigon.toDoubleOrZero().takeIf { it > 0.0 } ?: Double.NaN
    }
    val unidadVeredicto = when (modoVeredicto) {
        MODO_PANTALLA_ADITIVO, MODO_TEORICO_ADITIVO -> "lts/min"
        else -> "m3/hr"
    }
    val desviacion = Formulas.desviacionPct(referencia, comparado)
    val toleranciaPct = tolerancia.toDoubleOrZero()
    val nivel = Formulas.nivelVeredicto(desviacion, toleranciaPct)

    val veredictoTexto = when (nivel) {
        NivelVeredicto.OK -> "dentro de tolerancia"
        NivelVeredicto.ADVERTENCIA -> "desviación moderada"
        NivelVeredicto.FUERA -> "fuera de tolerancia"
        NivelVeredicto.SIN_DATOS -> "sin datos"
    }

    // Registra automáticamente todo lo ingresado y calculado, para que quede
    // disponible en el informe aunque no se presione ningún botón.
    LaunchedEffect(
        volumenRecipiente, tiempoLlenado, valorPantalla, unidadPantallaCodigo, densidadAditivo,
        medicionCodigo, cantidadAditivo, tiempoRecoleccion, modoVeredicto, pantallaHormigon,
        pantallaAditivo, tolerancia, desviacion, veredictoTexto
    ) {
        appState.registrarDatos(mapOf(
            "Verificación - Volumen recipiente (m3)" to volumenRecipiente,
            "Verificación - Tiempo llenado hormigón (s)" to tiempoLlenado,
            "Verificación - Caudal real de hormigón (m3/hr)" to formatNumber(caudalRealHormigon, 2),
            "Verificación - Valor de pantalla aditivo" to valorPantalla,
            "Verificación - Unidad de pantalla" to unidadPantallaCodigo,
            "Verificación - Densidad aditivo (kg/lt)" to densidadAditivo,
            "Verificación - Modo medición aditivo" to medicionCodigo,
            "Verificación - Cantidad aditivo recogida" to cantidadAditivo,
            "Verificación - Tiempo recolección aditivo (s)" to tiempoRecoleccion,
            "Verificación - Caudal real de aditivo (lts/min)" to formatNumber(caudalRealAditivo, 2),
            "Verificación - Modo veredicto" to modoVeredicto,
            "Verificación - Rendimiento según display (m3/hr)" to pantallaHormigon,
            "Verificación - Aditivo según display (lts/min)" to pantallaAditivo,
            "Verificación - Tolerancia (%)" to tolerancia,
            "Verificación - Desviación (%)" to formatNumber(desviacion, 1),
            "Verificación - Veredicto" to veredictoTexto
        ))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.title_verificacion), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(top = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = modoDidactico, onCheckedChange = { modoDidactico = it })
            Text(
                stringResource(R.string.label_modo_didactico),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))

        // ---------- Cronómetro ----------
        SectionCard(stringResource(R.string.section_cronometro)) {
            Text(
                String.format(Locale.US, "%.1f s", cronoSegundos),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (cronoCorriendo) {
                            cronoAcumuladoMs += (SystemClock.elapsedRealtime() - cronoAnclaMs).coerceAtLeast(0L)
                            cronoCorriendo = false
                        } else {
                            cronoAnclaMs = SystemClock.elapsedRealtime()
                            cronoAhoraMs = cronoAnclaMs
                            cronoCorriendo = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(if (cronoCorriendo) R.string.btn_crono_detener else R.string.btn_crono_iniciar))
                }
                OutlinedButton(
                    onClick = {
                        cronoCorriendo = false
                        cronoAcumuladoMs = 0L
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_crono_reiniciar))
                }
            }
            Spacer(modifier = Modifier.padding(top = 6.dp))
            Text(stringResource(R.string.desc_cronometro), style = MaterialTheme.typography.bodySmall)
        }

        // ---------- A · Caudal real de hormigón ----------
        SectionCard(stringResource(R.string.section_caudal_real_hormigon)) {
            NumberField(stringResource(R.string.label_volumen_recipiente), volumenRecipiente, { volumenRecipiente = it }, unit = "m3", step = 0.001, decimals = 3)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_tiempo_llenado), tiempoLlenado, { tiempoLlenado = it }, unit = "seg", step = 1.0, decimals = 1)
            Spacer(modifier = Modifier.padding(top = 6.dp))
            OutlinedButton(
                onClick = { tiempoLlenado = formatNumber(cronoSegundos, 1) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_usar_cronometro))
            }
            if (modoDidactico && caudalRealHormigon.isFinite()) {
                BloqueDidactico(
                    stringResource(
                        R.string.didactico_hormigon,
                        formatNumber(vRecipiente, 3),
                        formatNumber(tLlenado, 1),
                        formatNumber(caudalRealHormigon, 2)
                    )
                )
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            ResultRow(stringResource(R.string.result_rendimiento_real), caudalRealHormigon, "m3/hr", decimals = 2)
        }

        // ---------- Conversor pantalla → kilos ----------
        SectionCard(stringResource(R.string.section_conversor)) {
            NumberField(
                stringResource(R.string.label_valor_pantalla),
                valorPantalla,
                { valorPantalla = it },
                unit = when (unidadPantallaCodigo) {
                    "lts/hr" -> "L/h"
                    "lts/seg" -> "L/s"
                    else -> "L/min"
                },
                step = 0.1,
                decimals = 2
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.label_unidad_pantalla), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = unidadPantallaCodigo == "lts/min",
                    onClick = { unidadPantallaCodigo = "lts/min" },
                    label = { Text("L/min") }
                )
                FilterChip(
                    selected = unidadPantallaCodigo == "lts/hr",
                    onClick = { unidadPantallaCodigo = "lts/hr" },
                    label = { Text("L/h") }
                )
                FilterChip(
                    selected = unidadPantallaCodigo == "lts/seg",
                    onClick = { unidadPantallaCodigo = "lts/seg" },
                    label = { Text("L/s") }
                )
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_densidad_aditivo), densidadAditivo, { densidadAditivo = it }, unit = "kg/lt", step = 0.05, decimals = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = densidad == 1.4,
                    onClick = { densidadAditivo = "1.4" },
                    label = { Text("1.40 (Excel)") }
                )
                FilterChip(
                    selected = densidad == 1.33,
                    onClick = { densidadAditivo = "1.33" },
                    label = { Text("1.33 TamShot 110AFC") }
                )
            }
            if (modoDidactico && conversion.kgMin.isFinite()) {
                BloqueDidactico(
                    stringResource(
                        R.string.didactico_conversor,
                        formatNumber(conversion.ltsMin, 2),
                        formatNumber(densidad, 2),
                        formatNumber(conversion.kgMin, 2),
                        formatNumber(conversion.kgSeg, 3)
                    )
                )
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            ResultRow(stringResource(R.string.result_masa_equivalente), conversion.kgMin, "kg/min", decimals = 2)
            ResultRow(stringResource(R.string.result_masa_equivalente), conversion.kgSeg, "kg/seg", decimals = 3)
            ResultRow(
                stringResource(R.string.result_kilos_esperados, formatNumber(tRecoleccion, 0)),
                kilosEsperados,
                "kg",
                decimals = 2
            )
            Spacer(modifier = Modifier.padding(top = 6.dp))
            Text(stringResource(R.string.desc_conversor), style = MaterialTheme.typography.bodySmall)
        }

        // ---------- B · Caudal real de aditivo ----------
        SectionCard(stringResource(R.string.section_caudal_real_aditivo)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !medidoEnKg,
                    onClick = { medicionCodigo = "volumen" },
                    label = { Text(stringResource(R.string.chip_medi_volumen)) }
                )
                FilterChip(
                    selected = medidoEnKg,
                    onClick = { medicionCodigo = "peso" },
                    label = { Text(stringResource(R.string.chip_pese_kg)) }
                )
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(
                stringResource(if (medidoEnKg) R.string.label_masa_pesada else R.string.label_volumen_recolectado),
                cantidadAditivo,
                { cantidadAditivo = it },
                unit = if (medidoEnKg) "kg" else "lts",
                step = 0.05,
                decimals = 2
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_tiempo_recoleccion), tiempoRecoleccion, { tiempoRecoleccion = it }, unit = "seg", step = 1.0, decimals = 1)
            Spacer(modifier = Modifier.padding(top = 6.dp))
            OutlinedButton(
                onClick = { tiempoRecoleccion = formatNumber(cronoSegundos, 1) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_usar_cronometro))
            }
            if (modoDidactico && caudalRealAditivo.isFinite()) {
                if (medidoEnKg) {
                    BloqueDidactico(
                        stringResource(
                            R.string.didactico_probeta_peso,
                            formatNumber(cantidadAditivo.toDoubleOrZero(), 2),
                            formatNumber(densidad, 2),
                            formatNumber(cantidadAditivo.toDoubleOrZero() / densidad, 2),
                            formatNumber(tRecoleccion, 1),
                            formatNumber(caudalRealAditivo, 2)
                        )
                    )
                } else {
                    BloqueDidactico(
                        stringResource(
                            R.string.didactico_probeta_volumen,
                            formatNumber(cantidadAditivo.toDoubleOrZero(), 2),
                            formatNumber(tRecoleccion, 1),
                            formatNumber(caudalRealAditivo, 2)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            ResultRow(stringResource(R.string.result_caudal_real_aditivo), caudalRealAditivo, "lts/min", decimals = 2)
            Spacer(modifier = Modifier.padding(top = 6.dp))
            Text(stringResource(R.string.desc_probeta), style = MaterialTheme.typography.bodySmall)
        }

        // ---------- Veredicto ----------
        SectionCard(stringResource(R.string.section_que_verificar)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = modoVeredicto == MODO_PANTALLA_HORMIGON,
                    onClick = { modoVeredicto = MODO_PANTALLA_HORMIGON },
                    label = { Text(stringResource(R.string.chip_modo_pantalla_hormigon)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = modoVeredicto == MODO_PANTALLA_ADITIVO,
                    onClick = { modoVeredicto = MODO_PANTALLA_ADITIVO },
                    label = { Text(stringResource(R.string.chip_modo_pantalla_aditivo)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = modoVeredicto == MODO_TEORICO_HORMIGON,
                    onClick = { modoVeredicto = MODO_TEORICO_HORMIGON },
                    label = { Text(stringResource(R.string.chip_modo_teorico_hormigon)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = modoVeredicto == MODO_TEORICO_ADITIVO,
                    onClick = { modoVeredicto = MODO_TEORICO_ADITIVO },
                    label = { Text(stringResource(R.string.chip_modo_teorico_aditivo)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            when (modoVeredicto) {
                MODO_PANTALLA_HORMIGON -> {
                    NumberField(stringResource(R.string.label_pantalla_hormigon), pantallaHormigon, { pantallaHormigon = it }, unit = "m3/hr", step = 0.1, decimals = 2)
                }
                MODO_PANTALLA_ADITIVO -> {
                    NumberField(stringResource(R.string.label_pantalla_aditivo), pantallaAditivo, { pantallaAditivo = it }, unit = "lts/min", step = 0.1, decimals = 2)
                }
                MODO_TEORICO_HORMIGON -> {
                    if (teoricoHormigon == null) {
                        Text(stringResource(R.string.msg_falta_teorico_hormigon), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                MODO_TEORICO_ADITIVO -> {
                    if (teoricoAditivo == null) {
                        Text(stringResource(R.string.msg_falta_teorico_aditivo), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            NumberField(stringResource(R.string.label_tolerancia), tolerancia, { tolerancia = it }, unit = "%", step = 1.0, decimals = 0)
        }

        SectionCard(stringResource(R.string.result_title)) {
            val nombreReferencia = stringResource(
                when (modoVeredicto) {
                    MODO_PANTALLA_ADITIVO -> R.string.ref_real_probeta
                    MODO_TEORICO_HORMIGON -> R.string.ref_teorico_hormigon
                    MODO_TEORICO_ADITIVO -> R.string.ref_teorico_aditivo
                    else -> R.string.ref_real_aforo
                }
            )
            val nombreComparado = stringResource(
                when (modoVeredicto) {
                    MODO_TEORICO_HORMIGON -> R.string.ref_real_aforo
                    MODO_TEORICO_ADITIVO -> R.string.ref_real_probeta
                    else -> R.string.cmp_pantalla
                }
            )
            ResultRow(nombreReferencia, referencia, unidadVeredicto, decimals = 2)
            ResultRow(nombreComparado, comparado, unidadVeredicto, decimals = 2)
            Spacer(modifier = Modifier.padding(top = 8.dp))

            val colorNivel = when (nivel) {
                NivelVeredicto.OK -> VerdictOk
                NivelVeredicto.ADVERTENCIA -> VerdictWarn
                NivelVeredicto.FUERA -> VerdictBad
                NivelVeredicto.SIN_DATOS -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val desviacionTexto = if (desviacion.isFinite()) {
                (if (desviacion > 0.0) "+" else "") + formatNumber(desviacion, 1) + "%"
            } else {
                "—"
            }
            Text(
                desviacionTexto,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colorNivel,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.label_desviacion),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            DeviationGauge(
                desviacionPct = desviacion.takeIf { it.isFinite() },
                toleranciaPct = toleranciaPct
            )
            Spacer(modifier = Modifier.padding(top = 10.dp))

            val textoVeredicto = when (nivel) {
                NivelVeredicto.OK -> stringResource(R.string.veredicto_ok, formatNumber(toleranciaPct, 0))
                NivelVeredicto.ADVERTENCIA -> stringResource(R.string.veredicto_warn, formatNumber(toleranciaPct, 0))
                NivelVeredicto.FUERA -> stringResource(R.string.veredicto_bad)
                NivelVeredicto.SIN_DATOS -> stringResource(R.string.veredicto_sin_datos)
            }
            Surface(
                color = if (nivel == NivelVeredicto.SIN_DATOS) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    colorNivel.copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    textoVeredicto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorNivel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (modoDidactico && desviacion.isFinite()) {
                BloqueDidactico(
                    stringResource(
                        R.string.didactico_veredicto,
                        formatNumber(comparado, 2),
                        formatNumber(referencia, 2),
                        (if (desviacion > 0.0) "+" else "") + formatNumber(desviacion, 1)
                    )
                )
            }
            val esModoAditivo = modoVeredicto == MODO_PANTALLA_ADITIVO || modoVeredicto == MODO_TEORICO_ADITIVO
            if (desviacion.isFinite() && desviacion < 0.0 && esModoAditivo) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                Text(stringResource(R.string.hint_aditivo_bajo), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Bloque del modo didáctico: fórmula paso a paso con los valores actuales. */
@Composable
private fun BloqueDidactico(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp)
        )
    }
}
