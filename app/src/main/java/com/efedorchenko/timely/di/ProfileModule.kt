package com.efedorchenko.timely.di

import android.content.Context
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.EncProfileStorageImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    @Provides
    fun provideEncProfileStorage(@ApplicationContext context: Context): EncProfileStorage {
        return EncProfileStorageImpl.getInstance(context)
    }
}