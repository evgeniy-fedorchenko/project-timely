package com.efedorchenko.timely.di

import android.content.Context
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.EncUserProfileImpl
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.data.UserProfileImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileStorageModule {

    @Provides
    @Singleton
    fun provideEncProfileStorage(@ApplicationContext context: Context): EncUserProfile {
        return EncUserProfileImpl(context)
    }

    @Provides
    @Singleton
    fun provideProfileStorage(@ApplicationContext context: Context): UserProfile {
        return UserProfileImpl(context)
    }
}