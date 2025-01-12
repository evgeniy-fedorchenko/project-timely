package com.efedorchenko.timely.fragment.support

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
        Перед началом:
        - Нужно иметь отдельные таблицы по типу events и fines, но для команды (там плюсом колонка user_id)
        - Нужна кнопка обновления (бд пункт navMenu), по нажатию идем на сервер и берем актуальные данные и сразу рисуем из на календаре
        - Штрафы так же обновляются по этой кнопке

        При нажатии на юзера:
        - Показываем окошко загрузки с кнопкой отмены
        - Проверить локально в новых таблицах данные этого юзера, если их нет, то сходить на сервер за данными (смены и штрафы) и разместить их в бд
        - Убираем окно загрузки и отрисовываем календарь с данными выбранного юзера
            - мб сделать отдельную viewModel для данных других юзеров и при перескоке переходить
              через findNavController() с передачей флага, что используем другого юзера и id этого юзера тоже передавать.
              Переходить на MainFragment, а не CalendarFragment, чтобы Summary тоже была в коляске
            - SummaryFragment может получать флаг юзера через конструктор, а CalendarFragment нет, тк он инстанцируется через ViewPager
              Calendar должен получить значение либо через Bundle, как он получает monthOffset либо еще как-то



        - На пустом месте разместить имя просматриваемого юзера и шестеренку для настроек юзера (актуально для начальников)
        - В боковом меню сделать кнопку "домой", которая ведет на экран календаря, но уже со своими данными
            - собираем из бд (из своих таблиц свои данные и перерисовываем календарь)
            - мб стоит делать это через findNavController() и передавать аргумент user_id, если это наш юзер - просто берем данные из своей бд

        */
    }
}
