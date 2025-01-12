package com.efedorchenko.timely.fragment.support

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.databinding.DialogFinesShowItemBinding
import com.efedorchenko.timely.model.Fine
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat
import java.util.Locale

class FinesAdapter(
    private val viewModel: DataViewModel,
    private val isUserPrivileged: Boolean
) : RecyclerView.Adapter<FinesAdapter.FineViewHolder>() {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
        private val DECIMAL_FORMATTER = DecimalFormat("#,###")
    }

    private val fines: MutableList<Fine>? = viewModel.fines.value?.toMutableList()

    inner class FineViewHolder(val binding: DialogFinesShowItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FineViewHolder {
        val binding = DialogFinesShowItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FineViewHolder, position: Int) {
        val fine = fines?.getOrNull(position) ?: return

        holder.binding.date.text = fine.date.format(DATE_FORMATTER).lowercase()
        holder.binding.description.text = fine.description

        val formatted = DECIMAL_FORMATTER.format(fine.amount)
        val formattedFineAmount = "$formatted руб"
        holder.binding.amount.text = formattedFineAmount

        if (isUserPrivileged) {
            holder.itemView.setOnLongClickListener { view ->
                showDeletePopup(view, position)
                true
            }
        }
    }

    override fun getItemCount(): Int = fines?.size ?: 0

    private fun showDeletePopup(view: View, position: Int) {
        PopupMenu(view.context, view, CENTER, 0, R.style.delete_fine_popup).apply {
            val deleteText = SpannableString("Удалить")
            deleteText.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(view.context, R.color.light_gray)), 0,
                deleteText.length, 0
            )
            menu.add(deleteText)
            setOnMenuItemClickListener { item ->
                if (item.title.toString() == "Удалить") {
                    // TODO: прикрутить асинх удаление на беке
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