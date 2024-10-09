package com.efedorchenko.timely.service

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.R
import com.efedorchenko.timely.model.Fine

class FinesAdapter(private val fines: List<Fine>?) :
    RecyclerView.Adapter<FinesAdapter.ViewHolder>() {

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
        holder.date.text = fine?.receiptDate.toString()
        holder.description.text = fine?.description
        holder.amount.text = fine?.amount.toString()
    }

    override fun getItemCount(): Int {
        return fines?.size ?: 0
    }
}