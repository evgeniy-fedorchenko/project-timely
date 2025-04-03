package com.efedorchenko.timely.ui.support

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.databinding.DialogSpaceShowMemberBinding
import com.efedorchenko.timely.model.member.SpaceMember

class MembersAdapter(
    private val showMemberFunc: (member: SpaceMember) -> Unit,
    private val serviceMemberFunc: ((member: SpaceMember) -> Unit)? = null,
    private val withRoles: Boolean = false
) : ListAdapter<SpaceMember, MembersAdapter.MemberViewHolder>(MemberDiffCallback()) {

    inner class MemberViewHolder(val binding: DialogSpaceShowMemberBinding) : RecyclerView.ViewHolder(binding.root)

    class MemberDiffCallback : DiffUtil.ItemCallback<SpaceMember>() {
        override fun areItemsTheSame(oldItem: SpaceMember, newItem: SpaceMember): Boolean {
            return oldItem.userUuid == newItem.userUuid
        }

        override fun areContentsTheSame(oldItem: SpaceMember, newItem: SpaceMember): Boolean {
            return oldItem.changedAt == newItem.changedAt
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = DialogSpaceShowMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = getItem(position)

        with(holder.binding) {
            name.text = member.name
            userPosition.text = "Должность: ${member.position}"
            if (withRoles) userRole.text = "Роль в компании: ${member.role}"
            
            holder.itemView.setOnLongClickListener {
                showMemberFunc.invoke(member)
                true
            }

            serviceMemberFunc?.let {
                icon.visibility = View.VISIBLE
                icon.animClickListener { it.invoke(member) }
            }
        }
    }
}

/*
Перед началом:
- Нужно иметь отдельные таблицы по типу events и fines, но для команды (там плюсом колонка user_id)
- Нужна кнопка обновления (бд пункт navMenu), по нажатию идем на сервер и берем актуальные данные и сразу рисуем из на календаре
- Штрафы так же обновляются по этой кнопке

При нажатии на юзера:

1. Сходить в БД - найти данные выбранного юзера
   - Если этого юзера вообще нету в БД - показать индикатор загрузки и кнопку отмены и сходить за данными на бек (синхронно)
   - Если данные есть в БД - сходить на бек асинхронно и подгрузить новые данные - если бек недоступен, то показать тост
   - Если данные есть (с бд или с бека) - перерисовать календарь и сохранить данные в БД
   - Если данные есть в бд, то при получении данных с бека снова перерисовать календарь (и сохранить данные)
   - Кнопка "обновить данные" будет обновлять данные выбранного юзера, а не свои

      через findNavController() или через эмит с передачей флага, что используем другого юзера и id этого юзера тоже передавать.
      Переходить на MainFragment, а не CalendarFragment, чтобы Summary тоже была в коляске
    - SummaryFragment может получать флаг юзера через конструктор, а CalendarFragment нет, тк он инстанцируется через ViewPager
      Calendar должен получить значение либо через Bundle, как он получает monthOffset либо еще как-то

- На пустом месте разместить имя просматриваемого юзера и шестеренку для настроек юзера (актуально для начальников)
- В боковом меню сделать кнопку "домой", которая ведет на экран календаря, но уже со своими данными
    - собираем из бд (из своих таблиц свои данные и перерисовываем календарь)
    - мб стоит делать это через findNavController() и передавать аргумент user_id, если это наш юзер - просто берем данные из своей бд

*/
