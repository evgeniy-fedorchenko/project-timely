package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.efedorchenko.timely.databinding.AddEventBinding
import com.efedorchenko.timely.filter.CommentInputFilter
import com.efedorchenko.timely.filter.HoursInputFilter
import com.efedorchenko.timely.filter.MinutesInputFilter
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.service.OnSaveEventListener
import com.efedorchenko.timely.service.ToastHelper
import com.google.android.material.R.id.design_bottom_sheet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.util.Locale

class AddEventDialog : BottomSheetDialogFragment() {

    companion object {
        private val MIN_WORK_DURATION = Duration.ofHours(8)

        fun newInstance(listener: OnSaveEventListener, processedCellIdx: Int): AddEventDialog {
            return AddEventDialog().apply { setContext(listener, processedCellIdx) }
        }
    }

    private var _binding: AddEventBinding? = null
    private val binding get() = _binding!!

    private var processedCellIdx: Int? = null
    private var onSaveEventListener: OnSaveEventListener? = null

    private fun setContext(listener: OnSaveEventListener, processedCellIdx: Int) {
        this.onSaveEventListener = listener
        this.processedCellIdx = processedCellIdx

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        _binding = AddEventBinding.inflate(inflater, container, false)

        val targetDate = LocalDate.parse(arguments?.getString(OnSaveEventListener.SELECTED_DATE_KEY))
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))
        binding.textViewSelectedDate.text = targetDate.format(formatter)

        val hoursEditText = binding.editTextHours
        val minutesEditText = binding.editTextMinutes
        val commentEditText = binding.editTextComment

        hoursEditText.nextFocusDownId = minutesEditText.id
        minutesEditText.nextFocusDownId = commentEditText.id
        hoursEditText.addTextChangedListener(AddEventDialogFieldsWatcher(2, minutesEditText))
        minutesEditText.addTextChangedListener(AddEventDialogFieldsWatcher(2, commentEditText))

        hoursEditText.filters = arrayOf(HoursInputFilter())
        minutesEditText.filters = arrayOf(MinutesInputFilter())
        commentEditText.filters = arrayOf(CommentInputFilter())

        binding.buttonSave.setOnClickListener {
            val hours = hoursEditText.text.toString().toLongOrNull() ?: 0
            val minutes = minutesEditText.text.toString().toLongOrNull() ?: 0
            val comment = commentEditText.text.toString()
            val workDuration = Duration.of(hours * 60 + minutes, ChronoUnit.MINUTES)

            if (workDuration.compareTo(MIN_WORK_DURATION) < 0) {
                ToastHelper.showToast("Минимальная длина: 8 часов", requireContext())
            } else {
                val event = Event(targetDate, workDuration, comment)
                onSaveEventListener?.onSaveEvent(event, processedCellIdx)
                dismiss()
            }
        }
        return binding.root
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

private class AddEventDialogFieldsWatcher(
    private val requestNextAfterSymbols: Int,
    private val nextField: EditText
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

    override fun afterTextChanged(s: Editable?) {
        if (s?.length == requestNextAfterSymbols) {
            nextField.requestFocus()
        }
    }

}