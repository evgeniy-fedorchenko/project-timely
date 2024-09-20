package com.efedorchenko.timely

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.fragment.app.Fragment

class MonthFragment : Fragment() {

    companion object {
        private const val ARG_MONTH_OFFSET = "month_offset"

        fun newInstance(monthOffset: Int): MonthFragment {
            return MonthFragment().apply {
                arguments = Bundle().apply { putInt(ARG_MONTH_OFFSET, monthOffset) }
            }
        }
    }

    private lateinit var calendarGrid: GridLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_month, container, false)
        calendarGrid = view.findViewById(R.id.calendar_grid)
        return view
    }

}
