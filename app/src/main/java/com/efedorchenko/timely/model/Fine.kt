package com.efedorchenko.timely.model

import com.efedorchenko.timely.model.serializer.InstantSerializer
import com.efedorchenko.timely.model.serializer.LocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

@Serializable
@SerialName("fine")
data class Fine(

    @Transient
    override var appId: Long? = null,

    override var backendId: Long? = null,

    @Serializable(with = LocalDateSerializer::class)
    override val date: LocalDate,

    @Serializable(with = InstantSerializer::class)
    override val deletedAt: Instant? = null,

    @Serializable(with = InstantSerializer::class)
    override val changedAt: Instant? = null,

    val description: String,

    val amount: Int,
) : AbstractData() {

    override fun getType(): DataType {
        return DataType.FINE
    }

    override fun logicEquals(other: Any): Boolean {
        if (this == other) return true
        return when (other) {
            is Event -> false
            is Fine -> {
                this.date == other.date
                        && this.description == other.description
                        && this.amount == other.amount
                        && this.appId == other.appId   // тк может сущетсвовать два одинаковых Fine -> сравниваем appId
            }

            else -> return false
        }
    }
}
