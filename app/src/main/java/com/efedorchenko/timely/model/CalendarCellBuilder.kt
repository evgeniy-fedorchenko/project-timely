package com.efedorchenko.timely.model

import android.view.View
import android.view.View.OnClickListener
import androidx.fragment.app.FragmentManager
import com.efedorchenko.timely.R
import com.efedorchenko.timely.fragment.CalendarFragment
import com.efedorchenko.timely.service.CalendarHelper
import com.efedorchenko.timely.service.OnSaveEventListener
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.LocalDate

class CalendarCellBuilder {

    companion object {
        private const val ADD_EVENT_DIALOG_TAG = "add_event_dialog"
        private const val DATE_PASSSED = "Эта дата уже прошла"
        private const val CANNOT_EDIT = "Запланированную смену нельзя редактировать!"
    }

    private var fragment: CalendarFragment? = null
    private var date: LocalDate = LocalDate.now()
    private var type: CellType = CellType.NOT_CURRENT_MONTH
    private var useOnClickListenerWith: FragmentManager? = null
    private var event: Event? = null

    fun setDate(date: LocalDate) = apply {
        this.date = date
    }

    fun setType(type: CellType) = apply {
        this.type = type
    }

    fun setEvent(event: Event?) = apply {
        this.event = event
    }

    fun setOnClickListenerFor(fragment: CalendarFragment) = apply {
        this.fragment = fragment
    }

    fun build(): CalendarCell {
        val text = date.dayOfMonth.toString()
        val textStyle: Int
        val parentBackground: Int
        var onClickListener: OnClickListener? = null

        when (type) {
            CellType.NOT_CURRENT_MONTH -> {
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
        }
        if (fragment != null) {
            onClickListener = View.OnClickListener {
                when {
                    LocalDate.now().isAfter(date) ->
                        CalendarHelper.showToast(DATE_PASSSED, fragment!!.requireContext())

                    event != null ->
                        CalendarHelper.showToast(CANNOT_EDIT, fragment!!.requireContext())

                    else -> OnSaveEventListener.eventDialog(date, fragment!!, cellIdxOf(date))
                        .show(fragment!!.parentFragmentManager, ADD_EVENT_DIALOG_TAG)
                }
            }
        }
        return CalendarCell(text, textStyle, parentBackground, onClickListener, event)
    }

    fun cellIdxOf(date: LocalDate): Int {
        return date.dayOfMonth + ((date.withDayOfMonth(1).dayOfWeek.value + 6) % 7) - 1
    }

    enum class CellType {
        CURRENT_MONTH,
        NOT_CURRENT_MONTH
    }

}