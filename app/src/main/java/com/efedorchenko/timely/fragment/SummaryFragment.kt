package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.Event
import com.efedorchenko.timely.data.Fine
import com.efedorchenko.timely.data.MonthUID
import com.efedorchenko.timely.databinding.SummaryCardBinding
import com.efedorchenko.timely.repository.EventRepository
import com.efedorchenko.timely.repository.FineRepository
import com.efedorchenko.timely.service.MainViewModel
import org.threeten.bp.LocalDate

class SummaryFragment() : Fragment() {


    private lateinit var eventRepository: EventRepository
    private lateinit var  fineRepository: FineRepository
    private lateinit var viewModel: MainViewModel


    private var _binding: SummaryCardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SummaryCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        fineRepository = FineRepository(context)
        eventRepository = EventRepository(context)

        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        viewModel.events.observe(viewLifecycleOwner) { events ->
            updateSummary(events)
        }
    }

    private fun updateSummary(events: List<Event>?): List<Event>? {
        val daysWorked = events?.count().toString()
        val hoursWorked = events?.map { it.workDuration.toHours() }?.sum().toString()
        binding.daysWorked.text = resources.getString(R.string.days_worked_text, daysWorked)
        binding.hoursWorked.text = resources.getString(R.string.hours_worked_text, hoursWorked)
        return events
    }

    private fun updateSummary1(fines: List<Fine>?): List<Fine>? {
        val finesCount = fines?.count().toString()
        val finesAmount = fines?.map { it.amount }?.sum().toString()
        binding.finesCount.text = resources.getString(R.string.fines_count_text, finesCount)
        binding.finesAmount.text = resources.getString(R.string.fines_amount_text, finesAmount)
        return fines
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateSummaryData(monthOffset: Int) {

        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))

        val monthFines = fineRepository.findByMonth(monthUID)
        val monthEvents = eventRepository.findByMonth(monthUID, false)

        val daysWorked = monthEvents.count().toString()
        val hoursWorked = monthEvents.map { it.workDuration.toHours() }.sum().toString()
        val finesCount = monthFines.count().toString()
        val finesAmount = monthFines.map { it.amount }.sum().toString()

        binding.daysWorked.text = resources.getString(R.string.days_worked_text, daysWorked)
        binding.hoursWorked.text = resources.getString(R.string.hours_worked_text, hoursWorked)
        binding.finesCount.text = resources.getString(R.string.fines_count_text, finesCount)
        binding.finesAmount.text = resources.getString(R.string.fines_amount_text, finesAmount)
    }
}