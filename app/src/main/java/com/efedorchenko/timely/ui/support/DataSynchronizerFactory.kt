package com.efedorchenko.timely.ui.support

import androidx.fragment.app.DialogFragment
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.databinding.DialogSyncingDataBinding
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.SpaceService
import javax.inject.Inject

class DataSynchronizerFactory @Inject constructor(
    private val spaceService: SpaceService,
    private val dataService: DataService,
    private val userProfile: UserProfile,
    private val encUserProfile: EncUserProfile,
    private val viewModel: DataViewModel,
    private val spaceViewModel: SpaceViewModel
) {
    fun create(parent: DialogFragment, binding: DialogSyncingDataBinding): DataSynchronizer {
        return DataSynchronizer(
            parent = parent,
            spaceService = spaceService,
            dataService = dataService,
            parentBinding = binding,
            userProfile = userProfile,
            encUserProfile = encUserProfile,
            viewModel = viewModel,
            spaceViewModel = spaceViewModel
        )
    }

    fun create(): DataSynchronizer {
        return DataSynchronizer(
            spaceService = spaceService,
            dataService = dataService,
            userProfile = userProfile,
            encUserProfile = encUserProfile,
            viewModel = viewModel,
            spaceViewModel = spaceViewModel
        )
    }
}