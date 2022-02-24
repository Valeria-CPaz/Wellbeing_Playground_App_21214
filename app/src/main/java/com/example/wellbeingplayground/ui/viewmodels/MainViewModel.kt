package com.example.wellbeingplayground.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wellbeingplayground.database.Walk
import com.example.wellbeingplayground.others.SortType
import com.example.wellbeingplayground.repository.MainRepository
import kotlinx.coroutines.launch

class MainViewModel @ViewModelInject constructor(
    val mainRepository: MainRepository
) : ViewModel() {

    private val walksSortedByDate = mainRepository.getAllWalksSortedByDate()
    private val walksSortedByDistance = mainRepository.getAllWalksSortedByDistance()
    private val walksSortedByCalories = mainRepository.getAllWalksSortedByCaloriesBurned()
    private val walksSortedByTIme = mainRepository.getAllWalksSortedByTimeInMills()
    private val walksSortedByAvgSpeed = mainRepository.getAllWalksSortedByAvgSpeed()

    val walks = MediatorLiveData<List<Walk>>()

    var sortType = SortType.DATE

    init {
        walks.addSource(walksSortedByDate){result ->
            if (sortType == SortType.DATE){
                result?.let { walks.value = it }
            }
        }
        walks.addSource(walksSortedByDistance){result ->
            if (sortType == SortType.DISTANCE){
                result?.let { walks.value = it }
            }
        }
        walks.addSource(walksSortedByCalories){result ->
            if (sortType == SortType.CALORIES_BURNED){
                result?.let { walks.value = it }
            }
        }
        walks.addSource(walksSortedByTIme){result ->
            if (sortType == SortType.WALKING_TIME){
                result?.let { walks.value = it }
            }
        }
        walks.addSource(walksSortedByAvgSpeed){result ->
            if (sortType == SortType.AVG_SPEED){
                result?.let { walks.value = it }
            }
        }
    }

    fun sortWalks(sortType: SortType) = when(sortType){
        SortType.DATE -> walksSortedByDate.value?.let { walks.value = it }
        SortType.DISTANCE -> walksSortedByDistance.value?.let { walks.value = it }
        SortType.AVG_SPEED -> walksSortedByAvgSpeed.value?.let { walks.value = it }
        SortType.CALORIES_BURNED -> walksSortedByCalories.value?.let { walks.value = it }
        SortType.WALKING_TIME -> walksSortedByTIme.value?.let { walks.value = it }
    }.also {
        this.sortType = sortType
    }

    fun insertWalk(walk: Walk) = viewModelScope.launch {
        mainRepository.insertWalk(walk)
    }
}