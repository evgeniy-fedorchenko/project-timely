package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.SummaryCardBinding

class SummaryFragment : Fragment() {

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
        updateSummaryData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateSummaryData() {
        val daysWorked = getDaysWorked()
        val hoursWorked = getHoursWorked()
        val finesCount = getFinesCount()
        val finesAmount = getFinesAmount()

        binding.daysWorked.text = resources.getString(R.string.days_worked_text, daysWorked)
        binding.hoursWorked.text = resources.getString(R.string.hours_worked_text, hoursWorked)
        binding.finesCount.text = resources.getString(R.string.fines_count_text, finesCount)
        binding.finesAmount.text = resources.getString(R.string.fines_amount_text, finesAmount)
    }

    private fun getDaysWorked(): String {
        return "5"
    }

    private fun getHoursWorked(): String {
        return "40"
    }

    private fun getFinesCount(): String {
        return "2"
    }

    private fun getFinesAmount(): String {
        return "100"
    }
}