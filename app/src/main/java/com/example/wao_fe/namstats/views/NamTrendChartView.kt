//nam them
package com.example.wao_fe.namstats.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.max

class NamTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4F46E5")
        //nam them
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        style = Paint.Style.FILL
    }

    private val selectedPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4F46E5")
        style = Paint.Style.FILL
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 18f
        textAlign = Paint.Align.LEFT
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 16f
        textAlign = Paint.Align.LEFT
    }

    private var labels: List<String> = emptyList()
    private var values: List<Float> = emptyList()
    private var pointPositions: List<Pair<Float, Float>> = emptyList()
    private var selectedIndex: Int = -1
    private var pointClickListener: ((Int) -> Unit)? = null
    private var yAxisUnit: String = ""
    private var xAxisUnit: String = ""

    fun submitData(newValues: List<Float>, chartLabels: List<String>) {
        values = newValues
        labels = chartLabels
        selectedIndex = if (newValues.isNotEmpty()) 0 else -1
        invalidate()
    }

    fun setAxisUnits(yUnit: String, xUnit: String) {
        yAxisUnit = yUnit
        xAxisUnit = xUnit
        invalidate()
    }

    fun setOnPointSelectedListener(listener: (Int) -> Unit) {
        pointClickListener = listener
    }

    fun selectIndex(index: Int) {
        if (index in values.indices) {
            selectedIndex = index
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (values.isEmpty()) {
            canvas.drawText("Chua co du lieu", width / 2f, height / 2f, emptyPaint)
            return
        }

        val left = 64f
        val right = width - 48f
        val top = 40f
        val bottom = height - 72f
        val chartHeight = bottom - top
        val chartWidth = right - left

        val rawMaxValue = values.maxOrNull() ?: 0f
        val rawMinValue = values.minOrNull() ?: 0f
        val (axisMin, axisMax) = calculateAxisBounds(rawMinValue, rawMaxValue)
        val axisRange = max(axisMax - axisMin, 1f)
        val axisStep = axisRange / 4f
        val stepX = if (values.size > 1) chartWidth / (values.size - 1) else 0f
        val path = Path()
        val positions = mutableListOf<Pair<Float, Float>>()

        repeat(5) { index ->
            val y = top + chartHeight * index / 4f
            val axisValue = axisMax - axisStep * index
            canvas.drawLine(left, y, right, y, gridPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                formatAxisValue(axisValue),
                left - 6f,
                y + 10f,
                textPaint
            )
        }
        textPaint.textAlign = Paint.Align.LEFT

        drawAxisUnitHints(canvas, left, right, top, bottom)

        values.forEachIndexed { index, value ->
            val x = if (values.size == 1) left + chartWidth / 2f else left + index * stepX
            val ratio = ((value - axisMin) / axisRange).coerceIn(0f, 1f)
            val y = bottom - ratio * chartHeight
            positions.add(x to y)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        pointPositions = positions
        canvas.drawPath(path, linePaint)

        positions.forEachIndexed { index, (x, y) ->
            val paint = if (index == selectedIndex) selectedPointPaint else pointPaint
            //nam them
            val radius = if (index == selectedIndex) 7f else 4.5f
            canvas.drawCircle(x, y, radius, paint)
        }

        drawXAxisLabels(canvas, left, right, bottom)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && pointPositions.isNotEmpty()) {
            val tappedIndex = pointPositions.indices.minByOrNull { index ->
                val point = pointPositions[index]
                abs(point.first - event.x) + abs(point.second - event.y) * 0.3f
            } ?: -1

            if (tappedIndex >= 0) {
                selectedIndex = tappedIndex
                invalidate()
                pointClickListener?.invoke(tappedIndex)
                return true
            }
        }
        return true
    }

    private fun drawXAxisLabels(canvas: Canvas, left: Float, right: Float, bottom: Float) {
        if (labels.isEmpty()) return

        val labelY = bottom + 28f

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(labels.first(), left, labelY, textPaint)

        if (labels.size > 1) {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(labels.last(), right, labelY, textPaint)
        }

        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawAxisUnitHints(canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float) {
        if (yAxisUnit.isNotBlank()) {
            unitPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(yAxisUnit, left - 6f, top - 12f, unitPaint)
            unitPaint.textAlign = Paint.Align.LEFT
        }

        if (xAxisUnit.isNotBlank()) {
            unitPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(xAxisUnit, right, bottom + 56f, unitPaint)
            unitPaint.textAlign = Paint.Align.LEFT
        }
    }

    private fun calculateAxisBounds(minValue: Float, maxValue: Float): Pair<Float, Float> {
        if (minValue == maxValue) {
            //nam them
            val padding = if (minValue == 0f) 0.5f else max(abs(minValue) * 0.03f, 0.1f)
            return minValue - padding to maxValue + padding
        }

        val range = maxValue - minValue
        //nam them
        val padding = max(range * 0.05f, 0.02f)
        return minValue - padding to maxValue + padding
    }

    private fun formatAxisValue(value: Float): String {
        return BigDecimal(value.toDouble())
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
    }
}
