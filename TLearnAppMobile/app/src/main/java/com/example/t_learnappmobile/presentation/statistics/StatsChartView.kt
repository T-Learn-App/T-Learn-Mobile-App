package com.example.t_learnappmobile.presentation.statistics

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.t_learnappmobile.data.statistics.DailyStats
import androidx.core.graphics.toColorInt



class StatsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var statsList: List<DailyStats> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()
    @SuppressLint("SimpleDateFormat")
    private val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

    @SuppressLint("SimpleDateFormat")
    private val outFmt = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault())

    fun setStats(stats: List<DailyStats>) {
        statsList = stats

        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val height = if (heightMode == MeasureSpec.UNSPECIFIED) {
            200
        } else {
            MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (statsList.isEmpty()) {
            Log.d("StatsChartView", "No stats to draw")
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val paddingTop = paddingTop.toFloat()
        val paddingBottom = paddingBottom.toFloat()
        val contentWidth = w - paddingLeft - paddingRight
        val contentHeight = h - paddingTop - paddingBottom - 70f

        val maxValue = statsList.maxOfOrNull {
            maxOf(it.newWords, it.inProgressWords, it.learnedWords)
        }?.toFloat() ?: 0f



        if (maxValue <= 0f) {

            textPaint.color = Color.LTGRAY
            textPaint.textSize = 28f
            canvas.drawText("Нет данных за неделю", w / 2f, h / 2f, textPaint)
            return
        }

        val groupWidth = contentWidth / statsList.size
        val barWidth = (groupWidth * 0.25f).coerceAtLeast(8f)
        val centerYOffset = h - paddingBottom - 40f

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 22f
        textPaint.color = Color.GRAY

        statsList.forEachIndexed { index, s ->
            val centerX = paddingLeft + groupWidth * index + groupWidth / 2f

            fun drawBar(value: Int, offsetMultiplier: Float, color: Int) {
                if (value <= 0 || maxValue <= 0f) return
                val barHeight = (value / maxValue) * contentHeight
                val left = centerX + offsetMultiplier * barWidth - barWidth / 2f
                val right = left + barWidth
                val top = centerYOffset - barHeight
                rect.set(left, top, right, centerYOffset)
                barPaint.color = color
                canvas.drawRoundRect(rect, 6f, 6f, barPaint)
            }

            drawBar(s.inProgressWords, 0f, "#FF9800".toColorInt())
            drawBar(s.learnedWords, 1f, "#4CAF50".toColorInt())

            val label = try {
                outFmt.format(inFmt.parse(s.date)!!)
            } catch (_: Exception) {
                s.date.takeLast(5)
            }
            canvas.drawText(label, centerX, centerYOffset + 25f, textPaint)
        }
    }
}
