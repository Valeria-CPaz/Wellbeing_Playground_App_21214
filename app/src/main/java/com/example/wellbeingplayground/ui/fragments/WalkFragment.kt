package com.example.wellbeingplayground.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wellbeingplayground.R
import com.example.wellbeingplayground.adapters.WalkAdapter
import com.example.wellbeingplayground.interfaces.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.wellbeingplayground.others.SortType
import com.example.wellbeingplayground.others.TrackingUtility
import com.example.wellbeingplayground.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_walk.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

@AndroidEntryPoint
class WalkFragment : Fragment(R.layout.fragment_walk), EasyPermissions.PermissionCallbacks {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var walkAdapter: WalkAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestPermission()
        setupRecyclerView()
        when(viewModel.sortType){
            SortType.DATE -> spFilter.setSelection(0)
            SortType.WALKING_TIME -> spFilter.setSelection(1)
            SortType.DISTANCE -> spFilter.setSelection(2)
            SortType.AVG_SPEED -> spFilter.setSelection(3)
            SortType.CALORIES_BURNED -> spFilter.setSelection(4)
        }

        spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                when(pos){
                    0 -> viewModel.sortWalks(SortType.DATE)
                    1 -> viewModel.sortWalks(SortType.WALKING_TIME)
                    2 -> viewModel.sortWalks(SortType.DISTANCE)
                    3 -> viewModel.sortWalks(SortType.AVG_SPEED)
                    4 -> viewModel.sortWalks(SortType.CALORIES_BURNED)
                }
            }

        }

        viewModel.walks.observe(viewLifecycleOwner, Observer {
            walkAdapter.submitList(it)
        })
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_walkFragment_to_trackingFragment)
        }
    }

    private fun setupRecyclerView() = rvWalks.apply {
        walkAdapter = WalkAdapter()
        adapter = walkAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun requestPermission() {
        if (TrackingUtility.hasLocationPermission(requireContext())) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

            EasyPermissions.requestPermissions(
                this,
                "you need to accept location permissions to use this app",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        else{
            EasyPermissions.requestPermissions(
                this,
                "you need to accept location permissions to use this app",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            AppSettingsDialog.Builder(this).build().show()
        }
        else{
            requestPermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }
}