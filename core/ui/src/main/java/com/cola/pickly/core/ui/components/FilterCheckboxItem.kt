package com.cola.pickly.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.theme.TealAccent

@Composable
fun FilterCheckboxItem(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    val backgroundColor = if (checked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Color.Transparent

    val labelColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else if (checked) {
        TealAccent
    } else {
        MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable(enabled = enabled) { onToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.scale(0.7f),
            colors = CheckboxDefaults.colors(
                checkedColor = TealAccent,
                uncheckedColor = labelColor
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
    }
}

@Composable
fun FilterTriStateCheckboxItem(
    label: String,
    state: ToggleableState,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    val isChecked = state != ToggleableState.Off
    val backgroundColor = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Color.Transparent

    val labelColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else if (isChecked) {
        TealAccent
    } else {
        MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable(enabled = enabled) { onToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TriStateCheckbox(
            state = state,
            onClick = null,
            modifier = Modifier.scale(0.7f),
            colors = CheckboxDefaults.colors(
                checkedColor = TealAccent,
                uncheckedColor = labelColor
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
    }
}
