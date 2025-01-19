package com.efedorchenko.timely.di

import android.app.Application
import com.efedorchenko.timely.data.MemberRepository
import com.efedorchenko.timely.data.RepositoryFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMemberRepository(application: Application): MemberRepository {
        return MemberRepository(application)
    }

    @Provides
    @Singleton
    fun provideRepositoryFactory(application: Application): RepositoryFactory {
        return RepositoryFactory(application)
    }
}