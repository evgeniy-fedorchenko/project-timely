package com.efedorchenko.timely.di

import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.data.repository.MemberRepository
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.SpaceService
import com.efedorchenko.timely.service.SpaceServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpaceServiceModule {

    @Provides
    @Singleton
    fun provideSpaceService(
        apiService: ApiService,
        memberRepository: MemberRepository,
        encUserProfile: EncUserProfile,
        userProfile: UserProfile,
        spaceViewModel: SpaceViewModel
    ): SpaceService {

        return SpaceServiceImpl(apiService, memberRepository, encUserProfile, userProfile, spaceViewModel)
    }
}
