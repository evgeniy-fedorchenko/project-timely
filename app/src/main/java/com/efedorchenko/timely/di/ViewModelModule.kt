package com.efedorchenko.timely.di

import android.app.Application
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.repository.MemberRepository
import com.efedorchenko.timely.data.repository.RepositoryFactory
import com.efedorchenko.timely.service.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Provides
    @Singleton
    fun provideDataViewModel(
        application: Application,
        repositoryFactory: RepositoryFactory,
        apiService: ApiService
    ): DataViewModel {
        return DataViewModel(application, repositoryFactory, apiService)
    }

    @Provides
    @Singleton
    fun provideSpaceViewModel(application: Application, memberRepository: MemberRepository): SpaceViewModel {
        return SpaceViewModel(application, memberRepository)
    }
}
