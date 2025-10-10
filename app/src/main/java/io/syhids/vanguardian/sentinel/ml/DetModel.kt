package io.syhids.vanguardian.sentinel.ml

import android.content.Context
import androidx.camera.core.ImageProxy
import io.syhids.vanguardian.sentinel.camera.CameraProcessor
import org.tensorflow.lite.support.image.TensorImage

class DetModel(context: Context, val onResults: (List<Recognition>, ImageProxy) -> Unit) :
    CameraProcessor.Listener<List<Recognition>> {
    private val model = EfficientdetLite4Metadata.newInstance(context)

    override fun processImage(image: ImageProxy): List<Recognition> {
        val tensorImage = TensorImage.fromBitmap(image.toBitmap())
        val ret = model.process(tensorImage).detectionResultList
            .filter { it.scoreAsFloat > 0.5f }
            .sortedByDescending { it.scoreAsFloat }
            .take(3)
            .map {
                Recognition(
                    id = it.categoryAsString,
                    title = it.categoryAsString,
                    confidence = it.scoreAsFloat
                )
            }

        return ret
    }

    override fun runResults(t: List<Recognition>, proxy: ImageProxy) {
        if (t.isNotEmpty()) {
            onResults(t, proxy)
        }
    }

    override fun close() {
        model.close()
    }
}