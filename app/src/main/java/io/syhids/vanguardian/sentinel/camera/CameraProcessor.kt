package io.syhids.vanguardian.sentinel.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class CameraProcessor<T>(val listener: Listener<T>) {
    private val executor = Executors.newSingleThreadExecutor()
    private val executorDispatcher = executor.asCoroutineDispatcher()

    interface Listener<T> {
        fun processImage(image: ImageProxy): T
        fun runResults(t: T, proxy: ImageProxy)
        fun close()
    }

    fun close() {
        listener.close()
        executor.shutdown()
        isRunning = false
    }

    var isRunning: Boolean = false

    fun startProcessing(context: Context, lifecycleOwner: LifecycleOwner) = CoroutineScope(executorDispatcher).launch {
        val imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageRotationEnabled(true)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) {
            processFrame(it)
        }

        withContext(Dispatchers.Main) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                CoroutineScope(executorDispatcher).launch {
                    withContext(Dispatchers.Main) {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
                        isRunning = true
                    }
                }
            }, executor)
        }
    }

    private fun processFrame(proxy: ImageProxy) = CoroutineScope(executorDispatcher).launch {
        try {
            val t = startProcessing(proxy)
            withContext(Dispatchers.Main) {
                listener.runResults(t, proxy)
            }
        } finally {
            proxy.close()
        }
    }

    private suspend fun startProcessing(proxy: ImageProxy): T {
        return withContext(executorDispatcher) {
            listener.processImage(proxy)
        }
    }
}