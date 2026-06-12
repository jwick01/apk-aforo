package com.hansbarrera.aditivosaforo

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Estado compartido entre pantallas: resultados recientes para "traer" valores
 * de una calculadora a otra, datos acumulados para el registro de aforo y
 * preferencias de apariencia persistidas en SharedPreferences.
 */
class AppState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Último rendimiento de la bomba calculado (m3/hr). */
    val rendimientoM3Hr = mutableStateOf<Double?>(null)

    /** Último caudal de aditivo/acelerante calculado (lts/min). */
    val caudalAditivoLtsMin = mutableStateOf<Double?>(null)

    /** Último porcentaje de aditivo calculado. */
    val porcentajeAditivoCalculado = mutableStateOf<Double?>(null)

    /**
     * Datos de entrada y resultados acumulados desde las distintas calculadoras
     * (clave legible -> valor), para incluir automáticamente en el registro de
     * aforo y, desde ahí, en el informe.
     */
    val datosInforme = mutableStateOf<Map<String, String>>(emptyMap())

    /** Tema: null = seguir al sistema, true = forzar oscuro, false = forzar claro. */
    val temaOscuro = mutableStateOf<Boolean?>(
        when (prefs.getInt("tema_oscuro", -1)) {
            1 -> true
            0 -> false
            else -> null
        }
    )

    fun registrarDatos(nuevos: Map<String, String>) {
        datosInforme.value = datosInforme.value + nuevos
    }

    fun limpiarDatosInforme() {
        datosInforme.value = emptyMap()
    }

    fun setTemaOscuro(valor: Boolean?) {
        temaOscuro.value = valor
        prefs.edit().putInt("tema_oscuro", when (valor) {
            true -> 1
            false -> 0
            null -> -1
        }).apply()
    }
}
