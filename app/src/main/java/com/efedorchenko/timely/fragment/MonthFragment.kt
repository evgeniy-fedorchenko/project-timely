package com.efedorchenko.timely.fragment

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.efedorchenko.timely.R
import com.efedorchenko.timely.event.Event
import com.efedorchenko.timely.event.OnSaveEventListener
import org.threeten.bp.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthFragment : Fragment(), OnSaveEventListener {

    companion object {
        private const val MONTH_OFFSET_ARG = "month_offset"
        const val SELECTED_DATE_KEY = "selected_date"
        private const val ADD_EVENT_DIALOG_TAG = "add_event_dialog"

        fun newInstance(monthOffset: Int): MonthFragment {
            return MonthFragment().apply {
                arguments = Bundle().apply { putInt(MONTH_OFFSET_ARG, monthOffset) }
            }
        }
    }

    private val events: List<Event> = ArrayList()
    private var monthOffset: Int = 0
    private lateinit var calendarGrid: GridLayout
    private lateinit var weekDays: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monthOffset = arguments?.getInt(MONTH_OFFSET_ARG) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.calendar_grid_layout, container, false)
        calendarGrid = view.findViewById(R.id.calendar_grid)
        weekDays = view.findViewById(R.id.days_of_week_grid)

        val context = requireContext()
        setupWeekDays(context)
        updateCalendar(context)
        return view
    }

    override fun onResume() {
        super.onResume()
        updateMonthTextView()
    }

    private fun setupWeekDays(context: Context) {
        weekDays.removeAllViews()
        val daysOfWeek = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        for (i in daysOfWeek.indices) {
            val frameLayout = createFrameLayout(context)
            val textView = TextView(context)

            textView.text = daysOfWeek[i]
            textView.gravity = Gravity.CENTER
            TextViewCompat.setTextAppearance(textView, R.style.weekday_cell)
            frameLayout.background = drawable(context, R.drawable.weekday_ordinary)

            frameLayout.addView(textView)
            weekDays.addView(frameLayout)
        }
    }

    private fun updateCalendar(context: Context) {
        calendarGrid.removeAllViews()

        val dateNow = LocalDate.now().plusMonths(monthOffset.toLong())
        val currentDayOfMonth = dateNow.dayOfMonth.toString()
        val monthLength = dateNow.lengthOfMonth()
        val dayOfWeekOfFirstDay = (dateNow.withDayOfMonth(1).dayOfWeek.value + 6) % 7

        for (i in 0 until 6 * 7) {
            val frameLayout = createFrameLayout(context)
            val textView = TextView(context)
            val dayOfMonth = i - dayOfWeekOfFirstDay + 1

            when {
                dayOfMonth < 1 -> {
                    val processingDate = dateNow.minusMonths(1).lengthOfMonth() + dayOfMonth
                    textView.text = processingDate.toString()
                    TextViewCompat.setTextAppearance(textView, R.style.inactive_calendar_cell)
                    frameLayout.background = drawable(context, R.drawable.inactive_day)
                }

                dayOfMonth in 1..monthLength -> {
                    textView.text = dayOfMonth.toString()
                    TextViewCompat.setTextAppearance(textView, R.style.active_calendar_cell)

                    val isWeekendDay = ((i + 2) % 7 == 0) or ((i + 1) % 7 == 0)
                    val isToday = currentDayOfMonth == dayOfMonth.toString() && monthOffset == 0

                    val drawableResource = when {
                        isWeekendDay && isToday -> R.drawable.weekend_current
                        isWeekendDay && !isToday -> R.drawable.weekend_ordinary
                        !isWeekendDay && isToday -> R.drawable.weekday_current
                        else -> R.drawable.weekday_ordinary
                    }
                    frameLayout.background = drawable(context, drawableResource)
                    frameLayout.setOnClickListener {
                        val bundle = Bundle()
                        val selectedDateStr = dateNow.withDayOfMonth(dayOfMonth).toString()
                        bundle.putString(SELECTED_DATE_KEY, selectedDateStr)

                        val addEventDialog = AddEventDialog.newInstance(this)
                        addEventDialog.arguments = bundle
                        addEventDialog.show(parentFragmentManager, ADD_EVENT_DIALOG_TAG)
                    }
                }

                else -> {
                    textView.text = (dayOfMonth - monthLength).toString()
                    TextViewCompat.setTextAppearance(textView, R.style.inactive_calendar_cell)
                    frameLayout.background = drawable(context, R.drawable.inactive_day)
                }
            }
            val topPadding = resources.getDimensionPixelSize(R.dimen.calendar_cell_padding_top)
            val rightPadding = resources.getDimensionPixelSize(R.dimen.calendar_cell_padding_end)
            textView.setPadding(0, topPadding, rightPadding, 0)

            textView.gravity = Gravity.TOP or Gravity.END
            frameLayout.addView(textView)
            calendarGrid.addView(frameLayout)
        }
    }


    private fun updateMonthTextView() {
        val activity = activity ?: return
        val monthTextView: TextView? = activity.findViewById(R.id.month_year_text)
        monthTextView?.let {
            val calendar = Calendar.getInstance(Locale("ru"))
            calendar.add(Calendar.MONTH, monthOffset)

            val formatter = SimpleDateFormat("LLLL yyyy", Locale("ru"))
            var monthName = formatter.format(calendar.time)
            if (monthName.isNotEmpty()) {
                monthName = monthName.substring(0, 1)
                    .uppercase(Locale.getDefault()) + monthName.substring(1)
            }
            it.text = monthName
        }
    }

    private fun drawable(context: Context, drawableResource: Int) =
        ContextCompat.getDrawable(context, drawableResource)

    private fun createFrameLayout(context: Context): FrameLayout {
        val frameLayout = FrameLayout(context)
        val params = GridLayout.LayoutParams()

        params.width = 0
        params.height = 0
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        frameLayout.layoutParams = params
        return frameLayout
    }

    override fun onSaveEvent(event: Event) {
        /*
        * Отобразить на календаре
        * Отправить на бек
        */
    }
}
