package com.efedorchenko.timely.di

import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.repository.RepositoryFactory
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.DataServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataServiceModule {

    @Provides
    @Singleton
    fun provideDataService(
        apiService: ApiService,
        repositoryFactory: RepositoryFactory,
        encUserProfile: EncUserProfile,
        viewModel: DataViewModel,
    ): DataService {

        return DataServiceImpl(apiService, repositoryFactory, viewModel, encUserProfile)
    }
}
