package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorageImpl
import com.efedorchenko.timely.databinding.DialogFinesShowBinding
import com.efedorchenko.timely.service.FinesAdapter
import com.efedorchenko.timely.service.SpaceItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FinesDialogFragment : DialogFragment() {

    private var _binding: DialogFinesShowBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var encProfileStorageImpl: EncProfileStorageImpl

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
        val fines = viewModel.fines.value
        binding.finesRecyclerView.adapter = FinesAdapter(fines?.toMutableList(), viewModel, encProfileStorageImpl)

        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing_horizontal)
        binding.finesRecyclerView.addItemDecoration(SpaceItemDecoration(spaceInPixels))
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(R.drawable.dialog_background)
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