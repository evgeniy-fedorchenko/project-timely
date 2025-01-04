package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.databinding.FragmentSummaryCardBinding
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.service.OnSaveFineListener
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class SummaryFragment : OnSaveFineListener() {

    private var _binding: FragmentSummaryCardBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryCardBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.showFinesButton.setOnClickListener {
            FinesDialogFragment().show(childFragmentManager, "FinesDialog")
        }

        if (!encProfileStorage.isPrivileged()) {
            return view
        }

        val addFineButton = binding.addFineButton
        addFineButton.visibility = View.VISIBLE
        addFineButton.setOnClickListener {
            val monthOffset = viewModel.monthOffset.value?.toLong() ?: 0
            val targetDate = LocalDate.now().plusMonths(monthOffset)
            val targetMonth = targetDate.month
            fineDialog(targetMonth, this).show(parentFragmentManager, ADD_FINE_DIALOG_TAG)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.events.observe(viewLifecycleOwner) { updateEvents(it) }
        viewModel.fines.observe(viewLifecycleOwner) { updateFines(it) }
    }

    override fun onSaveFine(newFine: Fine) {
        viewModel.addData(newFine)
    }

    private fun updateEvents(events: List<Event>?) {
        val daysWorked = events?.count().toString()
        val hoursWorked = events?.sumOf { it.workDuration.toHours() }.toString()
        binding.daysWorked.text = resources.getString(R.string.days_worked_text, daysWorked)
        binding.hoursWorked.text = resources.getString(R.string.hours_worked_text, hoursWorked)
    }

    private fun updateFines(fines: List<Fine>?) {
        val finesCount = fines?.count().toString()
        val finesAmount = fines?.sumOf { it.amount }.toString()
        binding.finesCount.text = resources.getString(R.string.fines_count_text, finesCount)
        binding.finesAmount.text = resources.getString(R.string.fines_amount_text, finesAmount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}