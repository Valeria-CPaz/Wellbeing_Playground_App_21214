package com.example.wellbeingplayground.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wellbeingplayground.R
import com.example.wellbeingplayground.database.Walk
import com.example.wellbeingplayground.others.TrackingUtility
import kotlinx.android.synthetic.main.item_walk.view.*
import java.text.SimpleDateFormat
import java.util.*

// adapter class
class WalkAdapter : RecyclerView.Adapter<WalkAdapter.WalkViewHolder>() {

    inner class WalkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


    val diffCallback = object : DiffUtil.ItemCallback<Walk>(){
        override fun areItemsTheSame(oldItem: Walk, newItem: Walk): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Walk, newItem: Walk): Boolean {
            return  oldItem.hashCode() == newItem.hashCode()
        }

    }

    val differ = AsyncListDiffer(this,diffCallback)

    fun submitList(list:List<Walk>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkViewHolder {
        return WalkViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_walk,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: WalkViewHolder, position: Int) {
        val walk = differ.currentList[position]
        holder.itemView.apply {
            Glide.with(this).load(walk.img).into(ivWalkImage)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = walk.timestamp
            }
            val dateFormat = SimpleDateFormat("dd.MM.yy",Locale.getDefault())
            tvDate.text = dateFormat.format(calendar.time)

            val avgSpeed = "${walk.avgSpeedInKMH}km/h"
            tvAvgSpeed.text = avgSpeed

            val distanceInKM = "${walk.distanceInMeters / 1000f}km"
            tvDistance.text = distanceInKM

            tvTime.text = TrackingUtility.getFormattedStopWatchTime(walk.timeInMillis)

            val caloriesBurned = "${walk.caloriesBurned}kcal"
            tvCalories.text = caloriesBurned
        }
    }
}