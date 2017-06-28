package com.erill.bicingplus.main

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main.*
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
    var progressDialog: ProgressDialog? = null

    var currentInfoType: InfoType = InfoType.BIKES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        component.inject(this)

        buildGoogleApiClient()
        createLocationRequest()
        fusedLocationClient = LocationServices.FusedLocationApi

        presenter.loadStations(currentInfoType)

        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            loadedMap -> loadLastLocation(loadedMap)
        }

        fab_change_mode.setOnClickListener { presenter.onChangeSetting(currentInfoType) }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh_option -> {
                presenter.loadStations(currentInfoType)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startLocationUpdates() {
        fusedLocationClient?.requestLocationUpdates(googleApiClient, locationRequest) {
            location ->
            Log.d(TAG, "New location ${location.latitude} - ${location.longitude}")
            if (lastLocation == null) {
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), CLOSE_ZOOM)
                googleMap?.animateCamera(cameraUpdate)
            }
            lastLocation = location
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
        googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(latLong, zoomLevel, DEFAULT_TILT, 0f)))
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

    override fun setInfoType(infoType: InfoType) {
        currentInfoType = infoType
        val resourceId: Int
        when (currentInfoType) {
            InfoType.BIKES -> resourceId = R.drawable.ic_directions_bike_white_24dp
            InfoType.PARKING -> resourceId = R.drawable.ic_local_parking_white_24dp
        }
        fab_change_mode.setImageDrawable(ContextCompat.getDrawable(this, resourceId))
    }

    override fun printStations(response: BicingResponse?, infoType: InfoType) {
        googleMap?.clear()
        response?.stations?.forEach {
            val latLong: LatLng = LatLng(it.lat.toDouble(), it.lon.toDouble())
            val markerOptions = MarkerOptions()
                    .position(latLong)
                    .title(it.street + " " + it.number)
            val isElectric = it.type == BikeType.ELECTRIC
            val valueStr = when (infoType) {
                InfoType.BIKES -> it.bikes
                InfoType.PARKING -> it.slots
            }
            val valueNum = valueStr.toFloat()
            val markerBitmap: Bitmap = writeTextOnDrawable(R.drawable.marker_circle, valueStr, isElectric)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
            markerOptions.zIndex(valueNum)

            googleMap?.addMarker(markerOptions)
        }
    }

    fun drawableToBitmap (drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height);
        drawable.draw(canvas)
        return bitmap
    }

    private fun writeTextOnDrawable(drawableId: Int, text: String, isElectric: Boolean): Bitmap {
        val drawable = ContextCompat.getDrawable(this, drawableId) as GradientDrawable
        val num = text.toInt()
        when (num) {
            0 -> drawable.setColor(ContextCompat.getColor(this,R.color.very_low_disponibility))
            in 1..6 -> drawable.setColor(ContextCompat.getColor(this,R.color.low_disponibility))
            in 7..12 -> drawable.setColor(ContextCompat.getColor(this,R.color.normal_disponibility))
            in 13..18 -> drawable.setColor(ContextCompat.getColor(this,R.color.good_disponibility))
            else -> drawable.setColor(ContextCompat.getColor(this,R.color.very_good_disponibility))
        }
        if (isElectric) {
            drawable.setStroke(MARKER_ELECTRIC_STROKE_SIZE, ContextCompat.getColor(this, R.color.electric_bike_marker))
        }
        else {
            drawable.setStroke(MARKER_ELECTRIC_STROKE_SIZE, ContextCompat.getColor(this, android.R.color.black))
        }
        val bitmap = drawableToBitmap(drawable)
        val markerBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.typeface = Typeface.create("Helvetica", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = convertToPixels(this, MARKER_TEXT_SIZE)
        paint.getTextBounds(text, 0, text.length, Rect())

        val canvas = Canvas(markerBitmap)

        val x = canvas.width / 2 - 2     //-2 used to regulate x position offset
        val distanceBaseLineToCenter = (paint.descent() + paint.ascent()) / 2
        val y = canvas.height / 2 - distanceBaseLineToCenter

        canvas.drawText(text, x.toFloat(), y, paint)

        return markerBitmap
    }

    fun convertToPixels(context: Context, dp: Int): Float {
        val conversionScale = context.resources.displayMetrics.density
        return (dp * conversionScale + 0.5f)
    }


    private fun getProgress(): ProgressDialog {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog!!.setCancelable(false)
            progressDialog!!.isIndeterminate = true
            progressDialog!!.setMessage(getString(R.string.loading))
        }
        return progressDialog as ProgressDialog
    }

    override fun showProgress() {
        if (getProgress().isShowing) return
        getProgress().show()
    }

    override fun hideProgress() {
        if (getProgress().isShowing) getProgress().dismiss()
    }

    override fun showSuccess() {
        val snackbar = Snackbar.make(coordinator_main, R.string.update_complete, Snackbar.LENGTH_SHORT)
        snackbar.show()
    }

    override fun showError() {
        val snackbar = Snackbar.make(coordinator_main, R.string.update_error, Snackbar.LENGTH_SHORT)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        snackbar.show()
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
