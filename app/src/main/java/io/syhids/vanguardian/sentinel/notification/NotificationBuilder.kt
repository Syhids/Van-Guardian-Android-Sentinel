package io.syhids.vanguardian.sentinel.notification

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import io.syhids.vanguardian.sentinel.ml.Recognition
import io.syhids.vanguardian.sentinel.toByteArray

class NotificationBuilder {
    fun buildText(recognitions: List<Recognition>): String = buildString {
        append("Someone is inside!")

        val recognitionsWithConfidence = recognitions
            .filter { it.confidence > 0.5 }

        if (recognitionsWithConfidence.size > 1) {
            append(
                " Recognized also: ${
                    recognitionsWithConfidence
                        .sortedByDescending { it.confidence }
                        .groupBy { it.title }
                        .map { (title, recognitions) ->
                            if (recognitions.size > 1) {
                                "$title(${recognitions.size})"
                            } else {
                                title
                            }
                        }
                        .joinToString()
                }")
        }
    }

    fun buildImage(proxy: ImageProxy): ByteArray? = proxy.toByteArray(format = Bitmap.CompressFormat.JPEG, quality = 100)
}