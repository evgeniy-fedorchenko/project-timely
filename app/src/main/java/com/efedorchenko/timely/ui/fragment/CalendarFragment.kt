package com.efedorchenko.timely.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.efedorchenko.timely.R
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.databinding.CalendarGridLayoutBinding
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.SaveResult
import com.efedorchenko.timely.model.calendar.CalendarBuilder
import com.efedorchenko.timely.model.calendar.CalendarCell
import com.efedorchenko.timely.model.calendar.applyTo
import com.efedorchenko.timely.model.calendar.calcCellIdx
import com.efedorchenko.timely.model.calendar.deleteFrom
import com.efedorchenko.timely.service.DataService
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.dialog.AddEventDialog
import com.efedorchenko.timely.ui.support.AddAbstractDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

// TODO 15.03.2025 20:16: вынести листенер в отдельный класс
@AndroidEntryPoint
class CalendarFragment : Fragment(), AddAbstractDataListener<Event> {

    companion object {
        private const val MONTH_OFFSET_ARG = "month_offset"
        private const val ADD_EVENT_DIALOG_TAG = "add_event_dialog"

        val YEAR_MONTH_FORMATTER = SimpleDateFormat("LLLL yyyy", Locale("ru"))
//        private val YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ru"))
//        private val SIMPLE_YEAR_MONTH_FORMATTER = SimpleDateFormat("LLLL yyyy", Locale("ru"))


        fun newInstance(monthOffset: Int, userUuid: String?): CalendarFragment {
            return CalendarFragment().apply {
                arguments = Bundle().apply {
                    putInt(MONTH_OFFSET_ARG, monthOffset)
                    putString(AbstractMainFragment.USER_UUID_ARG, userUuid)
                }
            }
        }
    }

    private var _binding: CalendarGridLayoutBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModel: DataViewModel

    @Inject
    lateinit var encUserProfile: EncUserProfile

    @Inject
    lateinit var dataService: DataService

    private var monthOffset: Int = 0
    private lateinit var monthEventsDef: Deferred<Map<LocalDate, Event>>

    private lateinit var calendarGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        monthOffset = arguments?.getInt(MONTH_OFFSET_ARG) ?: 0
        monthEventsDef = viewModel.getEventsAsyncStart(monthOffset, getUserUuidArgument())
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CalendarGridLayoutBinding.inflate(inflater, container, false)
        calendarGrid = binding.calendarGrid
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateCalendar()
        viewModel.monthOffset.observe(viewLifecycleOwner) { updateMonthTextView(it) }

        lifecycleScope.launch {
            viewModel.needUpdateData.collect { needsUpdate ->
                if (needsUpdate) {
                    monthEventsDef = viewModel.getEventsAsyncStart(monthOffset, getUserUuidArgument())
                    updateCalendar()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Установка новых смен (клик на пустую ячейку):
     * - Работник может ставить смену себе, если дата еще не прошла. Если прошла - показывается тост `datePassed`
     * - Работник не может ставить смены другим работникам. При попытке - игнор, меню не открывается
     * - Админ не может ставить смены себе, так как не имеет календаря в принципе
     * - Админ может ставить смену любому работнику, если дата еще не прошла. Если прошла -  тост `datePassed`
     *
     * Редактирование и удаление смен (клик на запоненную ячейку):
     * - Работник не может изменять свои смены. При открытии смены - доступно только чтение (не зависимо от даты)
     * - Работник не может изменять чужие смены. При открытии смены - доступно только чтение (не зависимо от даты)
     * - Админ может редактировать (и удалять) любые смены работников, не зависимо от даты
     *
     * Просмотр:
     * - Работникам доступен просмотр любых своих и чужих смен, не зависимо от даты
     * - Админу доступно редактирование (и удаление) чужих смен не зависимо от даты.
     *   Но установка НОВЫХ смен - только если дата еще не прошла
     */
    override fun showAddDataDialog(targetDate: LocalDate, context: Context?, existedData: Event?) {
        if (context == null) return

        val isAdmin = encUserProfile.isPrivileged()
        val isGuest = getUserUuidArgument() != null
        val isAfter = LocalDate.now().isAfter(targetDate)
        val dataPresent = existedData != null

        if (!dataPresent) {
            if (isGuest && !isAdmin) {
                return
            }
            if ((!isGuest && isAfter) || (isGuest && isAfter)) {
                ToastHelper.datePassed(context)
                return
            }
        }

        val readOnly = (dataPresent && !isAdmin)
        AddEventDialog.newInstance(this, existedData, readOnly, targetDate)
            .show(parentFragmentManager, ADD_EVENT_DIALOG_TAG)
    }

    /**
     * Для сохранении новых данных или изменения сущетсвующих.
     * Обновляется содержимое ячейки в UI, после чего в корутине данные сохраняются в БД и на сервер
     * - Если отправка на сервер не удалась - показывается тост `ToastHelper.NOT_SYNCED` (есть возможность
     *   отправить позже через [com.efedorchenko.timely.ui.dialog.SyncDialogFragment])
     * - Если удалась - есть несколько вариантов:
     *     - Если отправлял работник, то сервер мог изменить данные, если админ ранее назначил на эту же дату
     *       другую смену - смена с сервера имеет приоритет, снова обновляется UI и данные в локальной БД
     *     - Если смену отправлял админ - она перепишет смену на сервере, если роль этого админа старше роли
     *       юзера, который установил смену изначально, иначе местная смена обновится.
     *       При паритете веса ролей - приоритет у более старой смены
     *     - При изменении сущетсвующей смены логика такая же как и с добавлением новой смены админом
     */
    override fun onSaveData(data: Event) {
        updateCell(data)
        lifecycleScope.launch {
            when (val result = dataService.saveData(data, getUserUuidArgument())) {
                is SaveResult.ServerChanged -> updateCell(result.newData as Event)
                is SaveResult.Error -> cleanCell(data)
                is SaveResult.Success -> {}
                is SaveResult.SyncFiled -> viewModel.emitNotSynced.invoke()
            }
        }
    }

    /**
     * Очистить клетку на UI от метки смены
     */
    private fun cleanCell(event: Event) {
        val cellIdx = event.calcCellIdx()
        val targetCell = calendarGrid.getChildAt(cellIdx) as? ConstraintLayout
        targetCell?.let { event.deleteFrom(targetCell, cellIdx) }
    }

    /**
     * Нарисовать новую или перерисовать старую метку смены на UI
     */
    private fun updateCell(event: Event?) {
        if (event == null) return
        val cellIdx = event.calcCellIdx()
        val targetCell = calendarGrid.getChildAt(cellIdx) as? ConstraintLayout
        targetCell?.let { event.applyTo(targetCell, cellIdx, true) }
    }

    /**
     * Нарисовать ячейки календаря заново, стерев предыдущие.
     * Расположение рассчитывается на основе [monthOffset]. Данные смен этого месяца,
     * должны загружаться через [monthEventsDef].
     * Метод будет ожидать их 1 секунду, после чего начнет рисовать календарь без них
     */
    private fun updateCalendar() {
        if (getUserUuidArgument() == null && encUserProfile.isPrivileged()) return
        val context = context ?: return
        calendarGrid.removeAllViews()
        val cellBuilder = CalendarBuilder(monthOffset).clickListener(this).eventsDef(monthEventsDef)

        for (i in 0 until 6 * 7) {
            val cell = cellBuilder.buildForIndex(i)

            val textView = createTextView(context, cell)
            val parentLayout = createConstraintLayout(context, cell)
            cell.event?.applyTo(parentLayout, i, false)

            parentLayout.addView(textView)
            calendarGrid.addView(parentLayout)
        }
    }

    private fun updateMonthTextView(monthOffset: Int) {
        activity?.findViewById<TextView>(R.id.center_header)?.let { view ->
            val calendar = Calendar.getInstance(Locale("ru"))
            calendar.add(Calendar.MONTH, monthOffset)
            view.text = YEAR_MONTH_FORMATTER.format(calendar.time).replaceFirstChar { it.uppercase() }
        }
    }

    private fun createTextView(context: Context, cell: CalendarCell): TextView {
        val textView = TextView(context)
        val topPadding = resources.getDimensionPixelSize(R.dimen.calendar_date_padding_top)
        val rightPadding = resources.getDimensionPixelSize(R.dimen.calendar_date_padding_end)
        textView.setPadding(0, topPadding, rightPadding, 0)

        val layoutParams = ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        textView.layoutParams = layoutParams

        textView.text = cell.text
        TextViewCompat.setTextAppearance(textView, cell.textStyle)
        return textView
    }

    // TODO: посмотреть, может быстрее создать схему и инфлейтить ее
    private fun createConstraintLayout(context: Context, cell: CalendarCell): ConstraintLayout {
        val constraintLayout = ConstraintLayout(context)
        val params = GridLayout.LayoutParams()

        params.width = 0
        params.height = 0
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        constraintLayout.layoutParams = params

        cell.onClickListener?.let { constraintLayout.setOnClickListener(it) }
        constraintLayout.background = ContextCompat.getDrawable(context, cell.background)
        return constraintLayout
    }

    private fun getUserUuidArgument() = arguments?.getString(AbstractMainFragment.USER_UUID_ARG)
}
