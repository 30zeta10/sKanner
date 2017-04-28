package it.facile.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import it.facile.skanner.R

class AdjustView : FrameLayout {

    private val TAG = this::class.java.simpleName

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var initialized = false
    private var measured = false

    private lateinit var scannedDocument: Scan
    private lateinit var imageBitmap: Bitmap
    private lateinit var imageView: ImageView
    private lateinit var pointerView1: ImageView
    private lateinit var pointerView2: ImageView
    private lateinit var pointerView3: ImageView
    private lateinit var pointerView4: ImageView

    private val cornerIndicatorRadiusPx: Int by lazy { resources.getDimension(R.dimen.pointer_radius).toInt() / 2 }

    private val paint: Paint by lazy {
        Paint().apply {
            strokeWidth = 2f
            isAntiAlias = true
            color = if (Build.VERSION.SDK_INT >= 23)
                resources.getColor(R.color.adjustViewColor, context.theme)
            else
                resources.getColor(R.color.adjustViewColor)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(TAG, "onMeasure. initialized=$initialized")
        if (isInEditMode) return
        if (measured == false && initialized == true) {
            invalidate()
            positionPointers()
            measured = true
        }
    }


    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        Log.d(TAG, "dispatchDraw. initialized=$initialized")
        if (initialized == false) return
        canvas?.drawLine(from = pointerView1.getPosition(), to = pointerView2.getPosition(), paint = paint)
        canvas?.drawLine(from = pointerView2.getPosition(), to = pointerView3.getPosition(), paint = paint)
        canvas?.drawLine(from = pointerView3.getPosition(), to = pointerView4.getPosition(), paint = paint)
        canvas?.drawLine(from = pointerView4.getPosition(), to = pointerView1.getPosition(), paint = paint)
    }

    /**
     * Initialize the view with a Scan. It shows the
     */
    fun init(scannedDoc: Scan) {
        Log.d(TAG, "init. initialized=$initialized")
        if (initialized == false) {
            imageBitmap = loadBitmap(scannedDoc.scannedImageURI)!!
            imageView = buildImageView()
            scannedDocument = scannedDoc
            addView(imageView)
            initPointers()
            invalidate()
            initialized = true
        }
    }

    fun setNewRectangle(newRectangle: Rectangle) {
        Log.d(TAG, "setNewRectangle. initialized=$initialized")
        scannedDocument = scannedDocument.copy(detectedRectangle = newRectangle)
        positionPointers()
        invalidate()
    }

    /**
     * Return the Rectangle shown by the view with the new coordinates,
     * null if the view was not initialized.
     */
    fun getNewRectangle(): Rectangle? {
        if (initialized == false) return null

        return scannedDocument.detectedRectangle.copy(
                p1 = pointerView1.getPosition().scale(1 / calculateScaleFactor(imageBitmap, imageView)),
                p2 = pointerView2.getPosition().scale(1 / calculateScaleFactor(imageBitmap, imageView)),
                p3 = pointerView3.getPosition().scale(1 / calculateScaleFactor(imageBitmap, imageView)),
                p4 = pointerView4.getPosition().scale(1 / calculateScaleFactor(imageBitmap, imageView)))
    }


    private fun calculateScaleFactor(bitmap: Bitmap, imageView: ImageView): Float =
            minOf(imageView.measuredWidth.toFloat() / bitmap.width,
                    imageView.measuredHeight.toFloat() / bitmap.height)

    private fun buildPointerView(): ImageView = ImageView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setImageResource(R.drawable.pointer)
        setOnTouchListener(TouchListener())
    }

    private fun initPointers() {
        pointerView1 = buildPointerView()
        pointerView2 = buildPointerView()
        pointerView3 = buildPointerView()
        pointerView4 = buildPointerView()
        addView(pointerView1)
        addView(pointerView2)
        addView(pointerView3)
        addView(pointerView4)
    }

    private fun positionPointers() {
        val scaledRectangle = scannedDocument.detectedRectangle.scale(calculateScaleFactor(imageBitmap, imageView))
        pointerView1.setPosition(scaledRectangle.p1.first.toFloat() to scaledRectangle.p1.second.toFloat() )
        pointerView2.setPosition(scaledRectangle.p2.first.toFloat() to scaledRectangle.p2.second.toFloat() )
        pointerView3.setPosition(scaledRectangle.p3.first.toFloat() to scaledRectangle.p3.second.toFloat() )
        pointerView4.setPosition(scaledRectangle.p4.first.toFloat() to scaledRectangle.p4.second.toFloat() )
    }

    private fun buildImageView() = ImageView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        adjustViewBounds = true
        setImageBitmap(imageBitmap)
    }


    private fun ImageView.positionPointerView(pt: Pt) {
        val newX = if (pt.first + cornerIndicatorRadiusPx > this.measuredWidth) this.measuredWidth.toFloat()
        else if (pt.first + cornerIndicatorRadiusPx < 0) 0f
        else pt.first.toFloat()
        val newY = if (pt.second + cornerIndicatorRadiusPx > this.measuredHeight) this.measuredHeight.toFloat()
        else if (pt.second + cornerIndicatorRadiusPx < 0) 0f
        else pt.second.toFloat()
        setPosition(newX to newY)
    }

    private fun ImageView.getPosition(): Pt = Math.round(x) + cornerIndicatorRadiusPx to Math.round(y) + cornerIndicatorRadiusPx

    private fun ImageView.setPosition(newPosition: Pair<Float, Float>) {
        x = (newPosition.first) - cornerIndicatorRadiusPx
        y = (newPosition.second) - cornerIndicatorRadiusPx
    }

    private fun Canvas.drawLine(from: Pt, to: Pt, paint: Paint) =
            drawLine(from.first.toFloat(), from.second.toFloat(), to.first.toFloat(), to.second.toFloat(), paint)

    private inner class TouchListener : OnTouchListener {

        private var downPt = PointF()
        private var startPt = PointF()

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPt.x, event.y - downPt.y)
                    val (newX, newY) = imageView.positionInside(startPt.x + mv.x, startPt.y + mv.y)

                    v.x = newX
                    v.y = newY
                    startPt = PointF(v.x, v.y)
                }
                MotionEvent.ACTION_DOWN -> {
                    downPt.x = event.x
                    downPt.y = event.y
                    startPt = PointF(v.x, v.y)
                }
            }
            invalidate()
            return true
        }
    }

    private fun ImageView.positionInside(x: Float, y: Float): Pair<Float, Float> {
        val newX = if (x + cornerIndicatorRadiusPx > measuredWidth) measuredWidth.toFloat() - cornerIndicatorRadiusPx
        else if (x + cornerIndicatorRadiusPx < 0) 0f - cornerIndicatorRadiusPx
        else x

        val newY = if (y + cornerIndicatorRadiusPx > measuredHeight) measuredHeight.toFloat() - cornerIndicatorRadiusPx
        else if (y + cornerIndicatorRadiusPx < 0) 0f - cornerIndicatorRadiusPx
        else y

        return newX to newY
    }
}