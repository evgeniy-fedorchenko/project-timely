package com.efedorchenko.timely.event

import org.threeten.bp.Duration
import org.threeten.bp.LocalDate

data class Event(
    var eventDate: LocalDate?,
    var workDuration: Duration?,
    var comment: String?,
    var color: Color?
)

class EventBuilder {
    private var eventDate: LocalDate? = null
    private var workDuration: Duration? = null
    private var comment: String? = null
    private var color: Color? = null

    fun setEventDate(date: LocalDate) = apply { this.eventDate = date }
    fun setWorkDuration(duration: Duration) = apply { this.workDuration = duration }
    fun setComment(comment: String) = apply { this.comment = comment }
    fun setColor(color: Color) = apply { this.color = color }

    fun build(): Event {
        return Event(eventDate, workDuration, comment, color)
    }
}
