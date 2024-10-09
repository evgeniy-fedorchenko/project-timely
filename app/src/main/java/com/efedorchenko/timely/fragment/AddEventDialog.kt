package com.efedorchenko.timely.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.efedorchenko.timely.R
import com.efedorchenko.timely.filter.CommentInputFilter
import com.efedorchenko.timely.filter.HoursInputFilter
import com.efedorchenko.timely.filter.MinutesInputFilter
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.service.CalendarHelper
import com.efedorchenko.timely.service.OnSaveEventListener
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
            return AddEventDialog().apply { setValues(listener, processedCellIdx) }
        }
    }

    private var processedCellIdx: Int? = null
    private var onSaveEventListener: OnSaveEventListener? = null

    private fun setValues(listener: OnSaveEventListener, processedCellIdx: Int) {
        this.onSaveEventListener = listener
        this.processedCellIdx = processedCellIdx

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fill_day, container, false)
        val selectedDateView = view.findViewById<TextView>(R.id.text_view_selected_date)

        val targetDate = LocalDate.parse(arguments?.getString(OnSaveEventListener.SELECTED_DATE_KEY))
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))
        selectedDateView.text = targetDate.format(formatter)

        val hoursEditText = view.findViewById<EditText>(R.id.edit_text_hours)
        val minutesEditText = view.findViewById<EditText>(R.id.edit_text_minutes)
        val commentEditText = view.findViewById<EditText>(R.id.edit_text_comment)
        val saveButton = view.findViewById<Button>(R.id.button_save)

        hoursEditText.nextFocusDownId = minutesEditText.id
        minutesEditText.nextFocusDownId = commentEditText.id
        hoursEditText.addTextChangedListener(AddEventDialogFieldsWatcher(2, minutesEditText))
        minutesEditText.addTextChangedListener(AddEventDialogFieldsWatcher(2, commentEditText))

        hoursEditText.filters = arrayOf(HoursInputFilter())
        minutesEditText.filters = arrayOf(MinutesInputFilter())
        commentEditText.filters = arrayOf(CommentInputFilter())

        saveButton.setOnClickListener {
            val hours = hoursEditText.text.toString().toLongOrNull() ?: 0
            val minutes = minutesEditText.text.toString().toLongOrNull() ?: 0
            val comment = commentEditText.text.toString()
            val workDuration = Duration.of(hours * 60 + minutes, ChronoUnit.MINUTES)

            if (workDuration.compareTo(MIN_WORK_DURATION) < 0) {
                CalendarHelper.showToast("Минимальная длина: 8 часов", requireContext())
            } else {
                val event = Event(targetDate, workDuration, comment)
                onSaveEventListener?.onSaveEvent(event, processedCellIdx)
                dismiss()
            }
        }
        return view
    }

}

private class AddEventDialogFieldsWatcher(
    private val requestNextAfterSymbols: Int,
    private val nextField: EditText
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        if (s?.length == requestNextAfterSymbols) {
            nextField.requestFocus()
        }
    }

}