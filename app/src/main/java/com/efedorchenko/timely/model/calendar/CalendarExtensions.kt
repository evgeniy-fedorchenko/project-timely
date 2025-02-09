package com.efedorchenko.timely.model.calendar

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.core.widget.TextViewCompat
import com.efedorchenko.timely.R
import com.efedorchenko.timely.model.Event
import org.threeten.bp.LocalDate
import java.util.Locale


private const val TEXT_VIEW_TAG_PREFIX = "wrk_drn_"
private const val MARKER_VIEW_TAG_PREFIX = "clr_mkr_"
private const val WORK_DURATION_FORMAT = "%02d:%02d"

fun List<Event>.toEventMap(): MutableMap<LocalDate, Event> {
    val map = HashMap<LocalDate, Event>()
    for (event in this) {
        map[event.date] = event
    }
    return map
}

fun Event.calcCellIdx() = date.dayOfMonth + ((date.withDayOfMonth(1).dayOfWeek.value + 6) % 7) - 1

fun Event.applyTo(parentLayout: ConstraintLayout, cellIdx: Int, needReplace: Boolean) {
    val context = parentLayout.context
    val resources = parentLayout.resources

    val markerView = View(context)   // Цветной маркер смены
    markerView.tag = "$MARKER_VIEW_TAG_PREFIX$cellIdx"

    markerView.layoutParams = cellColorMarkParams(resources)
    val color = when {
        date.isBefore(LocalDate.now()) -> Color.GREEN.getColorValue(context)
        else -> Color.ORANGE.getColorValue(context)
    }
    markerView.setBackgroundColor(color)
    markerView.alpha = 0.5f

    val textView = TextView(context)   // Количество часов
    textView.tag = "$TEXT_VIEW_TAG_PREFIX$cellIdx"
    val minutesCount = workDuration.toMinutes()
    val minutes = minutesCount / 60
    val hours = minutesCount % 60

    textView.text = String.format(Locale("ru"), WORK_DURATION_FORMAT, minutes, hours)
    textView.textSize = 18F
    textView.layoutParams = cellHoursTextParams(resources)
    TextViewCompat.setTextAppearance(textView, R.style.work_duration)

    if (needReplace) {
        val existingMarkerView = parentLayout.findViewWithTag<View>("$MARKER_VIEW_TAG_PREFIX$cellIdx")
        val existingTextView = parentLayout.findViewWithTag<View>("$TEXT_VIEW_TAG_PREFIX$cellIdx")
        if (existingTextView != null) {
            parentLayout.removeView(existingTextView)
        }
        if (existingMarkerView != null) {
            parentLayout.removeView(existingMarkerView)
        }
    }
    parentLayout.addView(textView)
    parentLayout.addView(markerView)
}

// Расширяет Event только для понимания контектса выполнения
fun Event.deleteFrom(parentLayout: ConstraintLayout, cellIdx: Int) {
    val existingMarkerView = parentLayout.findViewWithTag<View>("$MARKER_VIEW_TAG_PREFIX$cellIdx")
    val existingTextView = parentLayout.findViewWithTag<View>("$TEXT_VIEW_TAG_PREFIX$cellIdx")
    if (existingTextView != null) {
        parentLayout.removeView(existingTextView)
    }
    if (existingMarkerView != null) {
        parentLayout.removeView(existingMarkerView)
    }
}

private fun cellColorMarkParams(resources: Resources): ViewGroup.LayoutParams {
    val margin = resources.getDimensionPixelSize(R.dimen.half_px)
    val squareLayoutParams = ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, 0
    )
    squareLayoutParams.dimensionRatio = "W,3:10"
    squareLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
    squareLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
    squareLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
    squareLayoutParams.setMargins(margin, margin, margin, margin)

    return squareLayoutParams
}

private fun cellHoursTextParams(resources: Resources): ViewGroup.LayoutParams {
    val layoutParams = ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
    layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
    layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
    layoutParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.work_duration_bottom)

    return layoutParams
}