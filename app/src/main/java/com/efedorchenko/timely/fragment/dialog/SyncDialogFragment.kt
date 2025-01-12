package com.efedorchenko.timely.fragment.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.databinding.DialogSyncingDataBinding
import com.efedorchenko.timely.fragment.support.DoSyncButtonListener
import com.efedorchenko.timely.service.SpaceService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SyncDialogFragment : DialogFragment() {

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var profileStorage: ProfileStorage

    @Inject
    lateinit var encProfileStorage: EncProfileStorage

    private var _binding: DialogSyncingDataBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSyncingDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    /*
    * Что происходит по нажатию:
    * ✓ Отправляются неотправленные СМЕНЫ (если есть)
    * ✓ Отправляются неотправленные ШТРАФЫ (если есть)
    *
    * x Получение неизвестных СМЕН (по дельте по backend_id или timestamp создания)
    * x Получение неизвестных ШТРАФОВ (по дельте по backend_id или timestamp создания)
    *
    * x Получение новых участников компании
    * x Получение измененных штрафов и смен
    * x Получение удаленных штрафов и смен
    *
    * - */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var eventsOutOfSync = viewModel.getNotSyncedEvents()
        var finesOutOfSync = viewModel.getNotSyncedFine()

        with(binding) {
            if ((eventsOutOfSync.size + finesOutOfSync.size) > 0) {
                uploadDescription.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE
                dataLabel.visibility = View.VISIBLE

                if (eventsOutOfSync.isNotEmpty()) {
                    eventsCount.visibility = View.VISIBLE
                    eventsCount.text = getString(R.string.found_not_synced_events, eventsOutOfSync.size)
                }
                if (finesOutOfSync.isNotEmpty()) {
                    finesCount.visibility = View.VISIBLE
                    finesCount.text = getString(R.string.found_not_synced_fines, finesOutOfSync.size)
                }
            }
        }
        binding.doSyncButton.setOnClickListener(DoSyncButtonListener(this, binding, viewModel))
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onDestroyView() {
        // Закрыть соединение с сервером или продолжить синхрониться в корутине?
        super.onDestroyView()
        _binding = null
    }
}