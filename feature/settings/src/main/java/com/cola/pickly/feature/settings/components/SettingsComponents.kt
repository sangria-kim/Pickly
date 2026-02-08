package com.cola.pickly.feature.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
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

    val itemModifier = if (subtitle != null) {
        modifier.fillMaxWidth().heightIn(min = 52.dp, max = 60.dp)
    } else {
        modifier.fillMaxWidth().heightIn(min = 40.dp, max = 48.dp)
    }

    ListItem(
        modifier = itemModifier
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
        headlineContent = {
            Text(
                text = title,
                color = primaryColor,
                style = MaterialTheme.typography.labelLarge
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = secondaryColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
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
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        headlineContent = {
            Text(
                text = title,
                color = primaryColor,
                style = MaterialTheme.typography.labelLarge
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = secondaryColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
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
        headlineContent = {
            Text(
                text = title,
                color = primaryColor,
                style = MaterialTheme.typography.labelLarge
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = secondaryColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailingContent = {
            Text(
                text = value,
                color = secondaryColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}

@Composable
fun SettingsActionItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    showChevron: Boolean = false,
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
        headlineContent = {
            Text(
                text = title,
                color = primaryColor,
                style = MaterialTheme.typography.labelLarge
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = secondaryColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailingContent = if (trailingText != null || showChevron) {
            {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    if (trailingText != null) {
                        Text(
                            text = trailingText,
                            color = secondaryColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (showChevron) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = secondaryColor
                        )
                    }
                }
            }
        } else null
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
        headlineContent = {
            Text(
                text = title,
                color = primaryColor,
                style = MaterialTheme.typography.labelLarge
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = secondaryColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
}

@Composable
fun SettingsExpandableChecklist(
    title: String,
    subtitle: String? = null,
    items: List<Pair<String, Boolean>>,
    onItemCheckedChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            headlineContent = { Text(text = title, style = MaterialTheme.typography.labelLarge, color = secondaryColor) },
            supportingContent = subtitle?.let {
                { Text(text = it, style = MaterialTheme.typography.bodySmall, color = secondaryColor) }
            },
            trailingContent = {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    if (trailingLabel != null) {
                        Text(text = trailingLabel, style = MaterialTheme.typography.bodySmall, color = secondaryColor)
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = secondaryColor
                    )
                }
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
    val accentColor = MaterialTheme.colorScheme.primary

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
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                        append(valueFormatter(value))
                    }
                    if (defaultValue != null) {
                        withStyle(style = SpanStyle(color = secondaryColor)) {
                            append(" 기본 ${valueFormatter(defaultValue)}")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
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

@Composable
fun SettingsCardSection(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Card(
        modifier = modifier.padding(horizontal = 16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column { content() }
    }
}

@Composable
fun SettingsCardHeader(title: String, description: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun SettingsCardTuningHeader(title: String, actionText: String, onActionClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp).fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = actionText,
            style = MaterialTheme.typography.bodyMedium,
            color = TealAccent,
            modifier = Modifier.clickable(onClick = onActionClick)
        )
    }
}

@Composable
fun SettingsDebugSectionTitle(modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Text(
            text = "디버그",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        com.cola.pickly.core.ui.components.PillBadge(
            text = "개발/QA 전용",
            backgroundColor = Color(0xFFFFEBEE),
            textColor = Color(0xFFD32F2F),
            fontSize = 10.sp,
            height = 20.dp
        )
    }
}
