package com.constructiontracker

import android.app.Application
import com.constructiontracker.data.database.AppDatabase
import com.constructiontracker.data.repository.ConstructionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ConstructionTrackerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ConstructionRepository(database) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.initializeDefaultContractors()
        }
    }
}
