package com.efedorchenko.timely.service

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.R
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.security.SecurityServiceImpl
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat

class FinesAdapter(
    private val fines: List<Fine>?,
    private val viewModel: MainViewModel
) : RecyclerView.Adapter<FinesAdapter.ViewHolder>() {

    private lateinit var securityService: SecurityService

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
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.fine_item, parent, false)
        securityService = SecurityServiceImpl.getInstance(context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fine = fines?.get(position)
        holder.date.text = fine?.receiptDate?.format(DATE_FORMATTER)
        holder.description.text = fine?.description
        val formatted = DECIMAL_FORMATTER.format(fine?.amount)
        val formattedFineAmount = "$formatted руб"
        holder.amount.text = formattedFineAmount

        if (securityService.isPrivileged()) {
            holder.itemView.setOnLongClickListener { view ->
                showDeletePopup(view, position)
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return fines?.size ?: 0
    }

    private fun showDeletePopup(view: View, position: Int) {
        PopupMenu(view.context, view, Gravity.CENTER, 0, R.style.DeleteFinePopup).apply {
            menu.add("Удалить")
            setOnMenuItemClickListener { item ->
                if (item.title == "Удалить") {
//                    viewModel.removeFineAt(position)
                    notifyItemRemoved(position)
                }
                true
            }
            show()
        }
    }

}