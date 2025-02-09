package com.efedorchenko.timely

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@HiltAndroidApp
class TimelyApplication : Application(), ViewModelStoreOwner {

    private val coroutineExHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("TimelyApplication", "Coroutine error: ", throwable)
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + coroutineExHandler)
    override val viewModelStore = ViewModelStore()

    companion object {
        lateinit var instance: TimelyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }

}

// TODO: Придумать, что будет видеть юзер, пока его заявку не приняли админы
// TODO: Добавить вариант регистрации "одиночка". Может быть держать юзера в режиме одиночки пока его не приняли в пространство (пока админы не одобрили)
// TODO: очишать репозитории и viewModel при exit(), удалять ивенты и штрафы из бд при исключении юзера из пространства?
// TODO: выводить имя просматриваемого человека, для админа добавить кнопку настроек: там опции: задать зп, изменить роль, удалить из команды. При старте приложения у участника проверять его на беке, что его еще не удалили
// TODO: Добавить админу кнопку "прогулял смену" на ячейку простматриваемого участника
// TODO: отправлять новый ивент и штраф на бек при сохранении его. Если не получилось отправить - показывать алерт и выводить кнопку синхронизации (отправки)
// TODO: Добавить вкладку "Заявки на вступление в команду"
// TODO: Сделать progressBarы везде одинаковые
// TODO: Добавить алерт при выходе из учетки и при смене просматриваемого человека, что есть неотправленные данные и они потеряются


// TODO:
//  - добавить настройки для юзеров (доступны только админам),
//  - добавить принятие юзеров в пространство по решению админов, а не сразу
