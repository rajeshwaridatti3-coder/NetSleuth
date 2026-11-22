package com.rajeshwari.netsleuth.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DiagnosticRecord::class], version = 2, exportSchema = false)
abstract class NetSleuthDatabase : RoomDatabase() {
    abstract fun diagnosticDao(): DiagnosticDao

    companion object {
        @Volatile private var INSTANCE: NetSleuthDatabase? = null
        fun get(context: Context): NetSleuthDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, NetSleuthDatabase::class.java, "netsleuth.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
