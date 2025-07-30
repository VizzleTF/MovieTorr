package com.movietorr.android

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlin.math.min

class BottomSheetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "BottomSheetView"
        private const val DEBUG = true
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val shadowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLACK
        alpha = (255 * 0.3f).toInt()
    }
    
    private val path = Path()
    private val rect = RectF()
    private val shadowRect = RectF()
    
    var cornerRadius: Float = 28f
        set(value) {
            field = value
            invalidate()
        }
    
    var glassAlpha: Float = 0.8f
        set(value) {
            field = value
            invalidate()
        }
    
    var glassColor: Int = ContextCompat.getColor(context, android.R.color.white)
        set(value) {
            field = value
            invalidate()
        }
    
    private var dragHandleHeight: Float = 48f
    private var isDragging = false
    private var lastY = 0f
    
    init {
        if (DEBUG) {
            Log.d(TAG, "BottomSheetView init")
        }
        
        try {
            // Читаем кастомные атрибуты (используем те же что и LiquidGlassView)
            val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.LiquidGlassView)
            try {
                cornerRadius = typedArray.getDimension(R.styleable.LiquidGlassView_cornerRadius, 28f)
                glassAlpha = typedArray.getFloat(R.styleable.LiquidGlassView_glassAlpha, 0.8f)
                glassColor = typedArray.getColor(R.styleable.LiquidGlassView_glassColor, ContextCompat.getColor(context, android.R.color.white))
                
                if (DEBUG) {
                    Log.d(TAG, "Attributes loaded - cornerRadius: $cornerRadius, glassAlpha: $glassAlpha")
                }
            } finally {
                typedArray.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading attributes", e)
        }
        
        // Устанавливаем прозрачный фон
        setBackgroundColor(Color.TRANSPARENT)
        
        // Включаем аппаратное ускорение
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaY = event.y - lastY
                    // Здесь можно добавить логику drag эффекта
                    lastY = event.y
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    override fun dispatchDraw(canvas: Canvas) {
        try {
            if (DEBUG) {
                Log.d(TAG, "dispatchDraw called - width: $width, height: $height")
            }
            
            // Рисуем тень
            drawShadow(canvas)
            
            // Создаем скругленный прямоугольник
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            path.reset()
            path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            
            // Рисуем основной фон с эффектом стекла
            paint.color = glassColor
            paint.alpha = (255 * glassAlpha).toInt()
            canvas.drawPath(path, paint)
            
            // Добавляем эффекты жидкого стекла
            drawGlassEffects(canvas)
            
            // Рисуем drag handle
            drawDragHandle(canvas)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in dispatchDraw", e)
            // Fallback
            try {
                canvas.drawColor(glassColor)
            } catch (e2: Exception) {
                Log.e(TAG, "Error in fallback draw", e2)
            }
        }
        
        // Рисуем дочерние элементы
        super.dispatchDraw(canvas)
    }
    
    private fun drawShadow(canvas: Canvas) {
        try {
            // Создаем тень с размытием
            shadowRect.set(
                -8f, -8f, 
                width.toFloat() + 8f, 
                height.toFloat() + 8f
            )
            
            val shadowPath = Path()
            shadowPath.addRoundRect(shadowRect, cornerRadius + 8f, cornerRadius + 8f, Path.Direction.CW)
            
            // Рисуем тень с градиентом
            canvas.drawPath(shadowPath, shadowPaint)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in drawShadow", e)
        }
    }
    
    private fun drawGlassEffects(canvas: Canvas) {
        try {
            // Inner refraction эффект
            val refractionPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                alpha = (255 * 0.15f).toInt()
            }
            
            val gradient = android.graphics.LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.WHITE,
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            
            refractionPaint.shader = gradient
            canvas.drawPath(path, refractionPaint)
            
            // Shine эффект
            val shinePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                alpha = (255 * 0.1f).toInt()
            }
            
            val centerX = width * 0.3f
            val centerY = height * 0.3f
            val radius = min(width, height) * 0.8f
            
            val shineGradient = android.graphics.RadialGradient(
                centerX, centerY, radius,
                intArrayOf(Color.WHITE, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            
            shinePaint.shader = shineGradient
            canvas.drawPath(path, shinePaint)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in drawGlassEffects", e)
        }
    }
    
    private fun drawDragHandle(canvas: Canvas) {
        try {
            val handlePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.GRAY
                alpha = (255 * 0.5f).toInt()
            }
            
            val handleWidth = 48f
            val handleHeight = 6f
            val handleX = (width - handleWidth) / 2f
            val handleY = 12f
            
            val handleRect = RectF(handleX, handleY, handleX + handleWidth, handleY + handleHeight)
            canvas.drawRoundRect(handleRect, 3f, 3f, handlePaint)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in drawDragHandle", e)
        }
    }
} 