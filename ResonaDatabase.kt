package com.resona.player.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ResonaDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var instance: ResonaDatabase? = null

        fun get(context: Context): ResonaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ResonaDatabase::class.java,
                    "resona.db"
                ).build().also { instance = it }
            }
    }
}
