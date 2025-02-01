package com.efedorchenko.timely.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.efedorchenko.timely.databinding.DialogEventAddBinding
import com.efedorchenko.timely.ui.fragment.CalendarFragment
import com.efedorchenko.timely.ui.support.FragmentUtils
import com.efedorchenko.timely.input.AddEventDialogFieldsWatcher
import com.efedorchenko.timely.input.CommentInputFilter
import com.efedorchenko.timely.input.HoursInputFilter
import com.efedorchenko.timely.input.MinutesInputFilter
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.service.ToastHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.util.Locale

class AddEventDialog : BottomSheetDialogFragment() {

    companion object {
        private val MIN_WORK_DURATION = Duration.ofHours(8)
        const val SELECTED_DATE_KEY = "selected_date"

        fun newInstance(listener: AddAbstractDataListener<Event>): AddEventDialog {
            return AddEventDialog().apply { setListener(listener) }
        }
    }

    private var _binding: DialogEventAddBinding? = null
    private val binding get() = _binding!!

    private var addEventListener: AddAbstractDataListener<Event>? = null

    private fun setListener(listener: AddAbstractDataListener<Event>) {
        this.addEventListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEventAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetDate = LocalDate.parse(arguments?.getString(SELECTED_DATE_KEY))
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

            if (workDuration < MIN_WORK_DURATION) {
                ToastHelper.workDurationTooShort(requireContext(), MIN_WORK_DURATION)
            } else {
                val event = Event(
                    date = targetDate,
                    workDuration = workDuration,
                    comment = comment
                )
                addEventListener?.onSaveData(event)
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
