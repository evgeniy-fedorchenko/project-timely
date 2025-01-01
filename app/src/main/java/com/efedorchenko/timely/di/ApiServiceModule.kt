package com.efedorchenko.timely.di

import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.ApiServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ApiServiceModule {

    @Provides
    fun provideApiService(encProfileStorage: EncProfileStorage): ApiService {
        return ApiServiceImpl(encProfileStorage);
    }
}