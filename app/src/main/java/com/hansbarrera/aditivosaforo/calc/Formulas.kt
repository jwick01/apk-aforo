package com.hansbarrera.aditivosaforo.calc

/**
 * Resultado del cálculo de rendimiento de la bomba (caudal de hormigón).
 */
data class RendimientoResult(
    val volumenEfectivoLtsMin: Double,
    val volumenEfectivoLtsHr: Double,
    val rendimientoM3Hr: Double
)

/**
 * Resultado del cálculo de dosis de aditivo / acelerante.
 */
data class AditivoResult(
    val kilosAditivoKgMin: Double,
    val litrosAditivoLtsMin: Double
)

/**
 * Unidad en la que la pantalla del equipo muestra el caudal de aditivo.
 */
enum class UnidadPantalla { LTS_MIN, LTS_HR, LTS_SEG }

/**
 * Resultado de convertir la lectura de pantalla a caudal normalizado y masa.
 */
data class ConversionPantallaResult(
    val ltsMin: Double,
    val kgMin: Double,
    val kgSeg: Double
)

/**
 * Nivel del veredicto de la verificación según la tolerancia elegida.
 */
enum class NivelVeredicto { OK, ADVERTENCIA, FUERA, SIN_DATOS }

/**
 * Una fila de la tabla potenciómetro -> acelerante (kg/min).
 */
data class FilaPotenciometro(
    val potenciometro: Double,
    val aceleranteKgMin: Double
)

/**
 * Replica las fórmulas de la hoja "aditivos y envoladas" / "display" del Excel.
 */
object Formulas {

    /**
     * Rendimiento de la bomba calculado a partir del volumen del cilindro,
     * el número de émboladas por minuto y el factor de llenado.
     *
     * Excel: C7=(C4*C5)*C6 ; C8=C7*60 ; C9=C8/1000
     */
    fun rendimientoPorEmboladas(
        volumenCilindroLts: Double,
        emboladasPorMin: Double,
        factorLlenado: Double
    ): RendimientoResult {
        val efectivoLtsMin = volumenCilindroLts * emboladasPorMin * factorLlenado
        val efectivoLtsHr = efectivoLtsMin * 60.0
        val rendimientoM3Hr = efectivoLtsHr / 1000.0
        return RendimientoResult(efectivoLtsMin, efectivoLtsHr, rendimientoM3Hr)
    }

    /**
     * Rendimiento de la bomba calculado a partir del tiempo y volumen de llenado.
     *
     * Excel: G22=(3600*G21)/G20
     */
    fun rendimientoPorTiempoLlenado(
        volumenLlenadoM3: Double,
        tiempoLlenadoSeg: Double
    ): Double {
        if (tiempoLlenadoSeg == 0.0) return 0.0
        return (3600.0 * volumenLlenadoM3) / tiempoLlenadoSeg
    }

    /**
     * Dosis de aditivo/acelerante requerida en kg/min y lts/min.
     *
     * Excel: C13=((C11*C9)*C12)/60 ; C15=C13/C14
     */
    fun dosisAditivo(
        rendimientoM3Hr: Double,
        dosisCementoKg: Double,
        porcentajeAditivo: Double,
        densidadAditivo: Double
    ): AditivoResult {
        val kilosAditivoKgMin = (dosisCementoKg * rendimientoM3Hr * porcentajeAditivo) / 60.0
        val litrosAditivoLtsMin = if (densidadAditivo == 0.0) 0.0 else kilosAditivoKgMin / densidadAditivo
        return AditivoResult(kilosAditivoKgMin, litrosAditivoLtsMin)
    }

    /**
     * Desviación relativa entre un valor calculado y el valor leído en el display.
     * Excel: H15=((H7-L5)/L5) ; H16=((H12-L6)/L6) ; H17=((H10-L10)/L10)
     * Devuelve NaN cuando el display es 0: la desviación no está definida y
     * mostrar 0% haría parecer que el equipo coincide perfectamente.
     */
    fun desviacion(calculado: Double, display: Double): Double {
        if (display == 0.0) return Double.NaN
        return (calculado - display) / display
    }

    /**
     * Convierte la lectura de la pantalla del equipo a lts/min.
     */
    fun pantallaALtsMin(valor: Double, unidad: UnidadPantalla): Double = when (unidad) {
        UnidadPantalla.LTS_MIN -> valor
        UnidadPantalla.LTS_HR -> valor / 60.0
        UnidadPantalla.LTS_SEG -> valor * 60.0
    }

    /**
     * Convierte la lectura de pantalla (en cualquiera de sus unidades) a caudal
     * normalizado (lts/min) y masa (kg/min y kg/s) usando la densidad del aditivo.
     */
    fun conversionPantalla(
        valor: Double,
        unidad: UnidadPantalla,
        densidadKgLt: Double
    ): ConversionPantallaResult {
        if (!valor.isFinite() || valor <= 0.0 || !densidadKgLt.isFinite() || densidadKgLt <= 0.0) {
            return ConversionPantallaResult(Double.NaN, Double.NaN, Double.NaN)
        }
        val ltsMin = pantallaALtsMin(valor, unidad)
        val kgMin = ltsMin * densidadKgLt
        return ConversionPantallaResult(ltsMin, kgMin, kgMin / 60.0)
    }

    /**
     * Kilos que debería marcar la báscula tras recolectar durante [tiempoSeg] segundos.
     */
    fun kilosEsperados(kgSeg: Double, tiempoSeg: Double): Double {
        if (!kgSeg.isFinite() || tiempoSeg <= 0.0) return Double.NaN
        return kgSeg * tiempoSeg
    }

    /**
     * Caudal real de aditivo (lts/min) a partir de lo recolectado en un tiempo dado.
     * Si se pesó ([medidoEnKg]), la masa se convierte a litros con la densidad.
     */
    fun caudalRealAditivoLtsMin(
        cantidad: Double,
        tiempoSeg: Double,
        medidoEnKg: Boolean,
        densidadKgLt: Double
    ): Double {
        if (cantidad <= 0.0 || tiempoSeg <= 0.0) return Double.NaN
        val litros = if (medidoEnKg) {
            if (densidadKgLt <= 0.0) return Double.NaN
            cantidad / densidadKgLt
        } else {
            cantidad
        }
        return litros * 60.0 / tiempoSeg
    }

    /**
     * Desviación porcentual del valor comparado frente a la referencia.
     * Devuelve NaN si falta la referencia (la desviación no está definida).
     */
    fun desviacionPct(referencia: Double, comparado: Double): Double {
        if (!referencia.isFinite() || !comparado.isFinite() || referencia == 0.0) return Double.NaN
        return (comparado - referencia) / referencia * 100.0
    }

    /**
     * Clasifica la desviación: dentro de la tolerancia, moderada (hasta el doble)
     * o fuera de tolerancia. SIN_DATOS cuando la desviación no está definida.
     */
    fun nivelVeredicto(desviacionPct: Double, toleranciaPct: Double): NivelVeredicto {
        if (!desviacionPct.isFinite() || !toleranciaPct.isFinite() || toleranciaPct <= 0.0) {
            return NivelVeredicto.SIN_DATOS
        }
        val abs = kotlin.math.abs(desviacionPct)
        return when {
            abs <= toleranciaPct -> NivelVeredicto.OK
            abs <= toleranciaPct * 2.0 -> NivelVeredicto.ADVERTENCIA
            else -> NivelVeredicto.FUERA
        }
    }

    /**
     * Cantidad de acelerante requerida (kg/min) en función del rendimiento de la bomba,
     * la dosis de cemento por m3 y el porcentaje de acelerante requerido.
     *
     * Excel: C28=C27*C26 ; C29=C28/60 ; C31=(C26*C27)*C30/60
     */
    fun aceleranteRequeridoKgMin(
        rendimientoM3Hr: Double,
        cementoKgM3: Double,
        porcentajeAceleranteRequerido: Double
    ): Double {
        return (rendimientoM3Hr * cementoKgM3) * porcentajeAceleranteRequerido / 60.0
    }

    /**
     * Interpola linealmente la posición del potenciómetro a partir de una tabla
     * (potenciómetro, acelerante kg/min) y un valor objetivo de acelerante (kg/min).
     *
     * Replica la interpolación INDEX/MATCH + XLOOKUP de la hoja
     * "tablas para generar informe": busca las dos filas de la tabla cuyo valor
     * de acelerante "encierra" el objetivo y obtiene la posición intermedia
     * de forma proporcional. Si el objetivo está fuera de rango, se extrapola
     * usando los dos puntos extremos correspondientes.
     */
    fun potenciometroInterpolado(
        tabla: List<FilaPotenciometro>,
        aceleranteObjetivoKgMin: Double
    ): Double? {
        val filas = tabla.filter { it.aceleranteKgMin > 0.0 }.sortedBy { it.aceleranteKgMin }
        if (filas.size < 2) return null

        // Buscar el tramo [ace1, ace2] que contiene al objetivo.
        var lower = filas.first()
        var upper = filas.last()
        var found = false
        for (i in 0 until filas.size - 1) {
            val a = filas[i]
            val b = filas[i + 1]
            if (aceleranteObjetivoKgMin in a.aceleranteKgMin..b.aceleranteKgMin) {
                lower = a
                upper = b
                found = true
                break
            }
        }
        if (!found) {
            // Extrapolación con el tramo más cercano.
            lower = if (aceleranteObjetivoKgMin < filas.first().aceleranteKgMin) filas[0] else filas[filas.size - 2]
            upper = if (aceleranteObjetivoKgMin < filas.first().aceleranteKgMin) filas[1] else filas[filas.size - 1]
        }

        val ace1 = lower.aceleranteKgMin
        val ace2 = upper.aceleranteKgMin
        val pot1 = lower.potenciometro
        val pot2 = upper.potenciometro
        if (ace2 == ace1) return pot1

        return pot1 + (aceleranteObjetivoKgMin - ace1) * (pot2 - pot1) / (ace2 - ace1)
    }
}
