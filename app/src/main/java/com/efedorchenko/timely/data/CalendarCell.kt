package com.efedorchenko.timely.data

import android.view.View.OnClickListener
import com.efedorchenko.timely.R
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.LocalDate

data class CalendarCell(
    val text: String,
    val textStyle: Int,
    val parentBackground: Int,
    val onClickListener: OnClickListener?,
    val event: Event?,
) {

    class Builder {

        private var date: LocalDate = LocalDate.now()
        private var type: CellType = CellType.CURRENT_MONTH
        private var onClickListener: OnClickListener? = null
        private var event: Event? = null

        fun setDate(date: LocalDate) = apply {
            this.date = date
        }

        fun setType(type: CellType) = apply {
            this.type = type
        }

        fun setOnClickListener(listener: OnClickListener) = apply {
            this.onClickListener = listener
        }

        fun setEvent(event: Event?) = apply {
            this.event = event
        }

        fun build(): CalendarCell {
            val text = date.dayOfMonth.toString()
            val textStyle: Int
            val parentBackground: Int

            when (type) {
                CellType.PAST_MONTH -> {
                    textStyle = R.style.inactive_date
                    parentBackground = R.drawable.inactive_day
                }

                CellType.CURRENT_MONTH -> {
                    textStyle = R.style.active_date

                    val isWeekend = date.dayOfWeek == SATURDAY || date.dayOfWeek == SUNDAY
                    val isToday = LocalDate.now().isEqual(date)
                    parentBackground = when {
                        isWeekend && isToday -> R.drawable.weekend_current
                        isWeekend && !isToday -> R.drawable.weekend_ordinary
                        !isWeekend && isToday -> R.drawable.weekday_current
                        else -> R.drawable.weekday_ordinary
                    }
                }

                CellType.NEXT_MONTH -> {
                    textStyle = R.style.inactive_date
                    parentBackground = R.drawable.inactive_day
                }
            }
            return CalendarCell(text, textStyle, parentBackground, onClickListener, event)
        }
    }

    enum class CellType {
        PAST_MONTH,
        CURRENT_MONTH,
        NEXT_MONTH
    }

}