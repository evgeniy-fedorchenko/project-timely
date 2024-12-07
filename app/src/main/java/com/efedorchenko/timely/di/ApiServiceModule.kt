package com.efedorchenko.timely.di

import com.efedorchenko.timely.security.SecurityService
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
    fun provideApiService(securityService: SecurityService): ApiService {
        return ApiServiceImpl(securityService);
    }

}