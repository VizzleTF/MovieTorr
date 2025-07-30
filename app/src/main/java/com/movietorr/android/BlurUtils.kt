package com.movietorr.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View

object BlurUtils {
    
    /**
     * Применяет блюр к view и возвращает размытый bitmap
     */
    fun blurView(context: Context, view: View, radius: Float = 8f): Bitmap? {
        return try {
            // Создаем bitmap из view
            val bitmap = Bitmap.createBitmap(
                view.width, 
                view.height, 
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            
            // Применяем блюр
            blurBitmap(context, bitmap, radius)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Применяет блюр к bitmap используя RenderScript
     */
    fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float = 8f): Bitmap? {
        return try {
            val rs = RenderScript.create(context)
            val input = Allocation.createFromBitmap(rs, bitmap)
            val output = Allocation.createTyped(rs, input.type)
            
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            script.setRadius(radius)
            script.setInput(input)
            script.forEach(output)
            
            output.copyTo(bitmap)
            rs.destroy()
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
    
    /**
     * Создает фон с эффектом жидкого стекла
     */
    fun createLiquidGlassBackground(
        context: Context,
        cornerRadius: Float = 28f,
        blurRadius: Float = 8f,
        alpha: Float = 0.3f
    ): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = Paint().apply {
                isAntiAlias = true
                this.alpha = (255 * alpha).toInt()
            }
            
            private val rect = RectF()
            
            override fun draw(canvas: android.graphics.Canvas) {
                rect.set(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat())
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            }
            
            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }
            
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                paint.colorFilter = colorFilter
            }
            
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }
} 