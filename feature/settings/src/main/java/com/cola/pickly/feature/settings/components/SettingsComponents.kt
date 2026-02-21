package com.cola.pickly.feature.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cola.pickly.core.model.SensitivityLevel
import com.cola.pickly.core.ui.theme.TealAccent
import kotlin.math.roundToInt
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

    val minHeight = if (subtitle != null) 72.dp else 56.dp

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
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
                    checkedColor = TealAccent,
                    checkmarkColor = Color.White,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
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
    val accentColor = TealAccent

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
                style = MaterialTheme.typography.labelLarge,
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
            steps = 0,  // tick marks 제거
            colors = SliderDefaults.colors(
                thumbColor = TealAccent,
                activeTrackColor = TealAccent
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
    Column(modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
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
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SettingsCardTuningHeader(title: String? = null, actionText: String, onActionClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp).fillMaxWidth(),
        horizontalArrangement = if (title != null) Arrangement.SpaceBetween else Arrangement.End
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = actionText,
            style = MaterialTheme.typography.bodyMedium,
            color = TealAccent,
            modifier = Modifier.clickable(onClick = onActionClick)
        )
    }
}

/**
 * 스마트 제외 기준 항목 — 체크박스 + 아코디언 민감도 슬라이더.
 *
 * - [sensitivityLevel]이 null이면 슬라이더를 표시하지 않습니다(OCCLUDED, CROPPED).
 * - 슬라이더는 접힌 상태(기본)에서 민감도 레벨 텍스트 + 화살표를 표시합니다.
 * - 체크 해제 시 슬라이더가 자동으로 접힙니다.
 * - 7단계 Discrete Slider: steps = 5 (Compose Slider steps = 총 단계 수 - 2)
 */
@Composable
fun SmartDiscardCriterionItem(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    sensitivityLevel: SensitivityLevel?,
    onSensitivityChange: ((SensitivityLevel) -> Unit)?,
    sensitivityDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val hasSensitivity = sensitivityLevel != null && onSensitivityChange != null

    var isSliderExpanded by remember { mutableStateOf(false) }

    // 체크 해제 시 슬라이더 자동 접힘
    LaunchedEffect(isChecked) {
        if (!isChecked) isSliderExpanded = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 체크박스 + 기준 이름 + 민감도 레벨/화살표
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!isChecked) }
                .padding(horizontal = 16.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = TealAccent,
                    checkmarkColor = Color.White,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = label,
                color = primaryColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            if (hasSensitivity) {
                Row(
                    modifier = Modifier
                        .clickable(enabled = isChecked) {
                            isSliderExpanded = !isSliderExpanded
                        }
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sensitivityLevel!!.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isChecked) TealAccent else secondaryColor.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isSliderExpanded && isChecked) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isChecked) TealAccent else secondaryColor.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // 민감도 슬라이더 (체크 상태 + 펼침 상태일 때만 표시)
        if (hasSensitivity) {
            AnimatedVisibility(
                visible = isChecked && isSliderExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    if (!sensitivityDescription.isNullOrBlank()) {
                        Text(
                            text = sensitivityDescription,
                            style = MaterialTheme.typography.labelMedium,
                            color = secondaryColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // 방향 라벨을 슬라이더 좌우 바깥으로 배치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "덜 엄격",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor
                        )
                        Slider(
                            value = (sensitivityLevel.step - 1).toFloat(),
                            onValueChange = { raw ->
                                val step = raw.roundToInt() + 1
                                onSensitivityChange!!(SensitivityLevel.fromStep(step))
                            },
                            valueRange = 0f..6f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = TealAccent,
                                activeTrackColor = TealAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "더 엄격",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor
                        )
                    }

                    // 보조 문구 (모든 단계에 문구 배정, 높이 고정)
                    val hint = when (sensitivityLevel.step) {
                        1 -> "확실히 문제 있는 사진만 잡아요"
                        2 -> "눈에 띄는 사진 위주로 잡아요"
                        3 -> "조금 여유 있게 판단해요"
                        4 -> "적당한 기준으로 판단해요"
                        5 -> "꼼꼼하게 살펴봐요"
                        6 -> "까다롭게 골라내요"
                        7 -> "조금이라도 아쉬우면 잡아요"
                        else -> "적당한 기준으로 판단해요"
                    }
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}


