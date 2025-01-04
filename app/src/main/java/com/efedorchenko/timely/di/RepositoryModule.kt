package com.efedorchenko.timely.di

import android.app.Application
import com.efedorchenko.timely.data.DataRepository
import com.efedorchenko.timely.data.EventRepository
import com.efedorchenko.timely.data.FineRepository
import com.efedorchenko.timely.data.RepositoryFactory
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
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
    fun provideEventRepository(application: Application): DataRepository<Event> {
        return EventRepository(application);
    }

    @Provides
    @Singleton
    fun provideFineRepository(application: Application): DataRepository<Fine> {
        return FineRepository(application);
    }

    @Provides
    @Singleton
    fun provideRepositoryFactory(
        eventRepository: DataRepository<Event>, fineRepository: DataRepository<Fine>
    ): RepositoryFactory {

        return RepositoryFactory(eventRepository, fineRepository)
    }
}