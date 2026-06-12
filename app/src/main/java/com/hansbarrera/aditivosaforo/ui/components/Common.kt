package com.hansbarrera.aditivosaforo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Convierte un texto introducido por el usuario (con coma o punto decimal) a Double. */
fun String.toDoubleOrZero(): Double = this.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

/** Formatea un número con hasta [decimals] decimales, sin ceros sobrantes. */
fun formatNumber(value: Double, decimals: Int = 3): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val rounded = String.format(Locale.US, "%.${decimals}f", value)
    return rounded.trimEnd('0').trimEnd('.').ifEmpty { "0" }
}

/** Campo de entrada numérico decimal, reutilizado en todas las calculadoras. */
@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.isEmpty() || new.matches(Regex("^-?\\d*([.,]\\d*)?$"))) {
                onValueChange(new)
            }
        },
        label = if (label.isEmpty() && unit == null) null else {
            { Text(if (unit != null) "$label ($unit)" else label) }
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

/** Tarjeta con título de sección. */
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
            content()
        }
    }
}

/** Fila etiqueta/valor para mostrar resultados. */
@Composable
fun ResultRow(label: String, value: Double, unit: String, decimals: Int = 3) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        val text = if (unit.isBlank()) formatNumber(value, decimals) else "${formatNumber(value, decimals)} $unit"
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
