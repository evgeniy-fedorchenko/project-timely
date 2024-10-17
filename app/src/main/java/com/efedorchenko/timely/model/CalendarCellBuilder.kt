package com.efedorchenko.timely.model

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import androidx.fragment.app.FragmentManager
import com.efedorchenko.timely.R
import com.efedorchenko.timely.fragment.CalendarFragment
import com.efedorchenko.timely.service.OnSaveEventListener
import com.efedorchenko.timely.service.ToastHelper
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.LocalDate

class CalendarCellBuilder(private val context: Context) {

    companion object {
        private const val ADD_EVENT_DIALOG_TAG = "add_event_dialog"
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
                    LocalDate.now().isAfter(date) -> ToastHelper.datePassed(context)
                    event != null -> ToastHelper.cannotEditPlaned(context)
                    else -> OnSaveEventListener.eventDialog(date, fragment!!)
                        .show(fragment!!.parentFragmentManager, ADD_EVENT_DIALOG_TAG)
                }
            }
        }
        return CalendarCell(text, textStyle, parentBackground, onClickListener, event)
    }

    enum class CellType {
        CURRENT_MONTH,
        NOT_CURRENT_MONTH
    }

}