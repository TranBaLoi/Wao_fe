package com.example.wao_fe.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord

object HealthConnectManager {

    const val providerPackageName = "com.google.android.apps.healthdata"

    val stepReadPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    // Keep the requested permissions in one place so checks and request dialogs stay consistent.
    val readPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
    )

    fun getSdkStatus(context: Context): Int {
        return HealthConnectClient.getSdkStatus(context, providerPackageName)
    }

    fun buildInstallIntent(context: Context): Intent {
        val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
        return Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse(uriString)
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
        }
    }

    fun buildBrowserFallbackIntent(): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$providerPackageName"),
        )
    }
}
