package com.hansbarrera.aditivosaforo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    if (decimals <= 0) {
        return String.format(Locale.US, "%.0f", value)
    }
    val rounded = String.format(Locale.US, "%.${decimals}f", value)
    return rounded.trimEnd('0').trimEnd('.').ifEmpty { "0" }
}

private val numberInputRegex = Regex("^-?\\d*([.,]\\d*)?$")

/**
 * Campo de entrada numérico decimal, reutilizado en todas las calculadoras.
 *
 * Si se indica [step], se muestran botones +/- a los lados del campo para
 * poder ajustar el valor con un solo toque (útil cuando se trabaja con una
 * sola mano en campo).
 */
@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String? = null,
    modifier: Modifier = Modifier,
    step: Double? = null,
    decimals: Int = 2
) {
    val fieldLabel: (@Composable () -> Unit)? = if (label.isEmpty() && unit == null) null else {
        { Text(if (unit != null) "$label ($unit)" else label) }
    }

    if (step == null) {
        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                if (new.isEmpty() || new.matches(numberInputRegex)) {
                    onValueChange(new)
                }
            },
            label = fieldLabel,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledTonalIconButton(onClick = {
                val nuevo = (value.toDoubleOrZero() - step).coerceAtLeast(0.0)
                onValueChange(formatNumber(nuevo, decimals))
            }) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedTextField(
                value = value,
                onValueChange = { new ->
                    if (new.isEmpty() || new.matches(numberInputRegex)) {
                        onValueChange(new)
                    }
                },
                label = fieldLabel,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = {
                val nuevo = value.toDoubleOrZero() + step
                onValueChange(formatNumber(nuevo, decimals))
            }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
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
