package com.example.wellbeingplayground.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Walk::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class WalkingDatabase : RoomDatabase() {
    abstract fun getWalkDao():WalkDAO
}