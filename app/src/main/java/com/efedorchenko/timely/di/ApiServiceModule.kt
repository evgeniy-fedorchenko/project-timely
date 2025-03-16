package com.efedorchenko.timely.di

import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.ApiServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideApiService(encUserProfile: EncUserProfile): ApiService {
        return ApiServiceImpl(encUserProfile);
    }
}