// language: Kotlin, file: AdvisorOverlay.kt
// floating draggable HUD bubble
package com.cinder.coach.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.cinder.coach.logic.Suggestion

class AdvisorOverlay(private val ctx: Context) {

    private var wm: WindowManager? = null
    private var view: View? = null
    private lateinit var cardLine: TextView
    private lateinit var sideLine: TextView
    private lateinit var reasonLine: TextView

    private var everUpdated = false
    fun isFirst(): Boolean = !everUpdated

    fun show() {
        wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 20, 28, 20)
            background = GradientDrawable().apply {
                setColor(0xCC101018.toInt())
                cornerRadius = 24f
                setStroke(3, 0xFFE645E6.toInt())
            }
        }

        cardLine = TextView(ctx).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            text = "waiting for match…"
        }
        sideLine = TextView(ctx).apply {
            setTextColor(0xFFE645E6.toInt())
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }
        reasonLine = TextView(ctx).apply {
            setTextColor(0xFFBBBBBB.toInt())
            textSize = 12f
        }
        root.addView(cardLine); root.addView(sideLine); root.addView(reasonLine)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40; y = 400
        }

        root.setOnTouchListener(object : View.OnTouchListener {
            private var initX = 0; private var initY = 0
            private var touchX = 0f; private var touchY = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initX = params.x; initY = params.y
                        touchX = e.rawX; touchY = e.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initX + (e.rawX - touchX).toInt()
                        params.y = initY + (e.rawY - touchY).toInt()
                        wm?.updateViewLayout(root, params)
                    }
                }
                return true
            }
        })

        wm?.addView(root, params)
        view = root
    }

    fun update(s: Suggestion) {
        everUpdated = true
        view?.post {
            if (s.card.isEmpty()) {
                cardLine.text = "·"
                sideLine.text = "WAIT"
                reasonLine.text = s.reason
            } else {
                cardLine.text = "▶ ${s.card.replace('_', ' ').uppercase()}"
                sideLine.text = when (s.side) {
                    "LEFT"   -> "← LEFT BRIDGE"
                    "RIGHT"  -> "→ RIGHT BRIDGE"
                    "CENTER" -> "⊙ CENTER"
                    else     -> s.side
                }
                reasonLine.text = s.reason
            }
        }
    }

    fun hide() {
        view?.let { runCatching { wm?.removeView(it) } }
        view = null
    }
}
