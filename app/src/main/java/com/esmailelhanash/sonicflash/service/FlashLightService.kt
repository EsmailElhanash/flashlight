package com.esmailelhanash.sonicflash.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.esmailelhanash.sonicflash.R
import com.esmailelhanash.sonicflash.data.camera.CameraFactory
import com.esmailelhanash.sonicflash.data.flashlight.FlashPatternPlayer
import com.esmailelhanash.sonicflash.data.model.FlashPattern
import com.esmailelhanash.sonicflash.data.model.defaultFlashPatterns
import com.esmailelhanash.sonicflash.domain.camera.ICamera
import com.esmailelhanash.sonicflash.presentation.ui.MainActivity
import com.esmailelhanash.sonicflash.presentation.viewmodel.FlashlightViewModel
import com.esmailelhanash.sonicflash.presentation.viewmodel.FlashlightViewModelFactory

class FlashlightService : LifecycleService(), ViewModelStoreOwner {
    private val notificationChannelId = "my_notification_channel"

    private lateinit var cameraController: ICamera
    private val binder = LocalBinder()

    // toggle flash action
    private val toggleAction = "toggle"
    private val stopAction = "stop"

    // FlashlightViewModel instance
    private lateinit var flashlightViewModel : FlashlightViewModel

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): FlashlightService = this@FlashlightService
    }
    override fun onCreate() {
        super.onCreate()



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        startForeground(1, createNotification())
        initializeCameraController()

        flashlightViewModel = FlashlightViewModelFactory(
            FlashPatternPlayer(cameraController)
        ).create(FlashlightViewModel::class.java)

        flashlightViewModel.setFlashPattern(defaultFlashPatterns.first())

    }

    private fun initializeCameraController() {
        cameraController = CameraFactory.getCamera(this)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // if toggle command, toggle the flash
        if (intent?.action == toggleAction) {
            toggleFlash()
        }else if (intent?.action == stopAction) {
            stopSelf()
            // if flash on, turn off
            if (flashlightViewModel.isFlashLightOn.value == true) {
                flashlightViewModel.toggleFlash()
            }
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(notificationChannelId, "My Channel", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SonicFlash")
            .setContentText("SonicFlash is running")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

            .addAction(
                R.drawable.ic_launcher_foreground,  // Replace with your icon
                "Toggle Flashlight",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, FlashlightService::class.java).apply {
                        setAction(toggleAction)
                    },
                    PendingIntent.FLAG_MUTABLE
                )
            ).addAction(
                android.R.drawable.ic_menu_close_clear_cancel,  // Replace with your icon
                "Stop",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, FlashlightService::class.java).apply {
                        setAction(stopAction)
                    },
                    PendingIntent.FLAG_MUTABLE
                )
            )
            .setContentIntent(
                // MainActivity intent
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_MUTABLE
                )
            )
        // Add actions or other customizations as needed

        return builder.build()
    }

    override val viewModelStore: ViewModelStore
        get() = ViewModelStore()

    fun setFlashPattern(flashPattern: FlashPattern) {
        flashlightViewModel.setFlashPattern(flashPattern)
    }

    fun toggleFlash() {
        flashlightViewModel.toggleFlash()
    }

    fun isFlashOn(): LiveData<Boolean> {
        return flashlightViewModel.isFlashLightOn
    }

    fun getFlashPattern() : LiveData<FlashPattern> {
        return flashlightViewModel.flashPattern
    }

}
