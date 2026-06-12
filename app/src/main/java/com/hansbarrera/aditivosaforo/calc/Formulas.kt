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
 * Resultado de las desviaciones frente a las lecturas del display del equipo.
 */
data class DesviacionResult(
    val desviacionCaudalHormigon: Double,
    val desviacionCaudalAditivo: Double,
    val desviacionPorcentajeAditivo: Double
)

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
     */
    fun desviacion(calculado: Double, display: Double): Double {
        if (display == 0.0) return 0.0
        return (calculado - display) / display
    }

    fun desviaciones(
        rendimientoCalculadoM3Hr: Double,
        rendimientoDisplayM3Hr: Double,
        caudalAditivoCalculadoLtsMin: Double,
        aditivoDisplayLtsMin: Double,
        porcentajeAditivoCalculado: Double,
        porcentajeAditivoDisplay: Double
    ): DesviacionResult {
        return DesviacionResult(
            desviacionCaudalHormigon = desviacion(rendimientoCalculadoM3Hr, rendimientoDisplayM3Hr),
            desviacionCaudalAditivo = desviacion(caudalAditivoCalculadoLtsMin, aditivoDisplayLtsMin),
            desviacionPorcentajeAditivo = desviacion(porcentajeAditivoCalculado, porcentajeAditivoDisplay)
        )
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
