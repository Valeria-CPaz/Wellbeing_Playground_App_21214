package com.example.wellbeingplayground.others

import android.content.Context
import com.example.wellbeingplayground.database.Walk
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.marker_view.view.*
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(
    val walks: List<Walk>,
    c: Context,
    layoutId:Int
): MarkerView(c,layoutId) {

    override fun getOffset(): MPPointF {
        return MPPointF(-width/2f,-height.toFloat())
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        if (e==null){
            return
        }
        val curWalkId = e.x.toInt()
        val walk = walks[curWalkId]

        val calendar = Calendar.getInstance().apply {
            timeInMillis = walk.timestamp
        }
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        tvDate.text = dateFormat.format(calendar.time)

        val avgSpeed = "${walk.avgSpeedInKMH}km/h"
        tvAvgSpeed.text = avgSpeed

        val distanceinKM = "${walk.distanceInMeters / 1000f}km"
        tvDistance.text = distanceinKM

        tvDuration.text = TrackingUtility.getFormattedStopWatchTime(walk.timeInMillis)

        val caloriesBurned = "${walk.caloriesBurned}kcal"
        tvCaloriesBurned.text = caloriesBurned
    }
}