package com.cola.pickly.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cola.pickly.ui.theme.Gray100
import com.cola.pickly.ui.theme.TextPrimary
import com.cola.pickly.ui.theme.TextSecondary

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
        HorizontalDivider(color = Gray100)
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
        color = TextSecondary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = Gray100)
}

@Composable
fun SettingsRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .clickable(onClick = onClick),
        leadingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        headlineContent = { Text(text = title, color = TextPrimary) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = TextSecondary) }
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
    ListItem(
        modifier = modifier.fillMaxWidth(),
        headlineContent = { Text(text = title, color = TextPrimary) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = TextSecondary) }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
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
    ListItem(
        modifier = modifier.fillMaxWidth(),
        headlineContent = { Text(text = title, color = TextPrimary) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = TextSecondary) }
        },
        trailingContent = { Text(text = value, color = TextSecondary) }
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
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
            }
            .clickable(enabled = enabled, onClick = onClick),
        headlineContent = { Text(text = title, color = if (enabled) TextPrimary else TextSecondary) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = TextSecondary) }
        },
        trailingContent = trailingText?.let { tt ->
            { Text(text = tt, color = TextSecondary) }
        }
    )
}


