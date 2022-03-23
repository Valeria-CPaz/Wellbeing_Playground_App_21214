package com.example.wellbeingplayground.database

import androidx.lifecycle.LiveData
import androidx.room.*

// queries for DB
@Dao
interface WalkDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalk(walk: Walk)

    @Delete
    suspend fun deleteWalk(walk: Walk)

    @Query("SELECT * FROM walking_table ORDER BY timestamp DESC")
    fun getAllWalksSortedByDate(): LiveData<List<Walk>>

    @Query("SELECT * FROM walking_table ORDER BY timeInMillis DESC")
    fun getAllWalksSortedByTimeInMills(): LiveData<List<Walk>>

    @Query("SELECT * FROM walking_table ORDER BY caloriesBurned DESC")
    fun getAllWalksSortedByCaloriesBurned(): LiveData<List<Walk>>

    @Query("SELECT * FROM walking_table ORDER BY avgSpeedInKMH DESC")
    fun getAllWalksSortedByAvgSpeed(): LiveData<List<Walk>>

    @Query("SELECT * FROM walking_table ORDER BY distanceInMeters DESC")
    fun getAllWalksSortedByDistance(): LiveData<List<Walk>>

    @Query("SELECT SUM(timeInMillis) FROM walking_table")
    fun getTotalTimeInMillis(): LiveData<Long>

    @Query("SELECT SUM(caloriesBurned) FROM walking_table")
    fun getTotalCaloriesBurned(): LiveData<Int>

    @Query("SELECT SUM(distanceInMeters) FROM walking_table")
    fun getTotalDistance(): LiveData<Int>

    @Query("SELECT AVG(avgSpeedInKMH) FROM walking_table")
    fun getTotalAvgSpeed(): LiveData<Int>

}