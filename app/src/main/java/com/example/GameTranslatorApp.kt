package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.repository.GameTranslatorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GameTranslatorApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { GameTranslatorRepository(database.translationDao(), database.glossaryDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.util.UserPreferencesManager.init(this)
    }

    companion object {
        lateinit var instance: GameTranslatorApp
            private set
    }
}
