package com.example.tvlimit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.tvlimit.proto.Settings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val allApps = remember { AppList.loadInstalledLaunchables(this) }
                var tab by remember { mutableStateOf("home") }

                val settings by AppState.settings.collectAsState()

                Surface {
                    androidx.compose.foundation.layout.Column {
                        TopBar(tab) { tab = it }
                        if (tab == "home") {
                            TvLauncherScreen(
                                blockedPackages = settings.blockedList.toSet(),
                                onLaunch = { pkg -> PackageUtils.launchPackage(this, pkg) },
                                allApps = allApps
                            )
                        } else {
                            PinGate(settings.pinHash, settings.pinSalt) {
                                SettingsScreen(
                                    settings = settings,
                                    onToggleBlock = { pkg, on ->
                                        scope.launch {
                                            AppState.update { cur ->
                                                val builder = cur.toBuilder().clearBlocked()
                                                val current = cur.blockedList.toMutableSet()
                                                if (on) current.add(pkg) else current.remove(pkg)
                                                builder.addAllBlocked(current.distinct())
                                                builder.build()
                                            }
                                        }
                                    },
                                    onSetQuota = { pkg, minutes ->
                                        scope.launch {
                                            AppState.update { cur ->
                                                val builder = cur.toBuilder().clearQuotas()
                                                val map = cur.quotasList.associateBy { it.packageName }.toMutableMap()
                                                map[pkg] = com.example.tvlimit.proto.Quota.newBuilder()
                                                    .setPackageName(pkg).setMinutes(minutes).build()
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
                                                cur.toBuilder().setPinSalt(salt).setPinHash(hash).build()
                                            }
                                        }
                                    },
                                    onRequestUsageAccess = { startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
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
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(label = { Text("Home") }, selected = current == "home", onClick = { onSelect("home") })
        FilterChip(label = { Text("Settings") }, selected = current == "settings", onClick = { onSelect("settings") })
    }
}
