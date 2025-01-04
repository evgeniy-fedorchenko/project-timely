package com.efedorchenko.timely.model

import com.efedorchenko.timely.model.serializer.LocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate

@Serializable
@SerialName("fine")
data class Fine(

    override var appId: Long? = null,

    override var backendId: Long? = null,

    @Serializable(with = LocalDateSerializer::class)
    override val date: LocalDate,

    val description: String,

    val amount: Int,
) : AbstractData() {

    override fun getType(): DataType {
        return DataType.FINE
    }
}
