package com.efedorchenko.timely.ui.fragment

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.databinding.CalendarGridLayoutBinding
import com.efedorchenko.timely.model.CalendarCellBuilder
import com.efedorchenko.timely.model.CalendarCellBuilder.CellType
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.SaveResult
import com.efedorchenko.timely.model.applyTo
import com.efedorchenko.timely.model.deleteFrom
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.dialog.AddEventDialog
import com.efedorchenko.timely.ui.support.AddAbstractDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.threeten.bp.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CalendarFragment : Fragment(), AddAbstractDataListener<Event> {

    companion object {
        // FIXME: SELECTED_DATE_KEY дублируется в AddEventDialog
        private const val MONTH_OFFSET_ARG = "month_offset"
        const val SELECTED_DATE_KEY = "selected_date"
        const val EXISTING_EVENT_WORK_DURATION = "existing_work_duration"
        const val EXISTING_EVENT_COMMENT = "existing_comment"
        const val EXISTING_EVENT_APP_ID = "existing_app_id"
        private const val ADD_EVENT_DIALOG_TAG = "add_event_dialog"

        private val DATE_FORMATTER = SimpleDateFormat("LLLL yyyy", Locale("ru"))

        fun newInstance(monthOffset: Int, userUuid: String?): CalendarFragment {
            return CalendarFragment().apply {
                arguments = Bundle().apply {
                    putInt(MONTH_OFFSET_ARG, monthOffset)
                    putString(AbstractMainFragment.USER_UUID_ARG, userUuid)
                }
            }
        }
    }

    private var _binding: CalendarGridLayoutBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    @Inject
    lateinit var dataService: DataService

    private var monthOffset: Int = 0
    private lateinit var monthEventsDef: Deferred<Map<LocalDate, Event>>
    private lateinit var monthEvents: Map<LocalDate, Event>

    private lateinit var calendarGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        monthOffset = arguments?.getInt(MONTH_OFFSET_ARG) ?: 0
        monthEventsDef = viewModel.getEventsAsync(monthOffset, getUserUuidArgument())

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CalendarGridLayoutBinding.inflate(inflater, container, false)
        calendarGrid = binding.calendarGrid
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateCalendar()

        viewModel.monthOffset.observe(viewLifecycleOwner) { updateMonthTextView(it) }
        lifecycleScope.launch {
            viewModel.needUpdateData.collect { needsUpdate ->
                if (needsUpdate) {
                    monthEventsDef =
                        viewModel.getEventsAsync(monthOffset, getUserUuidArgument())
                    updateCalendar()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /*
    * Если календарь чужой и ты не админ - игнор
    * Если календарь свой и дата прошла - datePassed
    * Усли смена уже есть и ты не админ - cannotEditPlaned
    */
    override fun showAddDataDialog(targetDate: LocalDate, context: Context?, existedData: Event?) {
        if (context == null) return
        val userUuid = getUserUuidArgument()

        if (userUuid != null && !encProfileStorage.isPrivileged()) {
            return
        }
        if (LocalDate.now().isAfter(targetDate)) {
            ToastHelper.datePassed(context)
            return
        }
        if (existedData != null && !encProfileStorage.isPrivileged()) {
            ToastHelper.cannotEditPlaned(context)
            return
        }

        val bundle = Bundle()
        bundle.putString(SELECTED_DATE_KEY, targetDate.toString())
        existedData?.let {
            bundle.putLong(EXISTING_EVENT_WORK_DURATION, existedData.workDuration.seconds)
            bundle.putString(EXISTING_EVENT_COMMENT, existedData.comment)
            bundle.putLong(EXISTING_EVENT_APP_ID, existedData.appId ?: 0)
        }

        val addEventDialog = AddEventDialog.newInstance(this)
        addEventDialog.arguments = bundle
        addEventDialog.show(parentFragmentManager, ADD_EVENT_DIALOG_TAG)
    }

    override fun onSaveData(data: Event) {
        updateCell(data)
        lifecycleScope.launch {
            when (val result = dataService.saveData(data, getUserUuidArgument())) {
                is SaveResult.ServerChanged -> updateCell(result.newData as Event)
                is SaveResult.Error -> cleanCell(data)
                is SaveResult.Success -> {}
                is SaveResult.SyncFiled -> viewModel.emitNotSynced.invoke()
            }
        }
    }

    private fun cleanCell(event: Event) {
        val cellIdx = event.date.dayOfMonth + ((event.date.withDayOfMonth(1).dayOfWeek.value + 6) % 7) - 1
        val targetCell = calendarGrid.getChildAt(cellIdx) as? ConstraintLayout
        targetCell?.let { event.deleteFrom(targetCell, cellIdx) }
    }

    private fun updateCell(event: Event?) {
        if (event == null) return
        val cellIdx = event.date.dayOfMonth + ((event.date.withDayOfMonth(1).dayOfWeek.value + 6) % 7) - 1
        val targetCell = calendarGrid.getChildAt(cellIdx) as? ConstraintLayout
        targetCell?.let {
//            it.setOnClickListener { ToastHelper.cannotEditPlaned(requireContext()) }
            event.applyTo(targetCell, cellIdx, true)
        }
    }

    private fun updateCalendar() {
        if (getUserUuidArgument() == null && encProfileStorage.isPrivileged()) {

            return
        }
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
                dayOfMonth < 1 -> cellBuilder.setDate(
                    pastMonth.withDayOfMonth(dayOfMonth + pastMonth.lengthOfMonth())
                )

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

            cell.event?.applyTo(parentLayout, i, false)
            parentLayout.addView(textView)
            calendarGrid.addView(parentLayout)
        }
    }

    private fun updateMonthTextView(monthOffset: Int) {
        activity?.findViewById<TextView>(R.id.center_header)?.let {
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

    // TODO: посмотреть, может быстрее создать схему и инфлейтить ее
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

    private fun getUserUuidArgument() = arguments?.getString(AbstractMainFragment.USER_UUID_ARG)
}
