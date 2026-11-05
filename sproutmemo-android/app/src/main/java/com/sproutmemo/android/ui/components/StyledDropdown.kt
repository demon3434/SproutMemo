package com.sproutmemo.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun RequiredLabel(text: String, isRequired: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text)
        if (isRequired) {
            Text(" *", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> StyledDropdown(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemToString: (T) -> String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedItem?.let(itemToString) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { RequiredLabel(label, isRequired) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.neonBlue,
                unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.3f),
                focusedLabelColor = colors.neonBlue,
                unfocusedLabelColor = colors.textSecondary
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemToString(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
