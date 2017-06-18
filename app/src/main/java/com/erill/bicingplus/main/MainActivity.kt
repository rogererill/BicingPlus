package com.erill.bicingplus.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.erill.bicingplus.*
import com.erill.bicingplus.main.di.MainModule
import com.erill.bicingplus.ws.responses.BicingResponse
import com.erill.bicingplus.ws.responses.BikeType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import javax.inject.Inject



class MainActivity : AppCompatActivity(), MainView {

    @Inject lateinit var presenter: MainPresenter

    val component by lazy { app.component.plus(MainModule(this)) }
    var googleMap: GoogleMap? = null
    private val LOCATION_REQUEST_CODE: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        component.inject(this)

        presenter.loadStations()

        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            loadedMap -> loadDefaultLocation(loadedMap)
        }
    }

    override fun printStations(response: BicingResponse?) {
        response?.stations?.forEach {
            val latLong: LatLng = LatLng(it.lat.toDouble(), it.lon.toDouble())
            val markerOptions = MarkerOptions()
                    .position(latLong)
                    .title(it.street + " " + it.number)
            if (it.type == BikeType.ELECTRIC) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            }
            googleMap?.addMarker(markerOptions)
        }
    }

    private fun loadDefaultLocation(loadedMap: GoogleMap?) {
        googleMap = loadedMap
        val bcn = LatLng(DEFAULT_LAT, DEFAULT_LON)
        /*loadedMap?.addMarker(MarkerOptions().position(bcn)
                .title("My marker"))*/
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(bcn, DEFAULT_ZOOM))
        googleMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        checkLocation()
    }

    private fun checkLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        } else {
            val permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(this, permissionArray, LOCATION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            googleMap?.isMyLocationEnabled = permissions.size == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun showProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
