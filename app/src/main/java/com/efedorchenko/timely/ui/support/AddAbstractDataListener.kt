package com.efedorchenko.timely.ui.support

import com.efedorchenko.timely.model.AbstractData
import org.threeten.bp.LocalDate

interface AddAbstractDataListener<T : AbstractData> {

    fun showAddDataDialog(targetDate: LocalDate)

    fun onSaveData(data: T)

}