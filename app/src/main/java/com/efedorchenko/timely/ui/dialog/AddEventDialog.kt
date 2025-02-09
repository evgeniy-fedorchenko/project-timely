package com.efedorchenko.timely.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.efedorchenko.timely.databinding.DialogEventAddBinding
import com.efedorchenko.timely.input.AddEventDialogFieldsWatcher
import com.efedorchenko.timely.input.CommentInputFilter
import com.efedorchenko.timely.input.HoursInputFilter
import com.efedorchenko.timely.input.MinutesInputFilter
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.fragment.CalendarFragment
import com.efedorchenko.timely.ui.support.AddAbstractDataListener
import com.efedorchenko.timely.ui.support.setupAsExpandedBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.util.Locale

class AddEventDialog : BottomSheetDialogFragment() {

    companion object {
        private const val DATE_FORMATTER = "dd MMMM yyyy"
        private val MIN_WORK_DURATION = Duration.ofHours(8)

        fun newInstance(listener: AddAbstractDataListener<Event>, existedData: Event?): AddEventDialog {
            return AddEventDialog().apply {
                this.addEventListener = listener
                this.existedData = existedData
            }
        }
    }

    private var _binding: DialogEventAddBinding? = null
    private val binding get() = _binding!!

    private var addEventListener: AddAbstractDataListener<Event>? = null
    private var existedData: Event? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEventAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetDate = LocalDate.parse(arguments?.getString(CalendarFragment.SELECTED_DATE_KEY))
        val formatter = DateTimeFormatter.ofPattern(DATE_FORMATTER, Locale("ru"))
        with(binding) {
            textViewSelectedDate.text = targetDate.format(formatter)

            existedData?.let {
                editTextHours.setText(String.format(it.workDuration.toHours().toString()))
                editTextMinutes.setText(String.format(it.workDuration.toMinutesPart().toString()))
                editTextComment.setText(it.comment)
            }

            val hoursEditText = editTextHours
            val minutesEditText = editTextMinutes
            val commentEditText = editTextComment

            if (arguments?.getBoolean(CalendarFragment.NEEDS_BLOCK_INPUT) == true) {
                hoursEditText.isEnabled = false
                minutesEditText.isEnabled = false
                commentEditText.isEnabled = false
            }
            hoursEditText.nextFocusDownId = minutesEditText.id
            minutesEditText.nextFocusDownId = commentEditText.id
            hoursEditText.addTextChangedListener(AddEventDialogFieldsWatcher(2, minutesEditText))
            minutesEditText.addTextChangedListener(AddEventDialogFieldsWatcher(2, commentEditText))

            hoursEditText.filters = arrayOf(HoursInputFilter())
            minutesEditText.filters = arrayOf(MinutesInputFilter())
            commentEditText.filters = arrayOf(CommentInputFilter())

            buttonSave.setOnClickListener {
                val newHours = hoursEditText.text.toString().toLongOrNull() ?: 0
                val newMinutes = minutesEditText.text.toString().toLongOrNull() ?: 0
                val newComment = commentEditText.text.toString()
                val newWorkDuration = Duration.of(newHours * 60 + newMinutes, ChronoUnit.MINUTES)

                if (newWorkDuration < MIN_WORK_DURATION) {
                    ToastHelper.workDurationTooShort(requireContext(), MIN_WORK_DURATION)
                } else {
                    if (existedData == null
                        || existedData?.workDuration != newWorkDuration
                        || existedData?.comment != newComment
                    ) {
                        val appId = existedData?.appId
                        val event = Event(
                            appId = appId,
                            date = targetDate,
                            workDuration = newWorkDuration,
                            comment = newComment
                        )
                        addEventListener?.onSaveData(event)
                    }
                    dismiss()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.setupAsExpandedBottomSheet()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
