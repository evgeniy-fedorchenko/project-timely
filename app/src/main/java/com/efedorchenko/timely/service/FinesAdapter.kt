package com.efedorchenko.timely.service

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.R
import com.efedorchenko.timely.databinding.FineItemBinding
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.security.SecurityService
import com.efedorchenko.timely.security.SecurityServiceImpl
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat

class FinesAdapter(
    private val fines: MutableList<Fine>?,
    private val viewModel: MainViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var _binding: FineItemBinding? = null
    private val binding get() = _binding!!

    private val securityService: SecurityService = SecurityServiceImpl.requireInstance()

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M")
        private val DECIMAL_FORMATTER = DecimalFormat("#,###")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        _binding = FineItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return object : RecyclerView.ViewHolder(binding.root) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val fine = fines?.getOrNull(position) ?: return

        binding.date.text = fine.receiptDate.format(DATE_FORMATTER)
        binding.description.text = fine.description

        val formatted = DECIMAL_FORMATTER.format(fine.amount)
        val formattedFineAmount = "$formatted руб"
        binding.amount.text = formattedFineAmount

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
                    viewModel.delete(position)
                    fines?.removeAt(position)
                    notifyItemRemoved(position)
                }
                true
            }
            show()
        }
    }

}