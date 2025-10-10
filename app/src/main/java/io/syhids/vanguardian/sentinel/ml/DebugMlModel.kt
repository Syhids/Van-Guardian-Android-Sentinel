package io.syhids.vanguardian.sentinel.ml

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import io.syhids.vanguardian.sentinel.camera.CameraProcessor
import io.syhids.vanguardian.sentinel.with2Decimals

private val TAG = DebugMlModel::class.java.simpleName

class DebugMlModel(context: Context) : CameraProcessor.Listener<List<Recognition>> {
    private val detModel = DetModel(context, onResults = { _, _ -> })

    override fun processImage(image: ImageProxy): List<Recognition> {
        val ret = detModel.processImage(image)
        Log.i(TAG, "Processing image ${image.imageInfo.timestamp} on ${Thread.currentThread().name}. ${ret.toDebug()}")
        return ret
    }

    override fun runResults(t: List<Recognition>, proxy: ImageProxy) {
        Log.i("DebugMlModel", "Running results on ${Thread.currentThread().name}")
    }

    override fun close() {
        detModel.close()
    }

    fun List<Recognition>.toDebug() = this.sortedByDescending { it.confidence }
        .groupBy { it.title }
        .map { (title, recognitions) ->
            if (recognitions.size > 1) {
                "$title(${recognitions.size})[${recognitions.first().confidence.with2Decimals}]"
            } else {
                "$title[${recognitions.first().confidence.with2Decimals}]"
            }
        }
        .joinToString()
}