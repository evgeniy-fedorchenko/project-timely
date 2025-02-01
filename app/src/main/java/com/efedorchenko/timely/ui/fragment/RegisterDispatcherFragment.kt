package com.efedorchenko.timely.ui.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.DialogHelpChooseRoleBinding
import com.efedorchenko.timely.databinding.FragmentRegisterDispatcherBinding
import com.efedorchenko.timely.ui.fragment.AbstractRegisterFragment.Companion.HELP_DIALOG_WIDTH_RATIO
import com.efedorchenko.timely.model.auth.RoleType

class RegisterDispatcherFragment : Fragment() {

    private var _binding: FragmentRegisterDispatcherBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterDispatcherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        binding.buttonWorkerWithSpace.setOnClickListener {
            val action = RegisterDispatcherFragmentDirections
                .dispatchToRegisterFragment(RoleType.WORKER.name)
            navController.navigate(action)
        }

        binding.buttonWorkerWithoutSpace.setOnClickListener {
            val action = RegisterDispatcherFragmentDirections
                .dispatchToRegisterFragment(RoleType.WORKER.name, false)
            navController.navigate(action)
        }

        binding.buttonBoss.setOnClickListener {
            val action = RegisterDispatcherFragmentDirections
                .dispatchToRegisterFragment(RoleType.BOSS.name)
            navController.navigate(action)
        }

        binding.buttonCreator.setOnClickListener {
            navController.navigate(R.id.registerCreatorFragment)
        }

        binding.backToLoginTextView.setOnClickListener {
            navController.navigate(R.id.authFragment)
        }

        val context = requireContext()
        binding.chooseRoleHelpButton.setOnClickListener {
            val binding = DialogHelpChooseRoleBinding.inflate(LayoutInflater.from(context))
            val dialog = AlertDialog.Builder(context).setView(binding.root).create()

            dialog?.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * HELP_DIALOG_WIDTH_RATIO).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            dialog.show()

            binding.closeButton.setOnClickListener {
                dialog.cancel()
            }
        }
    }
}
