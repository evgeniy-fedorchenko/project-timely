package com.efedorchenko.timely.di

import android.app.Application
import com.efedorchenko.timely.data.DataRepository
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.MemberRepository
import com.efedorchenko.timely.data.RepositoryFactory
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
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
        eventRepository: DataRepository<Event>,
        fineRepository: DataRepository<Fine>,
        repositoryFactory: RepositoryFactory,
        memberRepository: MemberRepository,
        apiService: ApiService
    ): DataViewModel {

        return DataViewModel(
            application,
            eventRepository,
            fineRepository,
            repositoryFactory,
            memberRepository,
            apiService
        )
    }
}
