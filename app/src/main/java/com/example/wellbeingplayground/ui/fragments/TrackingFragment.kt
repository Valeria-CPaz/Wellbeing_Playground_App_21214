package com.example.wellbeingplayground.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.wellbeingplayground.R
import com.example.wellbeingplayground.database.Walk
import com.example.wellbeingplayground.others.Constants
import com.example.wellbeingplayground.others.TrackingUtility
import com.example.wellbeingplayground.services.Polyline
import com.example.wellbeingplayground.services.TrackingServices
import com.example.wellbeingplayground.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import java.util.*
import javax.inject.Inject
import kotlin.math.round

// tracking class
@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var map: GoogleMap? = null

    private var curTimeInMillis = 0L

    private var menu: Menu? = null

    @set:Inject
    var weight = 80f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView?.onCreate(savedInstanceState)

        btnToggleWalk.setOnClickListener {
            toggleRun()
        }

        btnFinishWalk.setOnClickListener {
            zoomToSeeWhoToTrack()
            endWalkAndSaveToDb()
        }
        mapView.getMapAsync{
            map = it
            addAllPolyLine()
        }

        subscribeTOObservers()
    }

    private fun subscribeTOObservers(){
        TrackingServices.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })
        TrackingServices.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyLine()
            moveCameraToUser()
        })

        TrackingServices.timeWalkInMillis.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis,true)
            tvTimer.text = formattedTime
        })
    }

    private fun toggleRun(){
        if(isTracking){
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(Constants.ACTION_PAUSE_SERVICE)
        }
        else{
            sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu,menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (curTimeInMillis > 0L){
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCancelTrackingDialog(){
        val dialog = MaterialAlertDialogBuilder(requireContext(),R.style.AlertDialogTheme)
            .setTitle("Cancel the Walk ? ")
            .setMessage("Are You sure you want to cancel walk and delete all its data")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("yes"){_,_ ->
                stopWalk()
            }
            .setNegativeButton("No"){ dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    private fun stopWalk(){

        sendCommandToService(Constants.ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_walkFragment)
    }

    private fun updateTracking(isTracking : Boolean){
        this.isTracking = isTracking
        if(!isTracking){
            btnToggleWalk.text = "Start"
            btnFinishWalk.visibility = View.VISIBLE
        }
        else{
            btnToggleWalk.text = "Stop"
            menu?.getItem(0)?.isVisible = true
            btnFinishWalk.visibility = View.GONE
        }
    }

    private fun moveCameraToUser(){
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    Constants.MAP_ZOOM
                )
            )
        }
    }
    private fun zoomToSeeWhoToTrack(){
        val bounds = LatLngBounds.Builder()
        for (polyline in pathPoints){
            for (pos in polyline){
                bounds.include(pos)
            }
        }

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                (mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun endWalkAndSaveToDb(){
        map?.snapshot { bmp->

            var distanceInMetres =0
            for (polyline in pathPoints){
                distanceInMetres += TrackingUtility.calculatePolyLineLength(polyline).toInt()
            }
            val avgSpeed = round(distanceInMetres / 1000f) / (curTimeInMillis / 10000f / 60 / 60) / 10f
            val dateTimeStamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMetres / 1000f) * weight).toInt()
            val walk = Walk(bmp,dateTimeStamp,avgSpeed,distanceInMetres,curTimeInMillis,caloriesBurned)
            viewModel.insertWalk(walk)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Walk saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopWalk()
        }
    }

    private fun addAllPolyLine(){
        for (polyline in pathPoints){
            val polylineOptions = PolylineOptions()
                .color(Constants.POLYLINE_COLOR)
                .width(Constants.POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyLine(){
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1){
            val preLastLang = pathPoints.last()[pathPoints.last().size-2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(Constants.POLYLINE_COLOR)
                .width(Constants.POLYLINE_WIDTH)
                .add(preLastLang)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action:String) =
        Intent(requireContext(),TrackingServices::class.java).also{
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }


    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }


}