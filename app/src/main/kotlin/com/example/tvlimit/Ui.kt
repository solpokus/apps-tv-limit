@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.tv.foundation.ExperimentalTvFoundationApi::class
)
package com.example.tvlimit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.Card

@Composable
fun TvLauncherScreen(
    blockedPackages: Set<String>,
    onLaunch: (String) -> Unit,
    allApps: List<AppInfo>,
) {
    val visibleApps = remember(blockedPackages, allApps) {
        allApps.filter { it.packageName !in blockedPackages }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
    ) {
        HomeHero()
        Spacer(Modifier.height(12.dp))

        Text(
            "Apps",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(5),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visibleApps) { item ->
                AppCard(
                    label = item.label,
                    packageName = item.packageName,
                    onClick = { onLaunch(item.packageName) }
                )
            }
        }
    }
}

@Composable
private fun HomeHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111A2E))
    ) {
        // Background image (your new hero)
        Image(
            painter = painterResource(id = R.drawable.home_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // Gradient overlay for contrast
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x66000000), Color(0x99000000)),
                        startY = 0f, endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text("TV Limit", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Block or limit apps like YouTube TV. Safer screen time for the family.",
                color = Color(0xFFE5ECFF),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AppCard(
    label: String,
    packageName: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .size(width = 240.dp, height = 150.dp)
            .shadow(8.dp, shape, clip = true)
            .clip(shape)
            .background(Color(0xFF16233E))
            .focusable(true, interactionSource = remember { MutableInteractionSource() }),
    ) {
        // Minimal card—use package name as subtitle
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Text(packageName, color = Color(0xFFB8C7E8), style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
    onSetPin: (String) -> Unit,           // you can keep as no-op now
    onRequestUsageAccess: () -> Unit,
    apps: List<AppInfo>
) {
    var pin by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    var minutes by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
    ) {
        SettingsHero()
        Spacer(Modifier.height(12.dp))

        // Permissions row
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onRequestUsageAccess) { Text("Open Usage Access Settings") }
            Spacer(Modifier.width(12.dp))
            Text("Grant access so we can count watch time.", color = Color(0xFFB8C7E8))
        }

        Spacer(Modifier.height(20.dp))

        // Block list & quotas
        Text(
            "Manage Apps",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(4),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.height(320.dp)
        ) {
            items(apps.size) { i ->
                val app = apps[i]
                val isBlocked = app.packageName in settings.blockedList
                Card(onClick = { selected = app.packageName }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(app.label, color = Color.White)
                        Text(app.packageName, color = Color(0xFFB8C7E8), style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isBlocked,
                                onCheckedChange = { onToggleBlock(app.packageName, it) }
                            )
                            Text(if (isBlocked) "Blocked" else "Shown", color = Color.White)
                        }
                    }
                }
            }
        }

        // Quota editor row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it.filter(Char::isDigit) },
                label = { Text("Daily minutes quota") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val pkg = selected ?: return@Button
                val min = minutes.toIntOrNull() ?: return@Button
                onSetQuota(pkg, min)
            }) { Text("Save quota") }
            Spacer(Modifier.width(12.dp))
            Text(
                "Current quotas: " + settings.quotasList.joinToString { "${it.packageName}=${it.minutes}m" },
                color = Color(0xFFB8C7E8)
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111A2E))
    ) {
        Image(
            painter = painterResource(id = R.drawable.settings_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x66000000), Color(0x99000000))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text("Settings", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Choose apps to block and set daily time limits.", color = Color(0xFFE5ECFF))
        }
    }
}