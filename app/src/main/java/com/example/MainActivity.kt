package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.services.NusuVoiceService
import com.example.ui.theme.MyApplicationTheme
import com.example.api.PlatformManager

class MainActivity : FragmentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startNusuVoiceService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformManager.initialize(applicationContext)
        enableEdgeToEdge()
        
        // Lock screen availability and wake support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        setContent {
            MyApplicationTheme {
                NusuApp(
                    onStartVoiceService = {
                        checkAndRequestPermissions()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NusuVoiceService.isAppInForeground = true
        NusuVoiceService.instance?.onAppForegroundStateChanged(true)
        NusuVoiceService.instance?.checkAndShowFloatingWindow()
    }

    override fun onPause() {
        super.onPause()
        NusuVoiceService.isAppInForeground = false
        NusuVoiceService.instance?.onAppForegroundStateChanged(false)
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            startNusuVoiceService()
        }
    }

    private fun startNusuVoiceService() {
        val serviceIntent = Intent(this, NusuVoiceService::class.java).apply {
            action = NusuVoiceService.ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
