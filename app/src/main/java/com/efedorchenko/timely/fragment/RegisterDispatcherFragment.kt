package com.efedorchenko.timely.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.DialogHelpChooseRoleBinding
import com.efedorchenko.timely.databinding.FragmentRegisterDispatcherBinding

class RegisterDispatcherFragment : Fragment() {

    private var _binding: FragmentRegisterDispatcherBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRegisterDispatcherBinding.inflate(inflater, container, false)
        val view = binding.root

        val context = requireContext()
        val navController = findNavController()
        binding.buttonWorker.setOnClickListener {
            navController.navigate(R.id.registerWorkerFragment)
        }

        binding.buttonBoss.setOnClickListener {
            navController.navigate(R.id.registerBossFragment)
        }

        binding.buttonCreator.setOnClickListener {
            navController.navigate(R.id.registerCreatorFragment)
        }

        binding.backToLoginTextView.setOnClickListener {
            navController.navigate(R.id.authFragment)
        }

        binding.chooseRoleHelpButton.setOnClickListener {
            val binding = DialogHelpChooseRoleBinding.inflate(LayoutInflater.from(context))
            val dialog = AlertDialog.Builder(context).setView(binding.root).create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.show()

            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            binding.closeButton.setOnClickListener {
                dialog.cancel()
            }
        }

        return view
    }
}
