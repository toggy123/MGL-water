package com.example.mondrain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, DrainageCrossingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MonDrainDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: MonDrainDatabase? = null

        fun getDatabase(context: Context): MonDrainDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MonDrainDatabase::class.java,
                    "mon_drain_engineer.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
