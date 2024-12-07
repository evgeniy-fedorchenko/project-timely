package com.efedorchenko.timely.di

import android.content.Context
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.security.SecurityServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    fun provideSecurityService(@ApplicationContext context: Context): SecurityService {
        return SecurityServiceImpl.getInstance(context)
    }

}