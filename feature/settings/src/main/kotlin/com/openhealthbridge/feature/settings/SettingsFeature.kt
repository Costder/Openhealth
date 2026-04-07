package com.openhealthbridge.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SettingAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun SettingsFeature(
    title: String,
    subtitle: String,
    statuses: List<Pair<String, String>>,
    actions: List<SettingAction>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    statuses.forEach { (label, value) ->
                        Text("$label: $value", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { action ->
                    Button(onClick = action.onClick, enabled = action.enabled, modifier = Modifier.fillMaxWidth()) {
                        Text(action.label)
                    }
                }
            }
        }
        if (statuses.isNotEmpty()) {
            item {
                Text("Recent status", style = MaterialTheme.typography.titleMedium)
            }
            items(statuses) { (label, value) ->
                Text("$label: $value", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
