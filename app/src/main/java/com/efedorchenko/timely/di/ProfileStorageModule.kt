package com.efedorchenko.timely.di

import android.content.Context
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.EncProfileStorageImpl
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.data.ProfileStorageImpl
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
    fun provideEncProfileStorage(@ApplicationContext context: Context): EncProfileStorage {
        return EncProfileStorageImpl(context)
    }

    @Provides
    @Singleton
    fun provideProfileStorage(@ApplicationContext context: Context): ProfileStorage {
        return ProfileStorageImpl(context)
    }
}