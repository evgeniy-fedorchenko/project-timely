package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.efedorchenko.timely.databinding.AddFineBinding
import com.efedorchenko.timely.filter.CommentInputFilter
import com.efedorchenko.timely.filter.FineAmountFilter
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.service.CalendarHelper
import com.efedorchenko.timely.service.OnSaveFineListener
import com.google.android.material.R.id.design_bottom_sheet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.Year

class AddFineDialog : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(listener: SummaryFragment, targetMonth: Month): AddFineDialog {
            return AddFineDialog().apply { setContext(listener, targetMonth) }
        }
    }

    private var _binding: AddFineBinding? = null
    private val binding get() = _binding!!

    private var targetMonth: Month? = null
    private var onSaveFineListener: OnSaveFineListener? = null

    private fun setContext(listener: SummaryFragment, targetMonth: Month) {
        this.targetMonth = targetMonth
        this.onSaveFineListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        _binding = AddFineBinding.inflate(inflater, container, false)
        val view = binding.root

        val fineAmountField = binding.fineAmount
        val fineCommentField = binding.fineComment
        val selectedDayField = binding.selectedDay

        fineAmountField.nextFocusDownId = fineCommentField.id
        fineCommentField.filters = arrayOf(CommentInputFilter())
        fineAmountField.filters = arrayOf(FineAmountFilter())

        binding.buttonSave.setOnClickListener {
            val fineAmount = fineAmountField.text.toString().toIntOrNull()
            if (fineAmount == null || fineAmount <= 0) {
                CalendarHelper.showToast("Слишком маленькая сумма", requireContext())

//                Поставить вотчер на сумму чтоб не ввели больше 2 * 10^10
//                И вотчер на коммент для ограничения по длине в 100 символов

            } else {
                val receiptDate = LocalDate.of(Year.now().value, targetMonth, selectedDayField.value)
                val fine = Fine(receiptDate, fineCommentField.text.toString(), fineAmount)
                onSaveFineListener?.onSaveFine(fine)
                dismiss()
            }
        }
        return view
    }

    //    Поднятие диалога над клавиатурой
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setOnShowListener {
            val bottomSheet = dialog?.findViewById<View>(design_bottom_sheet)
            bottomSheet?.let { bs ->
                BottomSheetBehavior.from(bs).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }
}