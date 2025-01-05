package com.efedorchenko.timely.fragment.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.databinding.DialogSpaceShowItemBinding
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.service.SpaceService

class SpaceAdapter(
    private val members: List<SpaceMember>?,
    private val spaceService: SpaceService,
    private val dismissRequest: () -> Unit
) : RecyclerView.Adapter<SpaceAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(val binding: DialogSpaceShowItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = DialogSpaceShowItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members?.getOrNull(position) ?: return

        holder.binding.name.text = member.name
        val positionFormatted = "Должность: ${member.position}"
        holder.binding.position.text = positionFormatted

        holder.itemView.setOnLongClickListener {
//            longRunningOperation(member)
            dismissRequest.invoke()
            true
        }
    }

    override fun getItemCount(): Int = members?.size ?: 0

    private fun longRunningOperation(member: SpaceMember) {
        spaceService.downloadMember(member)
        /*
        Открыть профиль этого человека:
        - Перезагрузить БД новыми данными
        - Скрыть этот диалог и вернуться на главный экран - там уже будут новые данные (экран получит их из бд)
        - Добавить надпись типа "Выпросматриваете такого-то человека (для админов добавить шестеренку настроек: удалить, поменять данные профиля  итд)
        - Перед вставкой нового ивента проверять а тот ли это юзер (мб сохранять в profileStorage флаг, что загружен текущий юзер или нет)
        */
    }
}
