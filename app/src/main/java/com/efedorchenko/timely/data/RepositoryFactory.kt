package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import javax.inject.Inject

class RepositoryFactory @Inject constructor(
    private val eventRepository: DataRepository<Event>,
    private val fineRepository: DataRepository<Fine>
) {

    @Suppress("unchecked_cast")
    fun <T : AbstractData> getRepository(data: T): DataRepository<T> = when (data.getType()) {
        DataType.EVENT -> eventRepository as DataRepository<T>
        DataType.FINE -> fineRepository as DataRepository<T>
    }
}
