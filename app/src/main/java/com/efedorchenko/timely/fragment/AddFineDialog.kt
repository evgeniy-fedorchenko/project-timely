package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.efedorchenko.timely.databinding.DialogFineAddBinding
import com.efedorchenko.timely.fragment.support.AddFineListener
import com.efedorchenko.timely.input.CommentInputFilter
import com.efedorchenko.timely.input.FineAmountFilter
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.service.ToastHelper
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

    private var _binding: DialogFineAddBinding? = null
    private val binding get() = _binding!!

    private var targetMonth: Month? = null
    private var addFineListener: AddFineListener? = null

    private fun setContext(listener: SummaryFragment, targetMonth: Month) {
        this.targetMonth = targetMonth
        this.addFineListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = DialogFineAddBinding.inflate(inflater, container, false)
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
                ToastHelper.fineAmountTooSmall(requireContext())

            } else {
                val receiptDate = LocalDate.of(Year.now().value, targetMonth, selectedDayField.value)
                val fine = Fine(
                    date = receiptDate,
                    description = fineCommentField.text.toString(),
                    amount = fineAmount,
                )
                addFineListener?.onSaveFine(fine)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}