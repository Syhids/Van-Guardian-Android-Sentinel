package io.syhids.vanguardian.sentinel

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat

fun ImageProxy.toByteArray(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): ByteArray? {
    try {
        val bitmap = toBitmap()
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        return outputStream.toByteArray()
    } catch (e: IllegalStateException) {
        // Sometimes toBitmap throws it
        e.printStackTrace()
        return null
    } catch (e: NullPointerException) {
        // Sigh.. toBitmap throws this:
        // java.lang.NullPointerException: Attempt to invoke virtual method 'java.nio.Buffer java.nio.ByteBuffer.rewind()' on a null object reference
        e.printStackTrace()
        return null
    }
}

@Suppress("DEPRECATION") // Deprecated for third party Services.
inline fun <reified T> Context.isServiceRunning() =
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == T::class.java.name }

private val decimalFormat = DecimalFormat.getInstance().also { it.maximumFractionDigits = 2 }

val Float.with2Decimals: String
    get() = decimalFormat.format(this)