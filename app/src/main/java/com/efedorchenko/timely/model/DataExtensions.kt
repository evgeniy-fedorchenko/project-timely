package com.efedorchenko.timely.model

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.core.widget.TextViewCompat
import com.efedorchenko.timely.R
import org.threeten.bp.LocalDate
import java.util.Locale


private const val TEXT_CELL_TAG_PREFIX = "text_"
private const val MARKER_CELL_TAG_PREFIX = "square_"

fun List<Event>.toEventMap(): MutableMap<LocalDate, Event> {
    val map = HashMap<LocalDate, Event>()
    for (event in this) {
        map[event.date] = event
    }
    return map
}

fun Event.applyTo(parentLayout: ConstraintLayout, cellIdx: Int, needReplace: Boolean) {
    val context = parentLayout.context
    val resources = parentLayout.resources

    val markerView = View(context)
    markerView.tag = "$MARKER_CELL_TAG_PREFIX$cellIdx"

    markerView.layoutParams = cellColorMarkParams(resources)
    val color = when {
        date.isBefore(LocalDate.now()) -> Color.GREEN.getColorValue(context)
        else -> Color.ORANGE.getColorValue(context)
    }
    markerView.setBackgroundColor(color)
    markerView.alpha = 0.5f

    val textView = TextView(context)
    textView.tag = "$TEXT_CELL_TAG_PREFIX$cellIdx"
    val minutesCount = workDuration.toMinutes()
    val minutes = minutesCount / 60
    val hours = minutesCount % 60

    textView.text = String.format(Locale("ru"), "%02d:%02d", minutes, hours)
    textView.textSize = 18F
    textView.layoutParams = cellHoursTextParams(resources)
    TextViewCompat.setTextAppearance(textView, R.style.work_duration)

    if (needReplace) {
        val existingMarkerView = parentLayout.findViewWithTag<View>("$MARKER_CELL_TAG_PREFIX$cellIdx")
        val existingTextView = parentLayout.findViewWithTag<View>("$TEXT_CELL_TAG_PREFIX$cellIdx")
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

fun Event.deleteFrom(parentLayout: ConstraintLayout, cellIdx: Int) {
    val existingMarkerView = parentLayout.findViewWithTag<View>("$MARKER_CELL_TAG_PREFIX$cellIdx")
    val existingTextView = parentLayout.findViewWithTag<View>("$TEXT_CELL_TAG_PREFIX$cellIdx")
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