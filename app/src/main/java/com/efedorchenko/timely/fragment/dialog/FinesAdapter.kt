package com.efedorchenko.timely.fragment.dialog

import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.databinding.DialogFinesShowItemBinding
import com.efedorchenko.timely.model.Fine
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat

class FinesAdapter(
    private val fines: MutableList<Fine>?,
    private val viewModel: DataViewModel,
    private val encProfileStorage: EncProfileStorage
) : RecyclerView.Adapter<FinesAdapter.FineViewHolder>() {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M")
        private val DECIMAL_FORMATTER = DecimalFormat("#,###")
    }

    inner class FineViewHolder(val binding: DialogFinesShowItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FineViewHolder {
        val binding = DialogFinesShowItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FineViewHolder, position: Int) {
        val fine = fines?.getOrNull(position) ?: return

        holder.binding.date.text = fine.date.format(DATE_FORMATTER)
        holder.binding.description.text = fine.description

        val formatted = DECIMAL_FORMATTER.format(fine.amount)
        val formattedFineAmount = "$formatted руб"
        holder.binding.amount.text = formattedFineAmount

        if (encProfileStorage.isPrivileged()) {
            holder.itemView.setOnLongClickListener { view ->
                showDeletePopup(view, position)
                true
            }
        }
    }

    override fun getItemCount(): Int = fines?.size ?: 0

    private fun showDeletePopup(view: View, position: Int) {
        PopupMenu(view.context, view, CENTER, 0, R.style.DeleteFinePopup).apply {
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