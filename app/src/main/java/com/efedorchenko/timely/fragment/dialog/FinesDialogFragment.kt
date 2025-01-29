package com.efedorchenko.timely.fragment.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorageImpl
import com.efedorchenko.timely.data.ProfileStorageImpl
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.DialogFinesShowBinding
import com.efedorchenko.timely.fragment.MainWorkerFragment
import com.efedorchenko.timely.fragment.support.FinesAdapter
import com.efedorchenko.timely.fragment.support.RecyclerItemDecoration
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.Fine
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class FinesDialogFragment : DialogFragment() {

    companion object {
        val MONTH_NAMES = arrayOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")

        fun newInstance(someParam: String): FinesDialogFragment {
            val fragment = FinesDialogFragment()
            val args = Bundle()
            args.putString(MainWorkerFragment.USER_UUID_ARG, someParam)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: DialogFinesShowBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    @Inject
    lateinit var encProfileStorageImpl: EncProfileStorageImpl

    @Inject
    lateinit var profileStorageImpl: ProfileStorageImpl

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFinesShowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.finesRecyclerView.layoutManager = LinearLayoutManager(context)
        val selectedMember = spaceViewModel.selectedMember.value

        binding.headerUserName.text = selectedMember?.name ?: profileStorageImpl.getName()

        viewModel.monthOffset.value?.let {
            val targetDate = LocalDate.now().plusMonths(it.toLong())
            val monthName = MONTH_NAMES[targetDate.monthValue - 1]
            val headerDateText = "Штрафы за $monthName ${targetDate.year}"
            binding.headerDate.text = headerDateText
        }

        val fines = viewModel.get<Fine>(DataType.FINE, selectedMember?.userUuid)?.toMutableList()
        if (fines.isNullOrEmpty()) {
            binding.emptyFinesText.visibility = View.VISIBLE
            binding.finesRecyclerView.visibility = View.GONE
        } else {
            binding.finesRecyclerView.adapter = FinesAdapter(fines, viewModel, encProfileStorageImpl.isPrivileged())
        }

        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing_horizontal)
        binding.finesRecyclerView.addItemDecoration(RecyclerItemDecoration(spaceInPixels))
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.9).toInt()
            setLayout(width, height)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}