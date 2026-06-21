package com.hairstyle.generator.ui.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlin.math.abs

/**
 * 图片前后对比View
 * 实现类似ComfyUI的Image Comparer效果
 */
class ImageComparerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 图片Bitmap
    private var beforeBitmap: Bitmap? = null
    private var afterBitmap: Bitmap? = null

    // 图片显示矩阵
    private val imageMatrix = Matrix()

    // 分割线位置 (0.0 - 1.0)
    private var dividerPosition: Float = 0.5f

    // 触摸状态
    private var isDragging = false

    // 密度
    private val density: Float = context.resources.displayMetrics.density

    // 尺寸（使用 lazy 确保正确初始化）
    private val dividerWidthPx: Float by lazy { 4f * density }
    private val dividerBorderWidthPx: Float by lazy { 8f * density }
    private val handleRadiusPx: Float by lazy { 22f * density }
    private val handleStrokeWidthPx: Float by lazy { 3f * density }
    private val arrowSizePx: Float by lazy { 10f * density }
    private val arrowOffsetPx: Float by lazy { 7f * density }

    // 画笔（使用 lazy 确保正确初始化）
    private val dividerBorderPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AA000000")
            strokeWidth = dividerBorderWidthPx
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    private val dividerPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = dividerWidthPx
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    private val handleFillPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DD000000")
            style = Paint.Style.FILL
        }
    }

    private val handleStrokePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = handleStrokeWidthPx
            style = Paint.Style.STROKE
        }
    }

    private val arrowPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 3f * density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    init {
        // 关闭硬件加速以确保绑定正常工作
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        // 设置黑色背景
        setBackgroundColor(Color.BLACK)
    }

    /**
     * 加载两张对比图片
     */
    fun loadImages(context: Context, beforeUrl: String, afterUrl: String) {
        Log.d("ImageComparer", "loadImages called - before: '$beforeUrl', after: '$afterUrl'")

        // 检查 URL 是否有效
        if (beforeUrl.isBlank()) {
            Log.w("ImageComparer", "beforeUrl is empty! Only showing after image")
        }
        if (afterUrl.isBlank()) {
            Log.w("ImageComparer", "afterUrl is empty!")
        }

        dividerPosition = 0.5f
        var loadedCount = 0
        val targetCount = if (beforeUrl.isBlank()) 1 else 2

        val checkAndUpdate = {
            loadedCount++
            Log.d("ImageComparer", "Image loaded, count: $loadedCount / $targetCount")
            if (loadedCount >= targetCount) {
                post {
                    calculateImageMatrix()
                    invalidate()
                }
            }
        }

        // 加载原图（如果 URL 有效）
        if (beforeUrl.isNotBlank()) {
            Glide.with(context)
                .asBitmap()
                .load(beforeUrl.replace("http://", "https://"))
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        Log.d("ImageComparer", "Before image loaded: ${resource.width}x${resource.height}")
                        beforeBitmap = resource
                        checkAndUpdate()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        beforeBitmap = null
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Log.e("ImageComparer", "Before image load FAILED for URL: $beforeUrl")
                        checkAndUpdate()
                    }
                })
        } else {
            // beforeUrl 为空，清除 beforeBitmap
            beforeBitmap = null
        }

        // 加载结果图
        Glide.with(context)
            .asBitmap()
            .load(afterUrl.replace("http://", "https://"))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d("ImageComparer", "After image loaded: ${resource.width}x${resource.height}")
                    afterBitmap = resource
                    checkAndUpdate()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    afterBitmap = null
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e("ImageComparer", "After image load FAILED for URL: $afterUrl")
                    checkAndUpdate()
                }
            })
    }

    private fun calculateImageMatrix() {
        val bitmap = afterBitmap ?: beforeBitmap ?: return
        if (width == 0 || height == 0) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val scale = maxOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
        val scaledWidth = bitmapWidth * scale
        val scaledHeight = bitmapHeight * scale
        val dx = (viewWidth - scaledWidth) / 2f
        val dy = (viewHeight - scaledHeight) / 2f

        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(dx, dy)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateImageMatrix()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val dividerX = w * dividerPosition

        // 绘制右侧图片（结果图）
        afterBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, imageMatrix, null)
        }

        // 绘制左侧图片（原图），裁剪到分割线左侧
        beforeBitmap?.let { bitmap ->
            canvas.save()
            canvas.clipRect(0f, 0f, dividerX, h)
            canvas.drawBitmap(bitmap, imageMatrix, null)
            canvas.restore()
        }

        // 绘制分割线（先画深色边框，再画白色线）
        canvas.drawLine(dividerX, 0f, dividerX, h, dividerBorderPaint)
        canvas.drawLine(dividerX, 0f, dividerX, h, dividerPaint)

        // 绘制滑块手柄
        val centerY = h / 2f

        // 圆形背景
        canvas.drawCircle(dividerX, centerY, handleRadiusPx, handleFillPaint)
        // 圆形边框
        canvas.drawCircle(dividerX, centerY, handleRadiusPx, handleStrokePaint)

        // 左箭头 <
        val leftX = dividerX - arrowOffsetPx
        canvas.drawLine(leftX, centerY, leftX - arrowSizePx, centerY - arrowSizePx, arrowPaint)
        canvas.drawLine(leftX, centerY, leftX - arrowSizePx, centerY + arrowSizePx, arrowPaint)

        // 右箭头 >
        val rightX = dividerX + arrowOffsetPx
        canvas.drawLine(rightX, centerY, rightX + arrowSizePx, centerY - arrowSizePx, arrowPaint)
        canvas.drawLine(rightX, centerY, rightX + arrowSizePx, centerY + arrowSizePx, arrowPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val dividerX = width * dividerPosition
                val touchRadius = handleRadiusPx + 40f * density
                if (abs(event.x - dividerX) <= touchRadius) {
                    isDragging = true
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dividerPosition = (event.x / width).coerceIn(0.05f, 0.95f)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    parent.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun clear() {
        beforeBitmap = null
        afterBitmap = null
        invalidate()
    }
}
