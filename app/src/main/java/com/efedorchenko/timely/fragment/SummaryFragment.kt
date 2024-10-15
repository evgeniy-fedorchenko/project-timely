package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.SummaryCardBinding
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.service.MainViewModel
import com.efedorchenko.timely.service.OnSaveFineListener

class SummaryFragment() : OnSaveFineListener() {

    private lateinit var viewModel: MainViewModel
    private lateinit var securityService: SecurityService

    private var _binding: SummaryCardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SummaryCardBinding.inflate(inflater, container, false)
        val view = binding.root

        val showFinesButton = binding.showFinesButton
        showFinesButton.setOnClickListener {
        securityService = SecurityService.getInstance(requireContext())
            FinesDialogFragment().show(childFragmentManager, "FinesDialog")
        }
        return view
    }

        val addFineButton = binding.addFineButton
        addFineButton.visibility = View.VISIBLE
        addFineButton.setOnClickListener {
            val targetMonth =
                LocalDate.now().plusMonths(viewModel.monthOffset.value?.toLong() ?: 0).month
            OnSaveFineListener.fineDialog(targetMonth, this)
                .show(parentFragmentManager, ADD_FINE_DIALOG_TAG)
        }

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        viewModel.events.observe(viewLifecycleOwner) { updateEvents(it) }
        viewModel.fines.observe(viewLifecycleOwner) { updateFines(it) }
    }

    override fun onSaveFine(newFine: Fine) {
        viewModel.addFine(newFine)
    }


    private fun updateEvents(events: List<Event>?) {
        val daysWorked = events?.count().toString()
        val hoursWorked = events?.map { it.workDuration.toHours() }?.sum().toString()
        binding.daysWorked.text = resources.getString(R.string.days_worked_text, daysWorked)
        binding.hoursWorked.text = resources.getString(R.string.hours_worked_text, hoursWorked)
    }

    private fun updateFines(fines: List<Fine>?) {
        val finesCount = fines?.count().toString()
        val finesAmount = fines?.map { it.amount }?.sum().toString()
        binding.finesCount.text = resources.getString(R.string.fines_count_text, finesCount)
        binding.finesAmount.text = resources.getString(R.string.fines_amount_text, finesAmount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}