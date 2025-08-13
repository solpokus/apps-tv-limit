package com.example.tvlimit

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings //
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val allApps = remember(context) { AppList.loadInstalledLaunchables(context) }
                var tab by remember { mutableStateOf("home") }

                val settings by AppState.settings.collectAsState()

                Surface {
                    Column {
                        TopBar(tab) { tab = it }
                        if (tab == "home") {
                            TvLauncherScreen(
                                blockedPackages = settings.blockedList.toSet(),
                                onLaunch = { pkg -> PackageUtils.launchPackage(context, pkg) },
                                allApps = allApps
                            )
                        } else {
                            PinGate(settings.pinHash, settings.pinSalt) {
                                SettingsScreenTv(
                                    settings = settings,
                                    onToggleBlock = { pkg, on ->
                                        scope.launch {
                                            // 1) Persist UI state
                                            AppState.update { cur ->
                                                val builder = cur.toBuilder().clearBlocked()
                                                val current = cur.blockedList.toMutableSet()
                                                if (on) current.add(pkg) else current.remove(pkg)
                                                builder.addAllBlocked(current.distinct())
                                                builder.build()
                                            }

                                            // 2) Try to hard-block at the system level right away
                                            val hard = DeviceOwner.setPackageBlocked(context, pkg, on)

                                            // 3) If not hard-blocked, ensure Accessibility soft-block is your safety net
                                            //    (No code needed here—your SoftBlockService already handles it,
                                            //     but remind the user to enable the accessibility service.)
                                        }
                                    },
                                    onSetQuota = { pkg, minutes ->
                                        scope.launch {
                                            AppState.update { cur ->
                                                val builder = cur.toBuilder().clearQuotas()
                                                val map = cur.quotasList
                                                    .associateBy { it.packageName }
                                                    .toMutableMap()
                                                map[pkg] = com.example.tvlimit.proto.Quota
                                                    .newBuilder()
                                                    .setPackageName(pkg)
                                                    .setMinutes(minutes)
                                                    .build()
                                                builder.addAllQuotas(map.values)
                                                builder.build()
                                            }
                                        }
                                    },
                                    onSetPin = { newPin ->
                                        scope.launch {
                                            val salt = Security.randomSalt()
                                            val hash = Security.hashPin(newPin, salt)
                                            AppState.update { cur ->
                                                cur.toBuilder()
                                                    .setPinSalt(salt)
                                                    .setPinHash(hash)
                                                    .build()
                                            }
                                        }
                                    },
                                    onRequestUsageAccess = {
                                        // ✅ Uses AndroidSettings alias to avoid proto.Settings clash
                                        startActivity(
                                            Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)
                                        )
                                    },
                                    apps = allApps
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            label = { Text("Home") },
            selected = current == "home",
            onClick = { onSelect("home") }
        )
        FilterChip(
            label = { Text("Settings") },
            selected = current == "settings",
            onClick = { onSelect("settings") }
        )
    }
}
