package com.hansbarrera.aditivosaforo

import androidx.compose.runtime.mutableStateOf

/**
 * Resultados compartidos entre calculadoras, para evitar tener que
 * volver a teclear valores ya calculados en otra pantalla.
 */
class AppState {
    /** Último rendimiento de la bomba calculado (m3/hr). */
    val rendimientoM3Hr = mutableStateOf<Double?>(null)

    /** Último caudal de aditivo/acelerante calculado (lts/min). */
    val caudalAditivoLtsMin = mutableStateOf<Double?>(null)

    /** Último porcentaje de aditivo calculado. */
    val porcentajeAditivoCalculado = mutableStateOf<Double?>(null)

    /** Última cantidad de acelerante requerida (kg/min), para la pestaña del potenciómetro. */
    val aceleranteRequeridoKgMin = mutableStateOf<Double?>(null)
}
