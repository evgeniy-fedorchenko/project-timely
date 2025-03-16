package com.efedorchenko.timely.di

import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
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
        encUserProfile: EncUserProfile,
        userProfile: UserProfile,
        apiService: ApiService,
        spaceViewModel: SpaceViewModel
    ): AuthService {
        return AuthServiceImpl(encUserProfile, userProfile, apiService, spaceViewModel)
    }
}