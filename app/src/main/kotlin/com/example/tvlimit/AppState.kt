package com.example.tvlimit

import android.content.Context
import com.example.tvlimit.proto.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

object AppState {
    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var appContext: Context
    private val _settings = MutableStateFlow(Settings.getDefaultInstance())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            appContext.settingsStore.data.collect { _settings.value = it }
        }
    }

    fun blockedFlow(): Flow<Set<String>> = settings.map { it.blockedList.toSet() }
    fun quotasFlow(): Flow<Map<String, Int>> = settings.map { it.quotasList.associate { q -> q.packageName to q.minutes } }
    fun pinHashFlow(): Flow<Pair<String?, String?>> = settings.map { it.pinHash to it.pinSalt }

    suspend fun update(transform: (Settings) -> Settings) {
        appContext.settingsStore.updateData(transform)
        QuotaEnforcerWorker.enqueueNow(appContext)
    }
}
