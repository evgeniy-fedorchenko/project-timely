package com.efedorchenko.timely

import com.efedorchenko.timely.data.Event
import org.threeten.bp.LocalDate

class Helper {

    companion object {

        fun getMonthUID(date: LocalDate): Int = date.year * 100 + date.monthValue

        fun toMap(eventsList: List<Event>): MutableMap<LocalDate, Event> {
            val eventsMap: MutableMap<LocalDate, Event> = HashMap()
            if (eventsList.isEmpty()) {
                return eventsMap
            }
            eventsList.forEach { eventsMap[it.eventDate] = it }
            return eventsMap
        }
    }
}
