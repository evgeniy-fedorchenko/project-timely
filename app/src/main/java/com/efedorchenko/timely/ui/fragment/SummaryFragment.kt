package com.efedorchenko.timely.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.FragmentSummaryCardBinding
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.ui.dialog.AddFineDialog
import com.efedorchenko.timely.ui.dialog.FinesDialogFragment
import com.efedorchenko.timely.ui.support.AddAbstractDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class SummaryFragment : Fragment(), AddAbstractDataListener<Fine> {

    companion object {
        private const val ADD_FINE_DIALOG_TAG = "add_fine_dialog"
        private const val SHOW_FINES_DIALOG_TAG = "show_fines_dialog"
    }

    private var userUuid: String? = null
    private var _binding: FragmentSummaryCardBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    @Inject
    lateinit var dataService: DataService

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userUuid = spaceViewModel.selectedMember.value?.userUuid
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
                showAddDataDialog(LocalDate.now().plusMonths(monthOffset), context, null)
            }
        }

        updateEvents(viewModel.get(DataType.EVENT, userUuid))
        updateFines(viewModel.get(DataType.FINE, userUuid))
        setupDataObservers()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun showAddDataDialog(targetDate: LocalDate, context: Context?, existedData: Fine?) {
        AddFineDialog.newInstance(this, targetDate)
            .show(parentFragmentManager, ADD_FINE_DIALOG_TAG)
    }

    override fun onSaveData(data: Fine) {
        lifecycleScope.launch {
            dataService.saveData(data, userUuid)
            updateFines(viewModel.get(DataType.FINE, userUuid))
        }
    }

    private fun setupDataObservers() {
        viewModel.memberEvents.observe(viewLifecycleOwner) {
            if (userUuid != null) {
                updateEvents(it)
            }
        }
        viewModel.membersFines.observe(viewLifecycleOwner) {
            if (userUuid != null) {
                if (encProfileStorage.isPrivileged()) {
                updateFines(it)
                } else {
                    binding.finesCount.visibility = View.INVISIBLE
                    binding.finesAmount.visibility = View.INVISIBLE
                    binding.showFinesButton.visibility = View.INVISIBLE
                    binding.showFinesButton.isEnabled = false
                }
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

    private fun updateEvents(events: List<Event>?) {
        val daysWorked = (events?.count() ?: 0).toString()
        val hoursWorked = (events?.sumOf { it.workDuration.toHours() } ?: 0).toString()
        binding.daysWorked.text = resources.getString(R.string.days_worked_text, daysWorked)
        binding.hoursWorked.text = resources.getString(R.string.hours_worked_text, hoursWorked)
    }

    private fun updateFines(fines: List<Fine>?) {
        val finesCount = (fines?.count() ?: 0).toString()
        val finesAmount = (fines?.sumOf { it.amount } ?: 0).toString()
        binding.finesCount.text = resources.getString(R.string.fines_count_text, finesCount)
        binding.finesAmount.text = resources.getString(R.string.fines_amount_text, finesAmount)
    }
}
