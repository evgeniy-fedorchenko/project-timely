package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.efedorchenko.timely.databinding.CalendarPageBinding
import com.efedorchenko.timely.model.DayModel
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.service.CalendarAdapter
import com.efedorchenko.timely.service.MainViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.threeten.bp.LocalDate
import java.text.SimpleDateFormat
import java.util.Locale

class CalendarFragmentNew() : Fragment() {

    companion object {
        private const val MONTH_OFFSET_ARG = "month_offset"
        private val DATE_FORMATTER = SimpleDateFormat("LLLL yyyy", Locale("ru"))

        fun newInstance(monthOffset: Int): CalendarFragmentNew {
            return CalendarFragmentNew().apply {
                arguments = Bundle().apply { putInt(MONTH_OFFSET_ARG, monthOffset) }
            }
        }
    }

    private var _binding: CalendarPageBinding? = null
    private val binding get() = _binding!!

    private var monthOffset: Int = 0
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var monthEventsDef: Deferred<Map<LocalDate, Event>>
    private lateinit var monthEvents: Map<LocalDate, Event>
    private lateinit var viewModel: MainViewModel

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
    ): View? {
        _binding = CalendarPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gridLayoutManager = GridLayoutManager(context, 7)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1 // Все элементы занимают один столбец
            }
        }

        binding.calendarRecyclerView.layoutManager = gridLayoutManager
        binding.calendarRecyclerView.adapter = CalendarAdapter(generateMonthData())

        // Убедимся, что RecyclerView знает о всех элементах
        binding.calendarRecyclerView.setHasFixedSize(true)

        // Это заставит RecyclerView измерять себя так, чтобы вместить все элементы
        binding.calendarRecyclerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT



//        binding.calendarRecyclerView.layoutManager = GridLayoutManager(context, 7)
//        calendarAdapter = CalendarAdapter(generateMonthData())
//        binding.calendarRecyclerView.adapter = calendarAdapter
//        binding.calendarRecyclerView.setHasFixedSize(true)
    }

    private fun generateMonthData(): List<DayModel> {
        runBlocking {
            monthEvents = withTimeoutOrNull(1000) { monthEventsDef.await() } ?: emptyMap()
        }

        val currentMonth = LocalDate.now().plusMonths(monthOffset.toLong())
        val dayOfWeekOfFirstDay = (currentMonth.withDayOfMonth(1).dayOfWeek.value + 6) % 7
        val pastMonth = currentMonth.minusMonths(1)
        val nextMonth = currentMonth.plusMonths(1)
        val context = requireContext()

        return (1..42).map {
            DayModel(it.toString())
        }
    }
}