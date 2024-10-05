package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.efedorchenko.timely.Helper
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DatabaseHelper
import com.efedorchenko.timely.data.Event
import com.efedorchenko.timely.data.OnSaveEventListener
import com.efedorchenko.timely.fragment.CalendarCell.CellType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthFragment : Fragment(), OnSaveEventListener {

    companion object {
        private const val MONTH_OFFSET_ARG = "month_offset"
        private const val ADD_EVENT_DIALOG_TAG = "add_event_dialog"
        const val SELECTED_DATE_KEY = "selected_date"
        private val eventsCache: MutableMap<Int, MutableMap<LocalDate, Event>> = HashMap()

        fun newInstance(monthOffset: Int): MonthFragment {
            return MonthFragment().apply {
                arguments = Bundle().apply { putInt(MONTH_OFFSET_ARG, monthOffset) }
            }
        }
    }

    private var monthOffset: Int = 0
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var calendarHelper: CalendarHelper
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
        databaseHelper = DatabaseHelper(context)
        calendarHelper = CalendarHelper(context)

//        Когда юзер логинится - просить все ивенты с бека и обновлять бд
        val monthUID = Helper.getMonthUID(LocalDate.now().plusMonths(monthOffset.toLong()))
        val monthEvents = eventsCache[monthUID]
        if (monthEvents == null) {
            eventsCache[monthUID] = Helper.toMap(databaseHelper.findByMonth(monthUID, false))
        }

        setupWeekDays()
        updateCalendar()
        return view
    }

    override fun onResume() {
        super.onResume()
        updateMonthTextView()
    }

    override fun onSaveEvent(event: Event) {
        var monthEvents = eventsCache[Helper.getMonthUID(event.eventDate)]
        monthEvents?.let { monthEvents[event.eventDate] = event }

        updateCalendar()
        CoroutineScope(Dispatchers.IO).launch {
            databaseHelper.save(event)
            // Отправить данные на бек
        }
    }

    private fun setupWeekDays() {
        weekDays.removeAllViews()
        val context = requireContext()
        val daysOfWeek = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        for (i in daysOfWeek.indices) {
            val parentLayout = createConstraintLayout()
            val textView = TextView(context)

            textView.text = daysOfWeek[i]
            textView.gravity = Gravity.CENTER
            TextViewCompat.setTextAppearance(textView, R.style.day_of_week)
            parentLayout.background =
                ContextCompat.getDrawable(context, R.drawable.weekday_ordinary)

            parentLayout.addView(textView)
            weekDays.addView(parentLayout)
        }
    }

    private fun updateCalendar() {
        calendarGrid.removeAllViews()
        val context = requireContext()

        val currentMonth = LocalDate.now().plusMonths(monthOffset.toLong())
        val dayOfWeekOfFirstDay = (currentMonth.withDayOfMonth(1).dayOfWeek.value + 6) % 7
        val pastMonth = currentMonth.minusMonths(1)
        val nextMonth = currentMonth.plusMonths(1)

        val monthUID = Helper.getMonthUID(currentMonth)
        val monthEvents = eventsCache[monthUID]
        val currentEvents: Map<LocalDate, Event>

        currentEvents = if (monthEvents == null) HashMap() else monthEvents
        monthEvents?.let { eventsCache[monthUID] = monthEvents }

        for (i in 0 until 6 * 7) {

            val dayOfMonth = i - dayOfWeekOfFirstDay + 1
            val cellBuilder = CalendarCell.Builder()
            when {
                dayOfMonth < 1 -> cellBuilder
                    .setType(CellType.PAST_MONTH)
                    .setDate(pastMonth.withDayOfMonth(dayOfMonth + pastMonth.lengthOfMonth()))

                dayOfMonth in 1..currentMonth.lengthOfMonth() -> {
                    val processDate = currentMonth.withDayOfMonth(dayOfMonth)
                    val processEvent = currentEvents[processDate]

                    val onClickListener = View.OnClickListener {
                        when {
                            LocalDate.now().isAfter(processDate) -> calendarHelper.oldDateSelected()
                                .show()

                            processEvent != null -> calendarHelper.rejectRewriteEvent().show()
                            else -> calendarHelper.eventDialog(processDate, this)
                                .show(parentFragmentManager, ADD_EVENT_DIALOG_TAG)
                        }
                    }
                    cellBuilder
                        .setDate(processDate)
                        .setType(CellType.CURRENT_MONTH)
                        .setOnClickListener(onClickListener)
                        .setEvent(processEvent)
                }

                else -> cellBuilder
                    .setType(CellType.NEXT_MONTH)
                    .setDate(nextMonth.withDayOfMonth(dayOfMonth - currentMonth.lengthOfMonth()))
            }
            val cell = cellBuilder.build()

            val textView = createTextView()
            textView.text = cell.text
            TextViewCompat.setTextAppearance(textView, cell.textStyle)

            val parentLayout = createConstraintLayout()
            parentLayout.background = ContextCompat.getDrawable(context, cell.parentBackground)
            parentLayout.setOnClickListener(cell.onClickListener)

            cell.event?.applyTo(parentLayout)
            parentLayout.addView(textView)
            calendarGrid.addView(parentLayout)
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

    private fun createTextView(): TextView {
        val textView = TextView(context)
        val topPadding = resources.getDimensionPixelSize(R.dimen.calendar_date_padding_top)
        val rightPadding = resources.getDimensionPixelSize(R.dimen.calendar_date_padding_end)
        textView.setPadding(0, topPadding, rightPadding, 0)

        val layoutParams = ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        textView.layoutParams = layoutParams

        return textView
    }

    private fun createConstraintLayout(): ConstraintLayout {
        val constraintLayout = ConstraintLayout(requireContext())
        val params = GridLayout.LayoutParams()

        params.width = 0
        params.height = 0
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        constraintLayout.layoutParams = params
        return constraintLayout
    }

}
