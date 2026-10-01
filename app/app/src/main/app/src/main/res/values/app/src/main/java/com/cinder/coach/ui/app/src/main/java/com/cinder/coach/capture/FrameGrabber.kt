// language: Kotlin, file: FrameGrabber.kt
// pulls frames from MediaProjection VirtualDisplay into a reusable Bitmap
package com.cinder.coach.capture

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import java.nio.ByteBuffer

class FrameGrabber(
    private val projection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val densityDpi: Int,
    private val onFrame: (Bitmap) -> Unit
) {
    private val thread = HandlerThread("grabber").apply { start() }
    private val handler = Handler(thread.looper)

    private lateinit var reader: ImageReader
    private lateinit var display: VirtualDisplay

    fun start() {
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        reader.setOnImageAvailableListener({ r ->
            val image: Image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                onFrame(image.toBitmap(width, height))
            } catch (_: Throwable) {
                // drop frame, keep going
            } finally {
                image.close()
            }
        }, handler)

        display = projection.createVirtualDisplay(
            "cinder-capture",
            width, height, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, handler
        )
    }

    fun stop() {
        try { display.release() } catch (_: Throwable) {}
        try { reader.close() } catch (_: Throwable) {}
        thread.quitSafely()
    }

    private fun Image.toBitmap(w: Int, h: Int): Bitmap {
        val plane = planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * w

        val bmp = Bitmap.createBitmap(
            w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(buffer)

        return if (rowPadding == 0) bmp
        else Bitmap.createBitmap(bmp, 0, 0, w, h).also { bmp.recycle() }
    }
}
