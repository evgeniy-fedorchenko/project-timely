package com.efedorchenko.timely.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.ViewModelProvider
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.CalendarGridLayoutBinding
import com.efedorchenko.timely.model.CalendarCellBuilder
import com.efedorchenko.timely.model.CalendarCellBuilder.CellType
import com.efedorchenko.timely.model.CalendarCellBuilder.Companion.CANNOT_EDIT
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.service.MainViewModel
import com.efedorchenko.timely.service.OnSaveEventListener
import com.efedorchenko.timely.service.ToastHelper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.threeten.bp.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment() : OnSaveEventListener() {

    companion object {
        private const val MONTH_OFFSET_ARG = "month_offset"
        private val DATE_FORMATTER = SimpleDateFormat("LLLL yyyy", Locale("ru"))

        fun newInstance(monthOffset: Int): CalendarFragment {
            return CalendarFragment().apply {
                arguments = Bundle().apply { putInt(MONTH_OFFSET_ARG, monthOffset) }
            }
        }
    }

    private var _binding: CalendarGridLayoutBinding? = null
    private val binding get() = _binding!!

    private var monthOffset: Int = 0
    private lateinit var monthEventsDef: Deferred<Map<LocalDate, Event>>
    private lateinit var monthEvents: Map<LocalDate, Event>
    private lateinit var viewModel: MainViewModel

    private lateinit var calendarGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        monthOffset = arguments?.getInt(MONTH_OFFSET_ARG) ?: 0
        monthEventsDef = viewModel.getEventsAsync(monthOffset)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CalendarGridLayoutBinding.inflate(inflater, container, false)
        val view = binding.root

        calendarGrid = binding.calendarGrid
        viewModel.monthOffset.observe(viewLifecycleOwner) { updateMonthTextView(it) }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateCalendar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveEvent(event: Event, processedCellIdx: Int?) {
        updateCell(event, processedCellIdx)
        viewModel.addEvent(event)
        // Отправить данные на бек
    }

    private fun updateCell(event: Event?, processedCellIdx: Int?) {

        if (event != null && processedCellIdx != null) {
            val targetCell = calendarGrid.getChildAt(processedCellIdx) as? ConstraintLayout
            targetCell?.let {
                it.setOnClickListener { ToastHelper.cannotEditPlaned(requireContext()) }
                event.applyTo(targetCell)
            }
        }
    }

    private fun updateCalendar() {
        runBlocking {
            monthEvents = withTimeoutOrNull(1000) { monthEventsDef.await() } ?: emptyMap()
        }
        calendarGrid.removeAllViews()
        val currentMonth = LocalDate.now().plusMonths(monthOffset.toLong())
        val dayOfWeekOfFirstDay = (currentMonth.withDayOfMonth(1).dayOfWeek.value + 6) % 7
        val pastMonth = currentMonth.minusMonths(1)
        val nextMonth = currentMonth.plusMonths(1)
        val context = requireContext()

        for (i in 0 until 6 * 7) {

            val dayOfMonth = i - dayOfWeekOfFirstDay + 1
            val cellBuilder = CalendarCellBuilder(context)
            when {
                dayOfMonth < 1 -> {
                    cellBuilder.setDate(
                        pastMonth.withDayOfMonth(dayOfMonth + pastMonth.lengthOfMonth())
                    )
                }

                dayOfMonth in 1..currentMonth.lengthOfMonth() -> {
                    val processDate = currentMonth.withDayOfMonth(dayOfMonth)
                    cellBuilder
                        .setDate(processDate)
                        .setType(CellType.CURRENT_MONTH)
                        .setOnClickListenerFor(this)
                        .setEvent(monthEvents[processDate])
                }

                else -> cellBuilder.setDate(
                    nextMonth.withDayOfMonth(dayOfMonth - currentMonth.lengthOfMonth())
                )
            }
            val cell = cellBuilder.build()

            val textView = createTextView()
            textView.text = cell.text
            TextViewCompat.setTextAppearance(textView, cell.textStyle)

            val parentLayout = createConstraintLayout(context)
            parentLayout.setOnClickListener(cell.onClickListener)
            parentLayout.background =
                ContextCompat.getDrawable(context, cell.parentBackground)

            cell.event?.applyTo(parentLayout)
            parentLayout.addView(textView)
            calendarGrid.addView(parentLayout)
        }
    }

    private fun updateMonthTextView(monthOffset: Int) {
        activity?.findViewById<TextView>(R.id.month_year_text)?.let {
            val calendar = Calendar.getInstance(Locale("ru"))
            calendar.add(Calendar.MONTH, monthOffset)

            var monthName = DATE_FORMATTER.format(calendar.time)
                monthName = monthName.substring(0, 1)
                    .uppercase(Locale.getDefault()) + monthName.substring(1)

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

    private fun createConstraintLayout(context: Context): ConstraintLayout {
        val constraintLayout = ConstraintLayout(context)
        val params = GridLayout.LayoutParams()

        params.width = 0
        params.height = 0
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        constraintLayout.layoutParams = params
        return constraintLayout
    }

}
