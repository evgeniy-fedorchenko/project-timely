package com.efedorchenko.timely.service

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.databinding.CalendarCellBinding
import com.efedorchenko.timely.model.DayModel

class CalendarAdapter(private var days: List<DayModel>) :
    RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    class ViewHolder(val binding: CalendarCellBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dayModel: DayModel) {
            binding.monthDayNumber.text = dayModel.dayOfMonth

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            CalendarCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = 42

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(days[position])
    }

    fun updateItems(newItems: List<DayModel>) {
        days = newItems
        notifyDataSetChanged()
    }
}