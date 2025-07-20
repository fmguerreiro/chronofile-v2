// © Art Chaidarun

package com.chaidarun.chronofile

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

class ActivityFrequencyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var frequencyData: ActivityFrequencyData? = null
    private var hoveredSlot: Int = -1
    private var onSlotClickListener: ((TimeSlotData) -> Unit)? = null
    
    // Paint objects
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = ContextCompat.getColor(context, android.R.color.white)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val hoverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.black)
        alpha = 50
    }
    
    // Activity colors - expand this as needed
    private val activityColors = mapOf(
        "sleep" to "#8E24AA",
        "work" to "#1976D2", 
        "exercise" to "#388E3C",
        "food" to "#F57C00",
        "commute" to "#5D4037",
        "leisure" to "#00ACC1",
        "social" to "#E91E63",
        "study" to "#7B1FA2",
        "default" to "#607D8B"
    )
    
    // Dimensions
    private var barWidth = 0f
    private var barSpacing = 0f
    private var maxBarHeight = 0f
    private val paddingHorizontal = 16f
    private val paddingVertical = 8f
    
    fun setData(data: ActivityFrequencyData) {
        frequencyData = data
        calculateDimensions()
        invalidate()
    }
    
    fun setOnSlotClickListener(listener: (TimeSlotData) -> Unit) {
        onSlotClickListener = listener
    }
    
    private fun calculateDimensions() {
        val availableWidth = width - (paddingHorizontal * 2)
        val availableHeight = height - (paddingVertical * 2)
        
        barWidth = availableWidth / ActivityFrequencyData.SLOTS_PER_DAY
        barSpacing = 0f // Bars touch each other for continuous timeline effect
        maxBarHeight = availableHeight
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val data = frequencyData ?: return
        
        // Find max frequency for scaling
        val maxFrequency = data.timeSlots.maxOfOrNull { it.frequency } ?: 1f
        val scaleFactor = if (maxFrequency > 0) 1f / maxFrequency else 1f
        
        data.timeSlots.forEachIndexed { index, slot ->
            drawTimeSlot(canvas, slot, index, scaleFactor)
        }
    }
    
    private fun drawTimeSlot(canvas: Canvas, slot: TimeSlotData, index: Int, scaleFactor: Float) {
        val x = paddingHorizontal + (index * barWidth)
        val barHeight = if (slot.isEmpty) {
            maxBarHeight * 0.05f // Minimum height for empty slots
        } else {
            max(maxBarHeight * 0.05f, maxBarHeight * slot.frequency * scaleFactor)
        }
        val y = height - paddingVertical - barHeight
        
        // Choose color based on dominant activity
        val color = getActivityColor(slot.dominantActivity)
        barPaint.color = Color.parseColor(color)
        
        // Apply opacity based on consistency
        val opacity = if (slot.isEmpty) 0.1f else min(1f, 0.3f + slot.consistency * 0.7f)
        barPaint.alpha = (opacity * 255).toInt()
        
        // Draw bar
        val rect = RectF(x, y, x + barWidth, height - paddingVertical)
        canvas.drawRect(rect, barPaint)
        
        // Draw hover overlay if this slot is hovered
        if (index == hoveredSlot) {
            canvas.drawRect(rect, hoverPaint)
        }
        
        // Draw time labels for key hours (every 4 hours = 8 slots)
        if (index % 8 == 0) {
            val timeLabel = slot.startTime.substring(0, 2) // Just hour part
            canvas.drawText(
                timeLabel,
                x + barWidth / 2,
                height - 4f,
                textPaint.apply { 
                    color = ContextCompat.getColor(context, android.R.color.tertiary_text_dark)
                    textSize = 20f
                }
            )
        }
    }
    
    private fun getActivityColor(activity: String?): String {
        if (activity == null) return activityColors["default"]!!
        
        val activityLower = activity.lowercase()
        return activityColors.entries.find { (key, _) -> 
            activityLower.contains(key) 
        }?.value ?: activityColors["default"]!!
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val touchX = event.x
                val slotIndex = ((touchX - paddingHorizontal) / barWidth).toInt()
                
                if (slotIndex >= 0 && slotIndex < ActivityFrequencyData.SLOTS_PER_DAY) {
                    if (hoveredSlot != slotIndex) {
                        hoveredSlot = slotIndex
                        invalidate()
                    }
                } else {
                    if (hoveredSlot != -1) {
                        hoveredSlot = -1
                        invalidate()
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (hoveredSlot >= 0) {
                    frequencyData?.timeSlots?.getOrNull(hoveredSlot)?.let { slot ->
                        onSlotClickListener?.invoke(slot)
                    }
                }
                hoveredSlot = -1
                invalidate()
                return true
            }
            
            MotionEvent.ACTION_CANCEL -> {
                hoveredSlot = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    companion object {
        private const val TAG = "ActivityFrequencyChartView"
    }
}