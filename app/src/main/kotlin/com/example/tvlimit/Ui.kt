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
fun PinGate(
    pinHash: String?,
    pinSalt: String?,
    onSetPin: (String) -> Unit,     // NEW: needed to save the very first PIN
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // If no pin hash/salt -> we are in "create new PIN" mode
    val needsCreate = pinHash.isNullOrEmpty() || pinSalt.isNullOrEmpty()

    var input by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var unlocked by remember { mutableStateOf(false) }

    if (unlocked) {
        content()
        return
    }

    androidx.compose.foundation.layout.Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (needsCreate) {
            Text("Create Parent PIN", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = input,
                onValueChange = {
                    error = null
                    if (it.length <= 6) input = it.filter(Char::isDigit)
                },
                label = { Text("New PIN (4–6 digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    error = null
                    if (it.length <= 6) confirm = it.filter(Char::isDigit)
                },
                label = { Text("Confirm PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                when {
                    input.length !in 4..6 -> error = "PIN must be 4–6 digits"
                    input != confirm       -> error = "PINs don’t match"
                    else -> {
                        onSetPin(input)     // persist PIN via caller (MainActivity)
                        unlocked = true
                        android.widget.Toast
                            .makeText(context, "PIN created", android.widget.Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }) { Text("Save & Continue") }

        } else {
            Text("Enter PIN to access Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    error = null
                    if (it.length <= 6) input = it.filter(Char::isDigit)
                },
                label = { Text("PIN (4–6 digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val ok = Security.hashPin(input, pinSalt!!) == pinHash
                if (ok) {
                    unlocked = true
                    error = null
                } else {
                    error = "Wrong PIN"
                    android.widget.Toast
                        .makeText(context, "Incorrect PIN", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            }) { Text("Submit PIN") }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: com.example.tvlimit.proto.Settings,
    onToggleBlock: (String, Boolean) -> Unit,
    onSetQuota: (String, Int) -> Unit,
    onSetPin: (String) -> Unit,
    onRequestUsageAccess: () -> Unit,
    apps: List<AppInfo>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pin by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    var minutes by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        // Permissions section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onRequestUsageAccess) { Text("Open Usage Access Settings") }
            Spacer(Modifier.width(12.dp))
            Text("Grant access so we can count watch time.", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))

        // App selection + block toggle
        Text("Choose app", style = MaterialTheme.typography.titleMedium)
        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(4),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(apps.size) { i ->
                val app = apps[i]
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

        // Quota editor
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it.filter { c -> c.isDigit() } },
                label = { Text("Daily minutes quota") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val pkg = selected ?: run {
                    android.widget.Toast
                        .makeText(context, "Select an app first", android.widget.Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }
                val min = minutes.toIntOrNull() ?: run {
                    android.widget.Toast
                        .makeText(context, "Enter valid minutes", android.widget.Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }
                onSetQuota(pkg, min)
                android.widget.Toast
                    .makeText(context, "Saved quota for $pkg: $min minutes/day", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }) { Text("Save quota") }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Current quotas: " +
                    settings.quotasList.joinToString { "${it.packageName}=${it.minutes}m" },
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))

        // PIN section
        Text("Parent PIN", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            if (settings.pinHash.isNullOrEmpty()) "No PIN set. Create one below."
            else "A PIN is set. Enter a new one to change.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { c -> c.isDigit() } },
                label = { Text("New PIN (4–6 digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (pin.length in 4..6) {
                    onSetPin(pin)
                    pin = ""
                    android.widget.Toast
                        .makeText(context, "PIN saved", android.widget.Toast.LENGTH_SHORT)
                        .show()
                } else {
                    android.widget.Toast
                        .makeText(context, "PIN must be 4–6 digits", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            }) { Text("Save PIN") }
        }
    }
}
