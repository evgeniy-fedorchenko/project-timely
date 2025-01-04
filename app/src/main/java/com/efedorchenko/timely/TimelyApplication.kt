package com.efedorchenko.timely

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TimelyApplication : Application(), ViewModelStoreOwner {

    override val viewModelStore = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }

}

// TODO: Придумать, что будет видеть юзер, пока его заявку не приняли админы
// TODO: Добавить вариант регистрации "одиночка". Может быть держать юзера в режиме одиночки пока его не приняли в пространство (пока админы не одобрили)
// TODO: очишать репозитории и viewModel при exit(), удалять ивенты и штрафы из бд при исключении юзера из пространства?
// TODO: выводить имя просматриваемого человека, для админа добавить кнопку настроек: там опции: задать зп, изменить роль, удалить из команды. При старте приложения у участника проверять его на беке, что его еще не удалили
// TODO: Добавить админу кнопку "прогулял смену" на ячейку простматриваемого участника
// TODO: отправлять новый ивент и штраф на бек при сохранении его. Если не получилось отправить - показывать алерт и выводить кнопку синхронизации (отправки)
// TODO: Добавить вкладку "Заявки на вступление в команду"