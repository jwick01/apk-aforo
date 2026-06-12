package com.hansbarrera.aditivosaforo.data

/**
 * Preajustes de volumen de cilindro (litros) por tipo de bomba,
 * tomados de la hoja "aditivos y envoladas" / "tablas para generar informe".
 */
data class BombaPreset(
    val nombre: String,
    val volumenCilindroLts: Double
)

object Presets {
    val bombas = listOf(
        BombaPreset("Alpha 15", 10.1),
        BombaPreset("Alpha 20", 13.4),
        BombaPreset("Alpha 30", 18.85),
        BombaPreset("Alpha 40", 17.67),
        BombaPreset("TK-40", 17.7),
        BombaPreset("Putzmeister 1007", 25.4),
        BombaPreset("Personalizado", 0.0)
    )

    /** Factor de llenado por defecto usado en todo el Excel. */
    const val FACTOR_LLENADO_DEFAULT = 0.85

    /** Densidad de aditivo/acelerante por defecto usada en todo el Excel. */
    const val DENSIDAD_ADITIVO_DEFAULT = 1.4

    /**
     * Tabla potenciómetro -> acelerante (kg/min) de fábrica, tomada de la hoja
     * "tablas para generar informe" (Tabla49 / Tabla4).
     */
    val tablaPotenciometroDefault = listOf(
        4.0 to 4.0,
        5.0 to 6.0,
        6.0 to 8.0,
        7.0 to 10.6,
        8.0 to 13.0,
        9.0 to 14.4
    )
}
