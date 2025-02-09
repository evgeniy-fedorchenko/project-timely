package com.efedorchenko.timely.di

import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.AuthService
import com.efedorchenko.timely.service.AuthServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthServiceModule {

    @Provides
    @Singleton
    fun provideAuthService(
        encProfileStorage: EncProfileStorage, profileStorage: ProfileStorage, apiService: ApiService
    ): AuthService {
        return AuthServiceImpl(encProfileStorage, profileStorage, apiService)
    }
}