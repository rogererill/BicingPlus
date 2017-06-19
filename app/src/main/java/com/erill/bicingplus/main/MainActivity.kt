package com.erill.bicingplus.main

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.erill.bicingplus.*
import com.erill.bicingplus.main.di.MainModule
import com.erill.bicingplus.ws.responses.BicingResponse
import com.erill.bicingplus.ws.responses.BikeType
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import javax.inject.Inject



class MainActivity : AppCompatActivity(), MainView,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    @Inject lateinit var presenter: MainPresenter

    val component by lazy { app.component.plus(MainModule(this)) }
    var googleMap: GoogleMap? = null
    val LOCATION_REQUEST_CODE: Int = 1
    private val TAG = "MapActivity"

    var googleApiClient: GoogleApiClient? = null
    var fusedLocationClient: FusedLocationProviderApi? = null
    var lastLocation: Location? = null
    var locationRequest: LocationRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        component.inject(this)

        buildGoogleApiClient()
        createLocationRequest()
        fusedLocationClient = LocationServices.FusedLocationApi

        presenter.loadStations()

        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            loadedMap -> loadLastLocation(loadedMap)
        }
    }

    @Synchronized private fun buildGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        googleApiClient?.connect()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest?.interval = UPDATE_INTERVAL
        locationRequest?.fastestInterval = FASTEST_UPDATE_INTERVAL
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onResume() {
        super.onResume()
        if (googleApiClient?.isConnected as Boolean) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient?.removeLocationUpdates(googleApiClient) { Log.d(TAG, "Stop updates")}
    }

    private fun startLocationUpdates() {
        fusedLocationClient?.requestLocationUpdates(googleApiClient, locationRequest) {
            location ->
            Log.d(TAG, "New location ${location.latitude} - ${location.longitude}")
            lastLocation = location
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), CLOSE_ZOOM)
            googleMap?.animateCamera(cameraUpdate)
        }
    }

    private fun loadLastLocation(loadedMap: GoogleMap?) {

        googleMap = loadedMap
        lastLocation = fusedLocationClient?.getLastLocation(googleApiClient)

        val latLong: LatLng
        val zoomLevel: Float
        if (lastLocation != null) {
            latLong = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
            zoomLevel = CLOSE_ZOOM
        }
        else {
            latLong = LatLng(DEFAULT_LAT, DEFAULT_LON)
            zoomLevel = DEFAULT_ZOOM
        }

        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoomLevel))
        googleMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        checkLocation()
    }

    private fun checkLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
            /*googleMap?.setOnMyLocationButtonClickListener({
                loadLastLocation(googleMap, CLOSE_ZOOM)
                true
            })*/
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

    override fun showProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "Connected")
        startLocationUpdates()
    }

    override fun onConnectionSuspended(int: Int) {
        Log.d(TAG, "Connection Suspended")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "Connection Failed: ${connectionResult.errorMessage}")
    }


}
