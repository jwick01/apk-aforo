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

    /**
     * Se incrementa cada vez que se borra todo para comenzar un nuevo aforo.
     * Las pantallas usan este valor como key de rememberSaveable para que sus
     * campos vuelvan a los valores por defecto en vez de quedar en caché.
     */
    val formResetTrigger = mutableStateOf(0)

    /**
     * Se incrementa cada vez que se carga un registro del historial para
     * editarlo, forzando a la pestaña "Nuevo registro" a releer el borrador
     * (que ya contiene los datos de ese registro) en lugar de mantener los
     * campos que tenía cargados previamente.
     */
    val editarRegistroTrigger = mutableStateOf(0)

    fun registrarDatos(nuevos: Map<String, String>) {
        datosInforme.value = datosInforme.value + nuevos
    }

    fun limpiarDatosInforme() {
        datosInforme.value = emptyMap()
    }

    /** Borra todos los datos acumulados de las calculadoras y fuerza a las pantallas a reiniciar sus campos. */
    fun reiniciarTodo() {
        datosInforme.value = emptyMap()
        rendimientoM3Hr.value = null
        caudalAditivoLtsMin.value = null
        porcentajeAditivoCalculado.value = null
        formResetTrigger.value++
    }

    /** Trae los resultados de un registro del historial y pide a "Nuevo registro" que cargue sus datos. */
    fun cargarRegistroParaEditar(resultados: Map<String, String>) {
        datosInforme.value = resultados
        editarRegistroTrigger.value++
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
