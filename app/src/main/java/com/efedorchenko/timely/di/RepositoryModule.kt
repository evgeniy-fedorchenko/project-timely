package com.efedorchenko.timely.di

import android.app.Application
import com.efedorchenko.timely.data.DataRepository
import com.efedorchenko.timely.data.EventRepository
import com.efedorchenko.timely.data.FineRepository
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    fun provideEventRepository(application: Application): DataRepository<Event> {
        return EventRepository(application);
    }

    @Provides
    fun provideFineRepository(application: Application): DataRepository<Fine> {
        return FineRepository(application);
    }

}