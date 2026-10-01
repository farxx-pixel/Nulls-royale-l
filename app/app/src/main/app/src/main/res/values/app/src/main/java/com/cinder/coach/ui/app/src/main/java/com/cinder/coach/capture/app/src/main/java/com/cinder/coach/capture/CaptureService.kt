// language: Kotlin, file: CaptureService.kt
// foreground service: holds projection, runs analyze->advise->overlay loop
package com.cinder.coach.capture

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cinder.coach.logic.Advisor
import com.cinder.coach.logic.Suggestion
import com.cinder.coach.overlay.AdvisorOverlay
import com.cinder.coach.vision.FrameAnalyzer

class CaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "rc"
        const val EXTRA_RESULT_DATA = "rd"
        const val CHANNEL_ID = "cinder_capture"
        const val NOTIF_ID = 4242
    }

    private lateinit var projection: MediaProjection
    private lateinit var grabber: FrameGrabber
    private lateinit var analyzer: FrameAnalyzer
    private lateinit var overlay: AdvisorOverlay

    private var lastProcessed = 0L
    private val minFrameGapMs = 250L

    private var lastSuggestion: Suggestion? = null
    private var stableCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        @Suppress("DEPRECATION")
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode == -1 || resultData == null) { stopSelf(); return START_NOT_STICKY }

        startForegroundCompat()

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, resultData)
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stopSelf() }
        }, null)

        val dm = resources.displayMetrics
        analyzer = FrameAnalyzer(dm.widthPixels, dm.heightPixels)
        overlay = AdvisorOverlay(this).also { it.show() }

        grabber = FrameGrabber(projection, dm.widthPixels, dm.heightPixels, dm.densityDpi) { bmp ->
            val now = System.currentTimeMillis()
            if (now - lastProcessed < minFrameGapMs) { bmp.recycle(); return@FrameGrabber }
            lastProcessed = now
            handleFrame(bmp)
        }
        grabber.start()
        return START_NOT_STICKY
    }

    private fun handleFrame(bmp: Bitmap) {
        try {
            val state = analyzer.analyze(bmp)
            val call = Advisor.decide(state)

            // debounce: require 2 consecutive identical suggestions before HUD update
            if (call == lastSuggestion) stableCount++ else { lastSuggestion = call; stableCount = 0 }
            if (stableCount >= 2 || overlay.isFirst()) overlay.update(call)
        } catch (_: Throwable) {
            // swallow — never crash the service on a bad frame
        } finally {
            bmp.recycle()
        }
    }

    override fun onDestroy() {
        try { if (::grabber.isInitialized) grabber.stop() } catch (_: Throwable) {}
        try { if (::projection.isInitialized) projection.stop() } catch (_: Throwable) {}
        try { if (::overlay.isInitialized) overlay.hide() } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cinder Coach")
            .setContentText("Reading the arena")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }
}
