package io.syhids.vanguardian.sentinel.notification

import android.util.Log
import androidx.camera.core.ImageProxy
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.syhids.vanguardian.sentinel.BuildConfig
import io.syhids.vanguardian.sentinel.ml.Recognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration

private val TAG = NtfyShNotificationSender::class.java.simpleName

class NtfyShNotificationSender(
    private val scope: CoroutineScope,
    private val minTimeBetweenNotifications: Duration
) {
    private val notificationBuilder = NotificationBuilder()

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    private var lastSentTimeMillis = 0L

    fun canSendNotification(): Boolean {
        return System.currentTimeMillis() - lastSentTimeMillis > minTimeBetweenNotifications.inWholeMilliseconds
    }

    fun sendNotification(recognitions: List<Recognition>, image: ImageProxy) {
        val body = notificationBuilder.buildText(recognitions)
        val imageData = notificationBuilder.buildImage(image)
        lastSentTimeMillis = System.currentTimeMillis()
        scope.launch {

            if (imageData == null) {
                Log.w(TAG, "No photo could be retrieved!")
                sendTextNotification(body = body, title = "Person detected (no photo)")
            } else {
                // Ntfy.sh can't send a notification with text and imagen at the same time
                sendTextNotification(body = body, title = "Person detected")
                sendImageNotification(imageData)
            }
        }
    }

    private suspend fun sendTextNotification(body: String, title: String?) {
        httpClient.post {
            url(BuildConfig.NTFY_URL)
            setBody(body)
            headers {
                title?.let { title ->
                    append("Title", title)
                }
            }
        }
    }

    private suspend fun sendImageNotification(data: ByteArray) {
        httpClient.put {
            url(BuildConfig.NTFY_URL)
            setBody(data)
            headers {
                append("Filename", "image.jpg")
            }
        }
    }
}
