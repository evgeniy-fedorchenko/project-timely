package com.efedorchenko.timely.di

import android.app.Application
import com.efedorchenko.timely.repository.EventRepository
import com.efedorchenko.timely.repository.FineRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    fun provideEventRepository(application: Application): EventRepository {
        return EventRepository(application);
    }

    @Provides
    fun provideFineRepository(application: Application): FineRepository {
        return FineRepository(application);
    }

}