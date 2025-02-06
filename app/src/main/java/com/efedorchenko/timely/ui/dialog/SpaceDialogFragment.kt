package com.efedorchenko.timely.ui.dialog

import android.app.AlertDialog
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
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.DialogLoadingBinding
import com.efedorchenko.timely.databinding.DialogSpaceShowBinding
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.support.RecyclerItemDecoration
import com.efedorchenko.timely.ui.support.SpaceAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class SpaceDialogFragment : DialogFragment() {

    private var _binding: DialogSpaceShowBinding? = null
    private val binding get() = _binding!!

    private val spaceAdapter = SpaceAdapter({ member: SpaceMember -> showMember(member) })

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    @Inject
    lateinit var dataService: DataService

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

        setupRecycler()
        viewLifecycleOwner.lifecycleScope.launch {
            spaceViewModel.members.collect { members ->
                spaceAdapter.submitList(members)
            }
        }
    }

    private fun setupRecycler() {
        binding.membersRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.membersRecyclerView.adapter = spaceAdapter

        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing_horizontal)
        binding.membersRecyclerView.addItemDecoration(RecyclerItemDecoration(spaceInPixels))
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

    private fun showMember(member: SpaceMember) {
        val context = context ?: return

        val loadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        val loadingDialog = AlertDialog.Builder(context).setView(loadingBinding.root).create()
        loadingDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingBinding.memberName.text = member.name
        loadingDialog.show()

        var getMemberDataJob: Job? = null
        loadingBinding.buttonCancel.setOnClickListener {
            getMemberDataJob?.cancel()
            loadingDialog.dismiss()
        }

        getMemberDataJob = viewLifecycleOwner.lifecycleScope.launch {
            val weakFragment = WeakReference(this@SpaceDialogFragment)
            if (!dataService.loadData(member.userUuid)) {
                ToastHelper.errorGetMember(context)
                loadingDialog.dismiss()
                return@launch
            }

            spaceViewModel.switchTo(member)
            loadingDialog.dismiss()
            if (isAdded) {
                weakFragment.get()?.dismiss()
            }
        }
    }
}
