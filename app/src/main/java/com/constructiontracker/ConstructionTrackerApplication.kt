package com.constructiontracker

import android.app.Application
import com.constructiontracker.auth.UserPreferences
import com.constructiontracker.data.database.AppDatabase
import com.constructiontracker.data.repository.ConstructionRepository

class ConstructionTrackerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ConstructionRepository(database) }
    val userPreferences by lazy { UserPreferences(this) }
}
