package com.efedorchenko.timely.di

import com.efedorchenko.timely.data.MemberRepository
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
object MemberServiceModule {

    @Provides
    @Singleton
    fun provideMemberService(apiService: ApiService, memberRepository: MemberRepository): SpaceService {
        return SpaceServiceImpl(apiService, memberRepository)
    }
}