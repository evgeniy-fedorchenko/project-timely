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
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.databinding.DialogLoadingBinding
import com.efedorchenko.timely.databinding.DialogSpaceShowBinding
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.member.AcceptMemberResultType
import com.efedorchenko.timely.model.member.SpaceMember
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.dialog.SpaceDialogFragment.Companion.State.JOIN_REQUESTS
import com.efedorchenko.timely.ui.dialog.SpaceDialogFragment.Companion.State.MEMBERS
import com.efedorchenko.timely.ui.dialog.SpaceDialogFragment.Companion.State.valueOf
import com.efedorchenko.timely.ui.support.JoinRequestsAdapter
import com.efedorchenko.timely.ui.support.MembersAdapter
import com.efedorchenko.timely.ui.support.RecyclerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class SpaceDialogFragment : DialogFragment() {

    companion object {

        enum class State {
            MEMBERS, JOIN_REQUESTS
        }

        private const val STATE_ARG = "state_arg"

        fun newInstance(state: State): SpaceDialogFragment {
            return SpaceDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(STATE_ARG, state.toString())
                }
            }
        }
    }

    private var _binding: DialogSpaceShowBinding? = null
    private val binding get() = _binding!!

    private val showMemberFunc =   { member: SpaceMember -> showMember(member) }
    private val acceptMemberFunc = { member: SpaceMember, position: Int -> acceptMember(member, position) }
    private val rejectMemberFunc = { member: SpaceMember, position: Int -> rejectMember(member, position) }

    private val membersAdapter = MembersAdapter(showMemberFunc)
    private lateinit var joinRequestsAdapter: JoinRequestsAdapter

    @Inject
    lateinit var spaceViewModel: SpaceViewModel

    @Inject
    lateinit var dataService: DataService

    @Inject
    lateinit var spaceService: SpaceService

    @Inject
    lateinit var userProfile: UserProfile

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSpaceShowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val joinRequests = spaceViewModel.getJoinRequests()
        joinRequestsAdapter = JoinRequestsAdapter(joinRequests, acceptMemberFunc, rejectMemberFunc)

        userProfile.getSpaceName().let {
            val spaceRawText = getString(R.string.nav_menu_header_space, it)
            val spannablePositionText = SpannableString(spaceRawText)
            spannablePositionText.setSpan(
                StyleSpan(Typeface.BOLD), 10,
                spaceRawText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.spaceName.text = spannablePositionText
        }

        setupSubHeader(binding)
        setupRecycler()

//        JOIN_REQUESTS не имеют своей viewModel
        if (getState() == MEMBERS) {
            viewLifecycleOwner.lifecycleScope.launch {
                spaceViewModel.members.collect { membersAdapter.submitList(it) }
            }
        }
    }

    private fun setupSubHeader(binding: DialogSpaceShowBinding) {
        when (getState()) {
            MEMBERS -> binding.subheader.text = "Выберете участника для просмотра"
            JOIN_REQUESTS -> binding.subheader.text = "Заявки в компанию"
            null -> {}
        }
    }

    private fun setupRecycler() {
        binding.membersRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.membersRecyclerView.adapter = when (getState()) {
            MEMBERS -> membersAdapter
            JOIN_REQUESTS -> joinRequestsAdapter
            null -> null
        }

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

    private fun acceptMember(member: SpaceMember, position: Int) {
        val context = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            joinRequestsAdapter.removeAt(position)
            val acceptRemoteResult = spaceService.acceptRemote(member.userUuid, member.role ?: RoleType.WORKER)
            if (acceptRemoteResult == AcceptMemberResultType.SUCCESS) {
                spaceViewModel.acceptMember(member)
            } else {
                joinRequestsAdapter.addAt(position, member)
                ToastHelper.acceptMemberFiled(context, acceptRemoteResult)
            }
        }
    }

    private fun rejectMember(member: SpaceMember, position: Int) {
        val context = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            joinRequestsAdapter.removeAt(position)
            if (spaceService.rejectRemote(member.userUuid)) {
                spaceViewModel.removeMember(member)
            } else {
                ToastHelper.rejectMemberFiled(context)
                joinRequestsAdapter.addAt(position, member)
            }
        }
    }

    private fun getState() = arguments?.getString(STATE_ARG)?.let { valueOf(it) }
}
