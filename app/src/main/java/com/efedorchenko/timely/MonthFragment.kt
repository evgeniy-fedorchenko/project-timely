package com.efedorchenko.timely

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
import org.threeten.bp.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthFragment : Fragment() {

    companion object {
        private const val ARG_MONTH_OFFSET = "month_offset"

        fun newInstance(monthOffset: Int): MonthFragment {
            return MonthFragment().apply {
                arguments = Bundle().apply { putInt(ARG_MONTH_OFFSET, monthOffset) }
            }
        }
    }

    private var monthOffset: Int = 0
    private lateinit var calendarGrid: GridLayout
    private lateinit var weekDays: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monthOffset = arguments?.getInt(ARG_MONTH_OFFSET) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_month, container, false)
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

        val currentDate = LocalDate.now().plusMonths(monthOffset.toLong())
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        val daysInMonth = currentDate.lengthOfMonth()
        val dayOfWeekOfFirstDay = (firstDayOfMonth.dayOfWeek.value + 6) % 7

        for (i in 0 until 6 * 7) {
            val context = requireContext()
            val frameLayout = FrameLayout(context)
            val params = GridLayout.LayoutParams()

            params.width = 0
            params.height = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            frameLayout.layoutParams = params

            val textView = TextView(context)
            TextViewCompat.setTextAppearance(textView, R.style.CalendarCell)

            val dayOfMonth = i - dayOfWeekOfFirstDay + 1
            if (dayOfMonth in 1..daysInMonth) {
                val drawable = ContextCompat.getDrawable(context, R.drawable.active_day_background)
                val paddingEnd = resources.getDimensionPixelSize(R.dimen.calendar_cell_padding_end)
                val paddingTop = resources.getDimensionPixelSize(R.dimen.calendar_cell_padding_top)

                frameLayout.background = drawable
                textView.text = dayOfMonth.toString()
                textView.gravity = Gravity.TOP or Gravity.END
                textView.setPadding(0, paddingTop, paddingEnd, 0)
                TextViewCompat.setTextAppearance(textView, R.style.CalendarCell_Active)

            } else {
                val drawable = ContextCompat.getDrawable(context, R.drawable.inactive_day_background)
                frameLayout.background = drawable
                TextViewCompat.setTextAppearance(textView, R.style.CalendarCell_Inactive)
            }
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
}
