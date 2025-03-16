package com.efedorchenko.timely.ui.support

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.efedorchenko.timely.databinding.DialogSpaceShowJoinRequestBinding
import com.efedorchenko.timely.model.member.SpaceMember

class JoinRequestsAdapter(
    initialMembers: List<SpaceMember> = emptyList(),
    private val onMemberAccepted: (SpaceMember, Int) -> Unit,
    private val onMemberRejected: (SpaceMember, Int) -> Unit
) : RecyclerView.Adapter<JoinRequestsAdapter.JoinRequestViewHolder>() {

    private val members = initialMembers.toMutableList()

    inner class JoinRequestViewHolder(val binding: DialogSpaceShowJoinRequestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JoinRequestViewHolder {
        val binding = DialogSpaceShowJoinRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JoinRequestViewHolder(binding)
    }

    override fun getItemCount() = members.size

    override fun onBindViewHolder(holder: JoinRequestViewHolder, position: Int) {
        val member = members[position]

        with(holder.binding) {

            name.text = member.name
            val positionFormatted = "Желаемая должность: ${member.position}"
            userPosition.text = positionFormatted

            val roleFormatted = "Предпочитаемая роль: ${member.role?.title ?: "Unknown"}"
            role.text = roleFormatted

            acceptButton.setOnClickListener { onMemberAccepted.invoke(member, position) }
            rejectButton.setOnClickListener { onMemberRejected.invoke(member, position) }
        }
    }

    fun removeAt(position: Int) {
        if (position in 0 until members.size) {
            members.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addAt(position: Int, member: SpaceMember) {
        if (position in 0..members.size) {
            members.add(position, member)
            notifyItemInserted(position)
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
