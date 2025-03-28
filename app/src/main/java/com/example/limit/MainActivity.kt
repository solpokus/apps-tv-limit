package com.example.limit

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRequestPermission = findViewById<Button>(R.id.btnRequestPermission)
        val btnCheckUsage = findViewById<Button>(R.id.btnCheckUsage)

        // Request Usage Access Permission
        btnRequestPermission.setOnClickListener {
            requestUsagePermission()
        }

        // Check and display the most used app
        btnCheckUsage.setOnClickListener {
            val mostUsedApp = getMostUsedApp()
            Toast.makeText(this, "Most Used App: $mostUsedApp", Toast.LENGTH_LONG).show()
        }
    }

    // Function to request Usage Access permission
    private fun requestUsagePermission() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Usage access settings not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening settings", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to get the most used app
    private fun getMostUsedApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60  // Check last 1 hour usage

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        return stats.maxByOrNull { it.totalTimeInForeground }?.packageName ?: "No data"
    }
}