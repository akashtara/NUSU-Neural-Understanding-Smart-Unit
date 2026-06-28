package com.example.services

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.abs

class FloatingWindowHelper(private val context: Context, private val onClick: () -> Unit) {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    fun show() {
        if (!Settings.canDrawOverlays(context)) {
            return
        }

        if (floatingView != null) return // Already showing

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val size = (60 * context.resources.displayMetrics.density).toInt()
        
        val container = FrameLayout(context)
        val imageView = ImageView(context)
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#121212")) // Dark background
            setStroke(2, Color.parseColor("#4285F4")) // Glow border
        }
        imageView.background = bgDrawable
        imageView.setImageResource(android.R.drawable.ic_btn_speak_now) // Microphone icon
        imageView.setPadding(15, 15, 15, 15)
        
        container.addView(imageView, FrameLayout.LayoutParams(size, size))
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 20
        layoutParams.y = 200

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false
        var defaultTouchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        var currentTouchSlop = defaultTouchSlop

        container.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val prefs = context.getSharedPreferences("NUSU_PRO_PREFS", Context.MODE_PRIVATE)
                    val customSlop = prefs.getFloat("dev_touch_sensitivity", -1f)
                    currentTouchSlop = if (customSlop >= 0f) customSlop else defaultTouchSlop
                    
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val diffX = event.rawX - initialTouchX
                    val diffY = event.rawY - initialTouchY
                    layoutParams.x = initialX + diffX.toInt()
                    layoutParams.y = initialY + diffY.toInt()
                    windowManager?.updateViewLayout(container, layoutParams)
                    if (abs(diffX) > currentTouchSlop || abs(diffY) > currentTouchSlop) {
                        moved = true
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!moved) {
                        view.performClick()
                        onClick()
                    }
                    true
                }
                else -> false
            }
        }
        
        floatingView = container
        windowManager?.addView(floatingView, layoutParams)
    }

    fun remove() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }
}
