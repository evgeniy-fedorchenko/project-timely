package com.efedorchenko.timely.ui.fragment

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.databinding.DialogAccessKeysBinding
import com.efedorchenko.timely.databinding.DialogLoadingBinding
import com.efedorchenko.timely.databinding.FragmentMainBossBinding
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.support.FragmentUtils
import com.efedorchenko.timely.ui.support.RecyclerItemDecoration
import com.efedorchenko.timely.ui.support.SpaceAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainBossFragment : AbstractMainFragment() {

    private var _binding: FragmentMainBossBinding? = null
    private val binding get() = _binding!!

    private val showMemberFunc = { member: SpaceMember -> showMember(member) }
    private val serviceMemberFunc = { member: SpaceMember -> serviceMember(member) }

    @Inject
    override lateinit var viewModel: DataViewModel

    @Inject
    override lateinit var spaceViewModel: SpaceViewModel

    @Inject
    override lateinit var encProfileStorage: EncProfileStorage

    @Inject
    override lateinit var profileStorage: ProfileStorage

    @Inject
    lateinit var dataService: DataService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBossBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        super.setupSideMenu(context)   // Side navigation menu

        binding.headerSpaceName.text = profileStorage.getSpaceName()
        binding.headerLayout.centerHeader.text = getString(R.string.boss_panel_header)
        binding.keysButton.setOnClickListener { showAccessKeysDialog(context) }

        setupRecycler(context)   // Recycler of members list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getDrawerLayout() = binding.mainContent

    override fun getHeaderLayout() = binding.headerLayout

    override fun getNavigationView() = binding.navView

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
            if (!dataService.loadData(member.userUuid)) {
                ToastHelper.errorGetMember(context)
                loadingDialog.dismiss()
                return@launch
            }

            findNavController().navigate(R.id.mainWorkerFragment)
            loadingDialog.dismiss()
            spaceViewModel.switchTo(member)
        }
    }

    private fun setupRecycler(context: Context) {
        binding.membersRecyclerView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 16f * resources.displayMetrics.density)
            }
        }
        binding.membersRecyclerView.clipToOutline = true

        binding.membersRecyclerView.layoutManager = LinearLayoutManager(context)
        val members = spaceViewModel.members.value
        binding.membersRecyclerView.adapter = SpaceAdapter(members, showMemberFunc, serviceMemberFunc)
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing_horizontal)
        binding.membersRecyclerView.addItemDecoration(RecyclerItemDecoration(spaceInPixels))
    }

    private fun serviceMember(member: SpaceMember) {
        ToastHelper.message(member.userUuid, context)
    }

    private fun showAccessKeysDialog(context: Context) {
        val binding = DialogAccessKeysBinding.inflate(from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val keys = encProfileStorage.getSpaceKeys()
        binding.workerKey.text = keys?.workerKey
        binding.bossKey.text = keys?.bossKey

        FragmentUtils.setupButtonAnimationAndClick(binding.workerKeyCopyButton, context, {
            copyToClipboard(context, "worker_key", binding.workerKey.text.toString())
        })
        FragmentUtils.setupButtonAnimationAndClick(binding.bossKeyCopyButton, context, {
            copyToClipboard(context, "boss_key", binding.bossKey.text.toString())
        })
        dialog.show()
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        ToastHelper.keyCopied(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
