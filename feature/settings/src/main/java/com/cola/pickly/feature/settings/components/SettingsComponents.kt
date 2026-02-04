package com.cola.pickly.feature.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cola.pickly.core.ui.theme.TealAccent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val titleColor = MaterialTheme.colorScheme.onSurface
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = titleColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun SettingsGroupLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
fun SettingsRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .clickable(onClick = onClick),
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = TealAccent
                )
            )
        },
        headlineContent = { Text(text = title, color = primaryColor) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = secondaryColor) }
        }
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        modifier = modifier.fillMaxWidth(),
        headlineContent = { Text(text = title, color = primaryColor) },
        supportingContent = {
            // Subtitle이 없어도 높이를 유지하기 위해 빈 텍스트를 렌더링하거나 subtitle을 표시함
            val text = subtitle ?: " "
            Text(
                text = text,
                color = if (subtitle != null) secondaryColor else Color.Transparent
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TealAccent,
                    checkedBorderColor = TealAccent
                )
            )
        }
    )
}

@Composable
fun SettingsTextItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        modifier = modifier.fillMaxWidth(),
        headlineContent = { Text(text = title, color = primaryColor) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = secondaryColor) }
        },
        trailingContent = { Text(text = value, color = secondaryColor) }
    )
}

@Composable
fun SettingsActionItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    enabled: Boolean = true
) {
    val primaryColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
            }
            .clickable(enabled = enabled, onClick = onClick),
        headlineContent = { Text(text = title, color = primaryColor) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = secondaryColor) }
        },
        trailingContent = trailingText?.let { tt ->
            { Text(text = tt, color = secondaryColor) }
        }
    )
}

@Composable
fun SettingsCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Checkbox
                this.selected = checked
            }
            .clickable { onCheckedChange(!checked) },
        leadingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = TealAccent
                )
            )
        },
        headlineContent = { Text(text = title, color = primaryColor) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = secondaryColor) }
        }
    )
}

@Composable
fun SettingsExpandableChecklist(
    title: String,
    subtitle: String? = null,
    items: List<Pair<String, Boolean>>,
    onItemCheckedChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            headlineContent = { Text(text = title, color = primaryColor) },
            supportingContent = subtitle?.let {
                { Text(text = it, color = secondaryColor) }
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = secondaryColor
                )
            }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                val chunkedItems = items.withIndex().chunked(2)
                chunkedItems.forEach { rowItems ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { (index, itemPair) ->
                            val (itemTitle, isChecked) = itemPair
                            SettingsCheckboxItem(
                                title = itemTitle,
                                checked = isChecked,
                                onCheckedChange = { onItemCheckedChange(index, it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // 홀수 개수일 경우 빈 공간 채우기
                        if (rowItems.size < 2) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    modifier: Modifier = Modifier,
    valueFormatter: (Float) -> String = { it.toString() },
    defaultValue: Float? = null,
    subtitle: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = primaryColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (defaultValue != null) {
                    "현재 ${valueFormatter(value)} / 기본 ${valueFormatter(defaultValue)}"
                } else {
                    valueFormatter(value)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = TealAccent,
                activeTrackColor = TealAccent,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor
            )
        }
    }
}
