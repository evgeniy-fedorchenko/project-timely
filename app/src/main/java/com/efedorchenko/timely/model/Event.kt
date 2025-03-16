package com.efedorchenko.timely.model

import com.efedorchenko.timely.model.serializer.CustomDurationSerializer
import com.efedorchenko.timely.model.serializer.InstantSerializer
import com.efedorchenko.timely.model.serializer.LocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

@Serializable
@SerialName("event")
data class Event(

    @Transient
    override var appId: Long? = null,

    override var backendId: Long? = null,

    @Serializable(with = LocalDateSerializer::class)
    override val date: LocalDate,

    // Не хранится, при получении поля - запись удаляется из местной БД
    @Serializable(with = InstantSerializer::class)
    override val deletedAt: Instant? = null,

    @Serializable(with = InstantSerializer::class)
    override val changedAt: Instant? = null,

    override var owner: String? = null,

    @Serializable(with = CustomDurationSerializer::class)
    val workDuration: Duration,

    var comment: String? = null,

    var status: EventStatus? = null

) : AbstractData() {

    override fun getType(): DataType {
        return DataType.EVENT
    }

    override fun logicEquals(other: Any): Boolean {
        if (this == other) return true
        return when (other) {
            is Fine -> false
            is Event -> {
                this.date == other.date
                        && this.workDuration == other.workDuration
                        && this.comment == other.comment
            }

            else -> return false
        }
    }
}
