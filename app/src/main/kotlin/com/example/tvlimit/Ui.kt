@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.tv.foundation.ExperimentalTvFoundationApi::class
)
package com.example.tvlimit

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items   // <-- ensure this is present
import androidx.tv.material3.Card
// Removed wildcard; explicitly alias proto Settings
import com.example.tvlimit.proto.Settings as ProtoSettings

@Composable
fun TvLauncherScreen(
    blockedPackages: Set<String>,
    onLaunch: (String) -> Unit,
    allApps: List<AppInfo>,
) {
    val visibleApps = remember(blockedPackages, allApps) {
        allApps.filter { it.packageName !in blockedPackages }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Apps", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(5),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleApps.size) { index ->
                val item = visibleApps[index]
                Card(
                    onClick = { onLaunch(item.packageName) },
                    modifier = Modifier.size(220.dp, 140.dp).focusable(true)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.label)
                        Spacer(Modifier.height(8.dp))
                        Text(item.packageName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PinGate(pinHash: String?, pinSalt: String?, content: @Composable () -> Unit) {
    var input by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(pinHash.isNullOrEmpty() || pinSalt.isNullOrEmpty()) }

    if (unlocked) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter PIN", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 6) input = it.filter { c -> c.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                // avoid NPE if salt is missing
                val ok = !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()
                        && Security.hashPin(input, pinSalt!!) == pinHash
                if (ok) unlocked = true
            }) { Text("Unlock") }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: ProtoSettings,   // <-- use proto Settings
    onToggleBlock: (String, Boolean) -> Unit,
    onSetQuota: (String, Int) -> Unit,
    onSetPin: (String) -> Unit,
    onRequestUsageAccess: () -> Unit,
    apps: List<AppInfo>
) {
    var pin by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    var minutes by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onRequestUsageAccess) { Text("Open Usage Access Settings") }
            Spacer(Modifier.width(12.dp))
            Text("Grant access so we can count watch time.", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))

        Text("Choose app", style = MaterialTheme.typography.titleMedium)
        val selectable = apps
        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(4),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(selectable.size) { i ->
                val app = selectable[i]
                val isBlocked = app.packageName in settings.blockedList
                Card(onClick = { selected = app.packageName }) {
                    Column(Modifier.padding(8.dp)) {
                        Text(app.label)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isBlocked,
                                onCheckedChange = { onToggleBlock(app.packageName, it) }
                            )
                            Text(if (isBlocked) "Blocked" else "Shown")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it.filter { c -> c.isDigit() } },
                label = { Text("Daily minutes quota") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val pkg = selected ?: return@Button
                val min = minutes.toIntOrNull() ?: return@Button
                onSetQuota(pkg, min)
            }) { Text("Save quota for selected") }
            Spacer(Modifier.width(12.dp))
            Text(
                "Current quotas: " + settings.quotasList.joinToString { "${it.packageName}=${it.minutes}m" },
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Set/Change PIN", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { c -> c.isDigit() } },
                label = { Text("PIN") }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onSetPin(pin) }) { Text("Save PIN") }
        }
    }
}
