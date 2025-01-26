package com.efedorchenko.timely.fragment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.efedorchenko.timely.databinding.DialogFineAddBinding
import com.efedorchenko.timely.fragment.support.AddAbstractDataListener
import com.efedorchenko.timely.fragment.support.FragmentUtils
import com.efedorchenko.timely.input.CommentInputFilter
import com.efedorchenko.timely.input.FineAmountFilter
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.service.ToastHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

class AddFineDialog : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(listener: AddAbstractDataListener<Fine>, targetDate: LocalDate): AddFineDialog {
            return AddFineDialog().apply { setContext(listener, YearMonth.from(targetDate)) }
        }
    }

    private var _binding: DialogFineAddBinding? = null
    private val binding get() = _binding!!

    private var targetYearMonth: YearMonth = YearMonth.now()
    private var addFineListener: AddAbstractDataListener<Fine>? = null

    private fun setContext(listener: AddAbstractDataListener<Fine>, targetYearMonth: YearMonth) {
        this.targetYearMonth = targetYearMonth
        this.addFineListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFineAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val monthView = "${FinesDialogFragment.MONTH_NAMES[targetYearMonth.monthValue - 1]} ${targetYearMonth.year}"
        binding.headerTitle.text = String.format(binding.headerTitle.text.toString(), monthView)
        binding.selectedDay.value = if (YearMonth.now() == targetYearMonth) LocalDate.now().dayOfMonth else 1
        binding.selectedDay.maxValue = targetYearMonth.month.length(targetYearMonth.isLeapYear)

        val fineAmountField = binding.fineAmount
        val fineCommentField = binding.fineComment
        val selectedDayField = binding.selectedDay

        fineAmountField.nextFocusDownId = fineCommentField.id
        fineCommentField.filters = arrayOf(CommentInputFilter())
        fineAmountField.filters = arrayOf(FineAmountFilter())
        val context = context

        binding.buttonSave.setOnClickListener {
            val fineAmount = fineAmountField.text.toString().toIntOrNull()
            if (fineAmount == null || fineAmount <= 0) {
                ToastHelper.fineAmountTooSmall(context)
            } else if (fineCommentField.text == null || fineCommentField.text.toString().isEmpty()) {
                ToastHelper.needsFineDesc(context)
            } else {
                val receiptDate = LocalDate.of(targetYearMonth.year, targetYearMonth.month, selectedDayField.value)
                val fine = Fine(
                    date = receiptDate,
                    description = fineCommentField.text.toString(),
                    amount = fineAmount,
                )
                addFineListener?.onSaveData(fine)
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        FragmentUtils.setUpDialogListener(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}