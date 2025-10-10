package io.syhids.vanguardian.sentinel

import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import io.syhids.vanguardian.sentinel.camera.CameraProcessor
import io.syhids.vanguardian.sentinel.ml.DetModel
import io.syhids.vanguardian.sentinel.ml.Recognition
import io.syhids.vanguardian.sentinel.notification.NtfyShNotificationSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private val TAG = SentinelService::class.java.simpleName
private val MinTimeBetweenNotifications = 8.seconds

class SentinelService : LifecycleService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val DefaultPhotoInterval = 4.seconds
    private var takingPhotoIntervalSeconds: Int = DefaultPhotoInterval.inWholeSeconds.toInt()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "$TAG started")

        val defaultIntervalSeconds = DefaultPhotoInterval.inWholeSeconds.toInt()

        takingPhotoIntervalSeconds = intent?.getIntExtra("interval", defaultIntervalSeconds)
            ?: defaultIntervalSeconds

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sentinel Active")
            .setContentText("Taking camera photos every $takingPhotoIntervalSeconds seconds")
            .build()

        ServiceCompat.startForeground(this, 123123, notification, FOREGROUND_SERVICE_TYPE_CAMERA)

        launchDetectionLoop()

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(TAG, "Binding it to $intent")
        return super.onBind(intent)
    }

    private val notificationSender =
        NtfyShNotificationSender(scope, minTimeBetweenNotifications = MinTimeBetweenNotifications)
    private var cameraProcessor: CameraProcessor<List<Recognition>>? = null
        set(value) {
            field?.close()
            field = value
        }

    private fun launchDetectionLoop() {
        scope.launch {
            val mlModel = DetModel(this@SentinelService, onResults = { recognitions, proxy ->
                Log.v(TAG, "Recognitions: " + recognitions.toDebugString())

                if (notificationSender.canSendNotification() && recognitions.any { it.id == "person" && it.confidence > 0.5f }) {
                    Log.v(TAG, "Sending notification")
                    notificationSender.sendNotification(recognitions, proxy)
                }
            })
            cameraProcessor = CameraProcessor(mlModel).also {
                it.startProcessing(this@SentinelService, this@SentinelService)
            }
        }
    }

    private fun List<Recognition>.toDebugString(): String =
        joinToString { "${it.title}(${it.confidence.with2Decimals})" }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel("Service destroyed")
        cameraProcessor = null
    }
}
