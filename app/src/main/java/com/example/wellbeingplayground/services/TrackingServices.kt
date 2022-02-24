package com.example.wellbeingplayground.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getService
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.wellbeingplayground.R
import com.example.wellbeingplayground.interfaces.Constants
import com.example.wellbeingplayground.interfaces.Constants.ACTION_PAUSE_SERVICE
import com.example.wellbeingplayground.interfaces.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.wellbeingplayground.interfaces.Constants.NOTIFICATION_ID
import com.example.wellbeingplayground.others.TrackingUtility
import com.example.wellbeingplayground.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingServices: LifecycleService() {

    var serviceKilled = false
    var isFirstWalk = true

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val timeWalkInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var curNotificationBuilder : NotificationCompat.Builder

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
        val timeWalkInMillis = MutableLiveData<Long>()
    }

    private fun postInitializeValue(){
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeWalkInSeconds.postValue(0L)
        timeWalkInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        postInitializeValue()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, Observer {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    private fun killService(){
        serviceKilled = true
        isFirstWalk = true
        pauseService()
        postInitializeValue()
        stopForeground(true)
        stopSelf()
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstWalk){
                        startForegroundService()
                        isFirstWalk = false
                    }
                    else{
                        Timber.d("Resuming Service")
                        startTimer()
                    }
                }
                Constants.ACTION_PAUSE_SERVICE -> {
                    Timber.d("paused Service")
                    pauseService()
                }
                Constants.ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped Service")
                    killService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeWalk = 0L
    private var timeStarted =0L
    private var lastSecondTimestamp = 0L

    private fun startTimer(){
        addEmptyPolyLine()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!){
                lapTime = System.currentTimeMillis() - timeStarted

                timeWalkInMillis.postValue(timeWalk + lapTime)
                if(timeWalkInMillis.value!! >= lastSecondTimestamp +1000L){
                    timeWalkInSeconds.postValue(timeWalkInSeconds.value!! +1)
                    lastSecondTimestamp += 1000L
                }
                delay(Constants.TIMER_UPDATE_INTERVAL)
            }
            timeWalk += lapTime
        }
    }

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerEnabled = false

    }

    private fun updateNotificationTrackingState(isTracking:Boolean){
        val notificationActionText = if (isTracking) "pause" else "Resume"
        val pendingIntent = if (isTracking){
            val pauseIntent = Intent(this,TrackingServices::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this,1,pauseIntent, FLAG_UPDATE_CURRENT)
        }
        else{
            val resumeIntent = Intent(this,TrackingServices::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            getService(this,2,resumeIntent,FLAG_UPDATE_CURRENT)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder,ArrayList<NotificationCompat.Action>())
        }
        if(!serviceKilled){
            curNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_walk,notificationActionText,pendingIntent)
            notificationManager.notify(NOTIFICATION_ID,curNotificationBuilder.build())
        }

    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking:Boolean){
        if (isTracking){
            if (TrackingUtility.hasLocationPermission(this)){
                val request = LocationRequest().apply {
                    interval = Constants.LOCATION_UPDATE_INTERVAL
                    fastestInterval = Constants.FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallBack,
                    Looper.getMainLooper()
                )
            }
        }
        else{
            fusedLocationProviderClient.removeLocationUpdates(locationCallBack)
        }
    }

    val locationCallBack = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            super.onLocationResult(p0)
            if (isTracking.value!!){
                p0?.locations?.let { locations ->
                    for (location in locations){
                        addPathPoints(location)
                    }
                }
            }
        }
    }

    private fun addPathPoints(location:Location?){
        location?.let{
            val pos = LatLng(location
                .latitude,location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    private fun addEmptyPolyLine() = pathPoints.value?.apply{
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun startForegroundService(){
        startTimer()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }


        startForeground(Constants.NOTIFICATION_ID, baseNotificationBuilder.build())

        timeWalkInSeconds.observe(this, Observer {
            if (!serviceKilled){
                val notification = curNotificationBuilder
                    .setContentText(TrackingUtility.getFormattedStopWatchTime(it*1000L))
                notificationManager.notify(NOTIFICATION_ID,notification.build())
            }
        })
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {

        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}