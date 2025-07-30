package com.movietorr.android

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat

class LiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val path = Path()
    private val rect = RectF()
    
    var cornerRadius: Float = 28f
        set(value) {
            field = value
            invalidate()
        }
    
    var glassAlpha: Float = 0.3f
        set(value) {
            field = value
            invalidate()
        }
    
    var glassColor: Int = ContextCompat.getColor(context, android.R.color.white)
        set(value) {
            field = value
            invalidate()
        }
    
    init {
        // Читаем кастомные атрибуты
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.LiquidGlassView)
        try {
            cornerRadius = typedArray.getDimension(R.styleable.LiquidGlassView_cornerRadius, 28f)
            glassAlpha = typedArray.getFloat(R.styleable.LiquidGlassView_glassAlpha, 0.3f)
            glassColor = typedArray.getColor(R.styleable.LiquidGlassView_glassColor, ContextCompat.getColor(context, android.R.color.white))
        } finally {
            typedArray.recycle()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Создаем скругленный прямоугольник
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        path.reset()
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        
        // Применяем эффект жидкого стекла
        paint.color = glassColor
        paint.alpha = (255 * glassAlpha).toInt()
        
        // Рисуем основной фон
        canvas.drawPath(path, paint)
        
        // Добавляем inner refraction эффект
        drawInnerRefraction(canvas)
        
        // Добавляем shine эффект
        drawShineEffect(canvas)
    }
    
    private fun drawInnerRefraction(canvas: Canvas) {
        val refractionPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            alpha = (255 * 0.1f).toInt()
        }
        
        // Создаем градиент для inner refraction
        val gradient = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                ContextCompat.getColor(context, android.R.color.transparent),
                ContextCompat.getColor(context, android.R.color.white),
                ContextCompat.getColor(context, android.R.color.transparent)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        
        refractionPaint.shader = gradient
        canvas.drawPath(path, refractionPaint)
    }
    
    private fun drawShineEffect(canvas: Canvas) {
        val shinePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            alpha = (255 * 0.05f).toInt()
        }
        
        // Создаем радиальный градиент для shine
        val centerX = width * 0.3f
        val centerY = height * 0.3f
        val radius = kotlin.math.min(width, height) * 0.8f
        
        val shineGradient = android.graphics.RadialGradient(
            centerX, centerY, radius,
            intArrayOf(
                ContextCompat.getColor(context, android.R.color.white),
                ContextCompat.getColor(context, android.R.color.transparent)
            ),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        
        shinePaint.shader = shineGradient
        canvas.drawPath(path, shinePaint)
    }
} 