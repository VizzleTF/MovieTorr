package com.movietorr.android

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver

class LiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetView: View? = null
    private var targetBitmap: Bitmap? = null
    private var runtimeShader: RuntimeShader? = null
    private var shaderPaint: Paint? = null
    
    // Параметры эффекта
    private var blurRadius: Float = 8f
    private var cornerRadius: Float = 28f
    private var curvature: Float = 0.1f
    private var thickness: Float = 0.05f
    
    init {
        setupShader()
    }
    
    private fun setupShader() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            try {
                runtimeShader = RuntimeShader(SHADER_CODE)
                shaderPaint = Paint().apply {
                    shader = runtimeShader
                    isAntiAlias = true
                }
            } catch (e: Exception) {
                // Fallback для устройств без поддержки AGSL
                shaderPaint = Paint().apply {
                    color = Color.WHITE
                    alpha = 200
                    isAntiAlias = true
                }
            }
        } else {
            // Fallback для старых версий Android
            shaderPaint = Paint().apply {
                color = Color.WHITE
                alpha = 200
                isAntiAlias = true
            }
        }
    }
    
    fun setTargetView(view: View) {
        targetView?.viewTreeObserver?.removeOnPreDrawListener(targetLayoutListener)
        targetView = view
        view.viewTreeObserver.addOnPreDrawListener(targetLayoutListener)
    }
    
    private val targetLayoutListener = ViewTreeObserver.OnPreDrawListener {
        updateBitmap()
        true
    }
    
    private fun updateBitmap() {
        val view = targetView ?: return
        val shader = runtimeShader ?: return
        
        try {
            // Создаем bitmap из View правильным способом
            val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            view.draw(canvas)
            targetBitmap = bmp
            
            val bitmapShader = BitmapShader(
                bmp,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
            
            val targetPos = IntArray(2)
            val selfPos = IntArray(2)
            
            view.getLocationOnScreen(targetPos)
            getLocationOnScreen(selfPos)
            
            shader.setInputShader("iImage1", bitmapShader)
            shader.setFloatUniform("iImageResolution", bmp.width.toFloat(), bmp.height.toFloat())
            shader.setFloatUniform("iTargetViewPos", targetPos[0].toFloat(), targetPos[1].toFloat())
            shader.setFloatUniform("iShaderViewPos", selfPos[0].toFloat(), selfPos[1].toFloat())
            shader.setFloatUniform("iShaderResolution", width.toFloat(), height.toFloat())
            shader.setFloatUniform("iBlurRadius", blurRadius)
            shader.setFloatUniform("iCornerRadius", cornerRadius)
            shader.setFloatUniform("iCurvature", curvature)
            shader.setFloatUniform("iThickness", thickness)
            
            shaderPaint?.shader = shader
            invalidate()
        } catch (e: Exception) {
            // Fallback если что-то пошло не так
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (shaderPaint != null) {
            // Рисуем скругленные углы
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shaderPaint!!)
        }
    }
    
    companion object {
        private val SHADER_CODE = """
            uniform shader iImage1;
            uniform float2 iImageResolution;
            uniform float2 iTargetViewPos;
            uniform float2 iShaderViewPos;
            uniform float2 iShaderResolution;
            uniform float iBlurRadius;
            uniform float iCornerRadius;
            uniform float iCurvature;
            uniform float iThickness;
            
            float2 getUV(float2 coord) {
                float2 globalCoord = coord + iShaderViewPos - iTargetViewPos;
                return globalCoord / iImageResolution;
            }
            
            float2 applyLensDistortion(float2 coord, float2 center, float2 size, float radius, float curvature, float thickness) {
                float2 normalized = (coord - center) / size;
                float dist = length(normalized);
                
                if (dist > 1.0) {
                    return coord;
                }
                
                // Эффект линзы
                float distortion = 1.0 + curvature * (1.0 - dist * dist);
                float2 distorted = normalized * distortion;
                
                // Добавляем толщину стекла
                distorted *= (1.0 - thickness);
                
                return center + distorted * size;
            }
            
            half4 gaussianBlur(float2 uv, float2 resolution, float radius) {
                if (radius <= 0.0) {
                    return iImage1.eval(uv * resolution);
                }
                
                float2 texelSize = 1.0 / resolution;
                half4 color = half4(0.0);
                float totalWeight = 0.0;
                
                // Простой Gaussian blur 5x5
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        float2 offset = float2(x, y) * texelSize * radius;
                        float weight = exp(-(x*x + y*y) / (2.0 * radius * radius));
                        color += iImage1.eval((uv + offset) * resolution) * weight;
                        totalWeight += weight;
                    }
                }
                
                return color / totalWeight;
            }
            
            half4 main(float2 fragCoord) {
                float2 center = iShaderResolution * 0.5;
                float2 lensSize = iShaderResolution * 0.48;
                
                float2 distortedCoord = applyLensDistortion(
                    fragCoord, center, lensSize, iCornerRadius, iCurvature, iThickness
                );
                
                float2 uv = getUV(distortedCoord);
                return gaussianBlur(uv, iImageResolution, iBlurRadius);
            }
        """.trimIndent()
    }
} 