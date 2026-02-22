package com.example.t_learnappmobile.presentation.statistics

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.t_learnappmobile.data.statistics.DailyStats
import androidx.core.graphics.toColorInt


// Кастомный View для рисования графика статистики. Наследуется от View, чтобы рисовать на Canvas (холсте Android)
// @JvmOverloads для совместимости с Java
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
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (statsList.isEmpty()) return

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom - 40f

        val maxValue = statsList.maxOf {
            maxOf(it.newWords, it.inProgressWords, it.learnedWords)
        }.toFloat()
        if (maxValue <= 0f) return

        val groupWidth = contentWidth.toFloat() / statsList.size
        val barWidth = groupWidth * 0.2f
        val centerYOffset = height - paddingBottom - 40f


        statsList.forEachIndexed { index, s ->
            val centerX = paddingLeft + groupWidth * index + groupWidth / 2

            fun drawBar(value: Int, offsetMultiplier: Float, color: Int) {
                if (value <= 0) return
                val barHeight = (value / maxValue) * contentHeight
                val left = centerX + offsetMultiplier * barWidth - barWidth / 2
                val right = left + barWidth
                val top = centerYOffset - barHeight
                rect.set(left, top, right, centerYOffset)
                barPaint.color = color
                canvas.drawRoundRect(rect, 6f, 6f, barPaint)
            }

            drawBar(s.newWords, -1f, "#2196F3".toColorInt())
            drawBar(s.inProgressWords, 0f, "#FF9800".toColorInt())
            drawBar(s.learnedWords, 1f, "#4CAF50".toColorInt())

            val label = try {
                outFmt.format(inFmt.parse(s.date)!!)
            } catch (_: Exception) {
                s.date.takeLast(5)
            }
            canvas.drawText(label, centerX, height - paddingBottom.toFloat(), textPaint)
        }
    }
}
