package com.example.wellbeingplayground.repository


import com.example.wellbeingplayground.database.Walk
import com.example.wellbeingplayground.database.WalkDAO
import javax.inject.Inject

class MainRepository @Inject constructor(
    val walkDAO: WalkDAO
) {

    suspend fun insertWalk(walk:Walk) = walkDAO.insertWalk(walk)
    suspend fun deleteWalk(walk:Walk) = walkDAO.deleteWalk(walk)

    fun getAllWalksSortedByDate() = walkDAO.getAllWalksSortedByDate()

    fun getAllWalksSortedByDistance() = walkDAO.getAllWalksSortedByDistance()

    fun getAllWalksSortedByCaloriesBurned() = walkDAO.getAllWalksSortedByCaloriesBurned()

    fun getAllWalksSortedByAvgSpeed() = walkDAO.getAllWalksSortedByAvgSpeed()

    fun getAllWalksSortedByTimeInMills() = walkDAO.getAllWalksSortedByTimeInMills()

    fun getTotalAvgSpeed() = walkDAO.getTotalAvgSpeed()

    fun getTotalDistance() = walkDAO.getTotalDistance()

    fun getTotalCalories() = walkDAO.getTotalCaloriesBurned()

    fun getTotalTimeInMIlls() = walkDAO.getTotalTimeInMillis()
}