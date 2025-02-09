package com.efedorchenko.timely.model.calendar

import com.efedorchenko.timely.R
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.ui.fragment.CalendarFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.LocalDate

class CalendarBuilder(monthOffset: Int) {

    private val currentMonth: LocalDate
    private val dayOfWeekOfFirstDay: Int
    private val pastMonth: LocalDate
    private val nextMonth: LocalDate

    private var clickListener: CalendarFragment? = null
    private var eventsDef: Deferred<Map<LocalDate, Event>>? = null
    private var eventsReady: Map<LocalDate, Event>? = null

    init {
        val nowOffset = LocalDate.now().plusMonths(monthOffset.toLong())
        currentMonth = nowOffset
        dayOfWeekOfFirstDay = (nowOffset.withDayOfMonth(1).dayOfWeek.value + 6) % 7
        pastMonth = nowOffset.minusMonths(1)
        nextMonth = nowOffset.plusMonths(1)
    }

    fun clickListener(clickListener: CalendarFragment): CalendarBuilder {
        this.clickListener = clickListener
        return this
    }

    fun eventsDef(eventsDef: Deferred<Map<LocalDate, Event>>): CalendarBuilder {
        this.eventsDef = eventsDef
        return this
    }

    fun buildForIndex(index: Int): CalendarCell {
        val dayOfMonth = index - dayOfWeekOfFirstDay + 1
        val currentDate = when {
            dayOfMonth < 1 -> pastMonth.withDayOfMonth(dayOfMonth + pastMonth.lengthOfMonth())
            dayOfMonth in 1..currentMonth.lengthOfMonth() -> currentMonth.withDayOfMonth(dayOfMonth)
            else -> nextMonth.withDayOfMonth(dayOfMonth - currentMonth.lengthOfMonth())
        }

        val isCurrentMonth = dayOfMonth in 1..currentMonth.lengthOfMonth()
        val cellType = if (isCurrentMonth) CellType.CURRENT_MONTH else CellType.NOT_CURRENT_MONTH
        if (eventsReady == null) {
            runBlocking { eventsReady = withTimeoutOrNull(1000) { eventsDef?.await() } ?: emptyMap() }
        }
        val existedData = eventsReady?.get(currentDate)
        return CalendarCell(
            currentDate.dayOfMonth.toString(),
            getTextStyle(cellType),
            getBackground(cellType, currentDate),
            { clickListener?.showAddDataDialog(currentDate, it.context, existedData) },
            existedData
        )
    }

    private fun getTextStyle(cellType: CellType) = when (cellType) {
        CellType.NOT_CURRENT_MONTH -> R.style.inactive_date
        CellType.CURRENT_MONTH -> R.style.active_date
    }

    private fun getBackground(cellType: CellType, currentDate: LocalDate) = when (cellType) {
        CellType.NOT_CURRENT_MONTH -> R.drawable.inactive_day
        CellType.CURRENT_MONTH -> {
            val isToday = LocalDate.now().isEqual(currentDate)
            val isWeekend = currentDate.dayOfWeek == SATURDAY || currentDate.dayOfWeek == SUNDAY
            when {
                isWeekend && isToday -> R.drawable.weekend_current
                isWeekend && !isToday -> R.drawable.weekend_ordinary
                !isWeekend && isToday -> R.drawable.weekday_current
                else -> R.drawable.weekday_ordinary
            }
        }
    }

    private enum class CellType {
        CURRENT_MONTH,
        NOT_CURRENT_MONTH
    }
}
