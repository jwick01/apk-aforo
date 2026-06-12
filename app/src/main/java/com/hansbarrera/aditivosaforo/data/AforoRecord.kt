package com.hansbarrera.aditivosaforo.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Registro de un aforo de bomba: datos generales del trabajo, resultados de las
 * calculadoras y las fotografías asociadas (almacenadas como archivos locales
 * dentro de la carpeta del registro).
 */
data class AforoRecord(
    val id: String,
    val fecha: String,
    val cliente: String,
    val proyectoOMina: String,
    val lugarAforo: String,
    val operador: String,
    val numeroEquipo: String,
    val odometro: String,
    val observaciones: String,
    val resultados: Map<String, String> = emptyMap(),
    val fotos: List<String> = emptyList(),
    val fotoNotas: Map<String, String> = emptyMap()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("fecha", fecha)
        obj.put("cliente", cliente)
        obj.put("proyectoOMina", proyectoOMina)
        obj.put("lugarAforo", lugarAforo)
        obj.put("operador", operador)
        obj.put("numeroEquipo", numeroEquipo)
        obj.put("odometro", odometro)
        obj.put("observaciones", observaciones)

        // Agrupa las claves con formato "Sección - Campo" en sub-objetos por sección,
        // para que el JSON exportado quede organizado por calculadora.
        val resultadosJson = JSONObject()
        resultados.forEach { (clave, valor) ->
            val sep = clave.indexOf(" - ")
            if (sep >= 0) {
                val seccion = clave.substring(0, sep)
                val campo = clave.substring(sep + 3)
                val grupo = resultadosJson.optJSONObject(seccion)
                    ?: JSONObject().also { resultadosJson.put(seccion, it) }
                grupo.put(campo, valor)
            } else {
                resultadosJson.put(clave, valor)
            }
        }
        obj.put("resultados", resultadosJson)

        obj.put("fotos", JSONArray(fotos))

        val fotoNotasJson = JSONObject()
        fotoNotas.forEach { (nombre, nota) -> fotoNotasJson.put(nombre, nota) }
        obj.put("fotoNotas", fotoNotasJson)

        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): AforoRecord {
            val resultados = mutableMapOf<String, String>()
            obj.optJSONObject("resultados")?.let { resultadosJson ->
                resultadosJson.keys().forEach { clave ->
                    val valor = resultadosJson.get(clave)
                    if (valor is JSONObject) {
                        valor.keys().forEach { campo ->
                            resultados["$clave - $campo"] = valor.getString(campo)
                        }
                    } else {
                        resultados[clave] = resultadosJson.getString(clave)
                    }
                }
            }

            val fotos = mutableListOf<String>()
            obj.optJSONArray("fotos")?.let { fotosJson ->
                for (i in 0 until fotosJson.length()) {
                    fotos.add(fotosJson.getString(i))
                }
            }

            val fotoNotas = mutableMapOf<String, String>()
            obj.optJSONObject("fotoNotas")?.let { fotoNotasJson ->
                fotoNotasJson.keys().forEach { nombre ->
                    fotoNotas[nombre] = fotoNotasJson.getString(nombre)
                }
            }

            return AforoRecord(
                id = obj.getString("id"),
                fecha = obj.optString("fecha"),
                cliente = obj.optString("cliente"),
                proyectoOMina = obj.optString("proyectoOMina"),
                lugarAforo = obj.optString("lugarAforo"),
                operador = obj.optString("operador"),
                numeroEquipo = obj.optString("numeroEquipo"),
                odometro = obj.optString("odometro"),
                observaciones = obj.optString("observaciones"),
                resultados = resultados,
                fotos = fotos,
                fotoNotas = fotoNotas
            )
        }
    }
}
