package com.efedorchenko.timely.service

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.R
import com.efedorchenko.timely.model.Fine
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat

class FinesAdapter(private val fines: List<Fine>?) :
    RecyclerView.Adapter<FinesAdapter.ViewHolder>() {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M")
        private val DECIMAL_FORMATTER = DecimalFormat("#,###")
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.date)
        val description: TextView = view.findViewById(R.id.description)
        val amount: TextView = view.findViewById(R.id.amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fine_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fine = fines?.get(position)
        holder.date.text = fine?.receiptDate?.format(DATE_FORMATTER)
        holder.description.text = fine?.description
        val formatted = DECIMAL_FORMATTER.format(fine?.amount)
        val formattedFineAmount = "$formatted руб"
        holder.amount.text = formattedFineAmount
    }

    override fun getItemCount(): Int {
        return fines?.size ?: 0
    }
}