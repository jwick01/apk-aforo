package com.hansbarrera.aditivosaforo.data

/**
 * Agrupa resultados con claves "Sección - Campo" por sección, para mostrarlos
 * ordenados en pantalla y en el informe PDF.
 */
fun agruparResultados(datos: Map<String, String>): Map<String, List<Pair<String, String>>> {
    val grupos = LinkedHashMap<String, MutableList<Pair<String, String>>>()
    datos.forEach { (clave, valor) ->
        val sep = clave.indexOf(" - ")
        val seccion = if (sep >= 0) clave.substring(0, sep) else "General"
        val campo = if (sep >= 0) clave.substring(sep + 3) else clave
        grupos.getOrPut(seccion) { mutableListOf() }.add(campo to valor)
    }
    return grupos
}
