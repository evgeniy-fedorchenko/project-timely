package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.databinding.FragmentSummaryCardBinding
import com.efedorchenko.timely.fragment.dialog.AddFineDialog
import com.efedorchenko.timely.fragment.dialog.FinesDialogFragment
import com.efedorchenko.timely.fragment.support.AddFineListener
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import javax.inject.Inject

@AndroidEntryPoint
class SummaryFragment : Fragment(), AddFineListener {

    companion object {
        private const val ADD_FINE_DIALOG_TAG = "add_fine_dialog"
        private const val SHOW_FINES_DIALOG_TAG = "show_fines_dialog"

        fun newInstance(userUuid: String? = null): SummaryFragment {
            return SummaryFragment().apply {
                arguments = Bundle().apply {
                    putString(MainFragment.USER_UUID_ARG, userUuid)
                }
            }
        }
    }

    private var userUuid: String? = null
    private var _binding: FragmentSummaryCardBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userUuid = arguments?.getString(MainFragment.USER_UUID_ARG)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSummaryCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.showFinesButton.setOnClickListener {
            FinesDialogFragment().show(childFragmentManager, SHOW_FINES_DIALOG_TAG)
        }
        if (encProfileStorage.isPrivileged()) {
            val addFineButton = binding.addFineButton
            addFineButton.visibility = View.VISIBLE
            addFineButton.setOnClickListener {
                val monthOffset = viewModel.monthOffset.value?.toLong() ?: 0
                showAddFineDialog(LocalDate.now().plusMonths(monthOffset).month)
            }
        }
        viewModel.memberEvents.observe(viewLifecycleOwner) {
            if (userUuid != null) {
                updateEvents(it)
            }
        }
        viewModel.membersFines.observe(viewLifecycleOwner) {
            if (userUuid != null) {
                // Закоментировано для более легкого тестирования отобрражения чужих штрафов
//                if (encProfileStorage.isPrivileged()) {
                    updateFines(it)
//                } else {
//                    binding.finesCount.visibility = View.INVISIBLE
//                    binding.finesAmount.visibility = View.INVISIBLE
//                    binding.showFinesButton.visibility = View.INVISIBLE
//                    binding.showFinesButton.isEnabled = false
//                }
            }
        }


        viewModel.events.observe(viewLifecycleOwner) {
            if (userUuid == null) {
                updateEvents(it)
            }
        }
        viewModel.fines.observe(viewLifecycleOwner) {
            if (userUuid == null) {
                updateFines(it)
            }
        }
    }

    override fun showAddFineDialog(targetMonth: Month) {
        AddFineDialog.newInstance(this, targetMonth)
            .show(parentFragmentManager, ADD_FINE_DIALOG_TAG)
    }

    override fun onSaveFine(newFine: Fine) {
        viewModel.addNewData(newFine)
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