package com.efedorchenko.timely.ui.support

import android.content.Context
import com.efedorchenko.timely.model.AbstractData
import org.threeten.bp.LocalDate

interface AddAbstractDataListener<T : AbstractData> {

    fun showAddDataDialog(targetDate: LocalDate, context: Context?, existedData: T?)

    fun onSaveData(data: T)

}