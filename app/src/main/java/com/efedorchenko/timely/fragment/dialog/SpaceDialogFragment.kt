package com.efedorchenko.timely.fragment.dialog

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
import com.efedorchenko.timely.fragment.support.RecyclerItemDecoration
import com.efedorchenko.timely.fragment.support.SpaceAdapter
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.SpaceService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class SpaceDialogFragment : DialogFragment() {

    private var _binding: DialogSpaceShowBinding? = null
    private val binding get() = _binding!!

    private val showMemberFunc = { member: SpaceMember -> showMember(member) }

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    @Inject
    lateinit var spaceService: SpaceService

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

        binding.membersRecyclerView.layoutManager = LinearLayoutManager(context)
        val members = spaceViewModel.members.value
        binding.membersRecyclerView.adapter = SpaceAdapter(members, spaceService, showMemberFunc)

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

        val binding = DialogLoadingBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.memberName.text = member.name

        var getMemberDataJob: Job? = null
        binding.buttonCancel.setOnClickListener {
            getMemberDataJob?.cancel()
            dialog.dismiss()
        }

        getMemberDataJob = viewLifecycleOwner.lifecycleScope.launch {
            val weakFragment = WeakReference(this@SpaceDialogFragment)
            delay(3000)  // TODO: test delay удалить
            if (!dataService.loadData(member.userUuid)) {
                // Нарисовать плашку "вы простматриваете участника такого-то"
            }
            spaceViewModel.switchTo(member)
            dialog.dismiss()
            if (isAdded) {
                weakFragment.get()?.dismiss()
            }
        }
        dialog.show()
    }
}
