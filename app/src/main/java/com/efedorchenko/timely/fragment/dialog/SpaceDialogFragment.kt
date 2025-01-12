package com.efedorchenko.timely.fragment.dialog

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.databinding.DialogSpaceShowBinding
import com.efedorchenko.timely.fragment.support.SpaceAdapter
import com.efedorchenko.timely.fragment.support.SpaceItemDecoration
import com.efedorchenko.timely.service.SpaceService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SpaceDialogFragment : DialogFragment() {

    private var _binding: DialogSpaceShowBinding? = null
    private val binding get() = _binding!!

    private val dismissRequest = { func() }

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var spaceService: SpaceService

    @Inject
    lateinit var profileStorage: ProfileStorage

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSpaceShowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileStorage.getSpaceName().let {
            val spaceRawText = getString(R.string.nav_menu_header_space, it)
            val spannablePositionText = SpannableString(spaceRawText)
            spannablePositionText.setSpan(
                StyleSpan(Typeface.BOLD), 10,
                spaceRawText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.spaceName.text = spannablePositionText
        }

        binding.membersRecyclerView.layoutManager = LinearLayoutManager(context)
        val members = viewModel.members.value
        binding.membersRecyclerView.adapter = SpaceAdapter(members, spaceService, dismissRequest)

        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing_horizontal)
        binding.membersRecyclerView.addItemDecoration(SpaceItemDecoration(spaceInPixels))
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

    fun func() {
        lifecycleScope.launch {
            try {
                // Показываем индикатор
                withContext(Dispatchers.Main) {
                    binding.loadingContainer.visibility = View.VISIBLE
                }

                // Имитация длительной операции
                withContext(Dispatchers.IO) {
                    delay(5000)
                }

            } finally {
                // Скрываем индикатор
                withContext(Dispatchers.Main) {
                    binding.loadingContainer.visibility = View.GONE
                }
            }
        }
    }

}