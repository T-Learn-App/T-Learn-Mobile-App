package com.example.t_learnappmobile.presentation.statistics

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.toColorInt

class StatsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class ChartDayStats(
        val date: String,
        val inProgress: Int = 0,
        val learned: Int = 0
    )

    private var statsList: List<ChartDayStats> = emptyList()

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

    fun setStats(stats: List<StatisticsViewModel.DailyStats>) {
        statsList = stats.map { day ->
            ChartDayStats(
                date = day.date,
                inProgress = day.inProgressWords,
                learned = day.learnedWords
            )
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val height = if (heightMode == MeasureSpec.UNSPECIFIED) {
            250
        } else {
            MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (statsList.isEmpty()) {
            textPaint.color = Color.LTGRAY
            textPaint.textSize = 28f
            canvas.drawText("Нет данных за неделю", width / 2f, height / 2f, textPaint)
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val paddingTop = paddingTop.toFloat()
        val paddingBottom = paddingBottom.toFloat()
        val contentWidth = w - paddingLeft - paddingRight
        val contentHeight = h - paddingTop - paddingBottom - 100f

        val maxValue = statsList.maxOfOrNull { day ->
            maxOf(day.inProgress, day.learned)
        }?.toFloat() ?: 0f

        if (maxValue <= 0f) {
            textPaint.color = Color.LTGRAY
            textPaint.textSize = 28f
            canvas.drawText("Нет данных за неделю", w / 2f, h / 2f, textPaint)
            return
        }

        val groupWidth = contentWidth / statsList.size
        val barWidth = (groupWidth * 0.2f).coerceAtLeast(8f)
        val spacing = barWidth * 0.3f
        val centerYOffset = h - paddingBottom - 70f

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 22f
        textPaint.color = Color.GRAY

        statsList.forEachIndexed { index, s ->
            val centerX = paddingLeft + groupWidth * index + groupWidth / 2f

            drawBar(canvas, s.inProgress, -barWidth - spacing, "#FF9800".toColorInt(),
                centerX, barWidth, centerYOffset, maxValue, contentHeight)
            drawBar(canvas, s.learned, barWidth + spacing, "#4CAF50".toColorInt(),
                centerX, barWidth, centerYOffset, maxValue, contentHeight)

            val label = try {
                outFmt.format(inFmt.parse(s.date)!!)
            } catch (_: Exception) {
                s.date.takeLast(5)
            }
            canvas.drawText(label, centerX, centerYOffset + 25f, textPaint)
        }

        drawLegend(canvas, w, h)
    }

    private fun drawBar(
        canvas: Canvas,
        value: Int,
        offset: Float,
        color: Int,
        centerX: Float,
        barWidth: Float,
        centerYOffset: Float,
        maxValue: Float,
        contentHeight: Float
    ) {
        if (value <= 0 || maxValue <= 0f) return

        val barHeight = (value / maxValue) * contentHeight
        val left = centerX + offset - barWidth / 2f
        val right = left + barWidth
        val top = centerYOffset - barHeight

        rect.set(left, top, right, centerYOffset)
        barPaint.color = color
        canvas.drawRoundRect(rect, 6f, 6f, barPaint)
    }

    private fun drawLegend(canvas: Canvas, w: Float, h: Float) {
        val legendY = h - 10f
        val rectSize = 12f
        val totalWidth = 180f
        val startX = (w - totalWidth) / 2f

        val oldTextSize = textPaint.textSize
        val oldTextColor = textPaint.color

        textPaint.textSize = 14f
        textPaint.color = Color.DKGRAY
        textPaint.textAlign = Paint.Align.LEFT

        barPaint.color = "#FF9800".toColorInt()
        canvas.drawRect(startX, legendY - rectSize, startX + rectSize, legendY, barPaint)
        canvas.drawText("В процессе", startX + rectSize + 4f, legendY, textPaint)

        barPaint.color = "#4CAF50".toColorInt()
        canvas.drawRect(startX + 100f, legendY - rectSize, startX + 100f + rectSize, legendY, barPaint)
        canvas.drawText("Выученные", startX + 100f + rectSize + 4f, legendY, textPaint)

        textPaint.textSize = oldTextSize
        textPaint.color = oldTextColor
        textPaint.textAlign = Paint.Align.CENTER
    }
}