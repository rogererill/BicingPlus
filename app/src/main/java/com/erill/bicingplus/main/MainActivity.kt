package com.erill.bicingplus.main

import android.Manifest
import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
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
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
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
    var suggestionAdapter: SimpleCursorAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        component.inject(this)
        setSupportActionBar(toolbar)

        buildGoogleApiClient()
        createLocationRequest()

        setupMapAndLoadStations()
        setupSuggestionsAdapter()

        fusedLocationClient = LocationServices.FusedLocationApi
        fab_change_mode.setOnClickListener { presenter.onChangeSetting(currentInfoType) }
    }

    override fun onResume() {
        super.onResume()
        if (googleApiClient?.isConnected as Boolean) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        if (googleApiClient?.isConnected as Boolean) {
            fusedLocationClient?.removeLocationUpdates(googleApiClient) { Log.d(TAG, "Stop updates")}
        }
    }

    private fun setupMapAndLoadStations() {
        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            loadedMap ->
            loadLastLocation(loadedMap)
            presenter.loadStations(currentInfoType)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val searchItem = menu.findItem(R.id.search)
        val refreshItem = menu.findItem(R.id.refresh_option)
        val search: SearchView = searchItem.actionView as SearchView
        val searchManager: SearchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        setupSearchView(search, searchManager, refreshItem)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh_option -> {
                presenter.loadStations(currentInfoType)
                return true
            }
            R.id.locate_option -> {
                if (lastLocation != null) {
                    val cameraUpdate = CameraUpdateFactory.newLatLng(LatLng(lastLocation!!.latitude, lastLocation!!.longitude))
                    googleMap?.animateCamera(cameraUpdate)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupSearchView(search: SearchView, searchManager: SearchManager, refreshItem: MenuItem) {
        search.maxWidth = Int.MAX_VALUE
        search.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        search.suggestionsAdapter = suggestionAdapter
        search.setOnSearchClickListener {
            refreshItem.isVisible = false
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
        search.setOnCloseListener {
            refreshItem.isVisible = true
            supportActionBar?.setDisplayShowTitleEnabled(true)
            false
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                search.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) populateAdapter(newText)
                return false
            }
        })
        search.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = search.suggestionsAdapter.getItem(position) as Cursor
                val suggestionName: String = cursor.getString(1)
                search.setQuery(presenter.getSuggestion(suggestionName), true)
                search.clearFocus()
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(presenter.getLatLongForPosition(suggestionName), CLOSE_ZOOM)
                googleMap?.animateCamera(cameraUpdate)
                return true
            }
        })
    }

    private fun setupSuggestionsAdapter() {
        val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(R.id.text_suggestion)
        suggestionAdapter = SimpleCursorAdapter(this,
                R.layout.row_hint,
                null,
                from,
                to,
                0)

    }

    private fun populateAdapter(newText: String) {
        val cursor: MatrixCursor = presenter.getSuggestionMatrixCursor(newText)
        suggestionAdapter?.changeCursor(cursor)
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
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
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
            InfoType.BIKES -> {
                resourceId = R.drawable.ic_directions_bike_white_24dp
                showSnackbarMessage(R.string.showing_bikes)
            }
            InfoType.PARKING -> {
                resourceId = R.drawable.ic_local_parking_white_24dp
                showSnackbarMessage(R.string.showing_parking)
            }
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
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun writeTextOnDrawable(drawableId: Int, text: String, isElectric: Boolean): Bitmap {
        val drawable = ContextCompat.getDrawable(this, drawableId) as GradientDrawable
        val disponibility = presenter.getAvailabilities(text.toInt())
        when (disponibility) {
            AvailabilityType.VERY_LOW -> drawable.setColor(ContextCompat.getColor(this,R.color.very_low_disponibility))
            AvailabilityType.LOW -> drawable.setColor(ContextCompat.getColor(this,R.color.low_disponibility))
            AvailabilityType.NORMAL -> drawable.setColor(ContextCompat.getColor(this,R.color.normal_disponibility))
            AvailabilityType.GOOD -> drawable.setColor(ContextCompat.getColor(this,R.color.good_disponibility))
            AvailabilityType.VERY_GOOD -> drawable.setColor(ContextCompat.getColor(this,R.color.very_good_disponibility))
        }
        if (isElectric) {
            drawable.setStroke(MARKER_ELECTRIC_STROKE_SIZE, ContextCompat.getColor(this, R.color.electric_bike_marker))
        }
        else {
            drawable.setStroke(MARKER_ELECTRIC_STROKE_SIZE, ContextCompat.getColor(this, android.R.color.black))
        }
        val bitmap = drawableToBitmap(drawable)
        val markerBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val paint = getPaint(text)
        val canvas = Canvas(markerBitmap)

        val x = canvas.width / 2 - 2     //-2 used to regulate x position offset
        val distanceBaseLineToCenter = (paint.descent() + paint.ascent()) / 2
        val y = canvas.height / 2 - distanceBaseLineToCenter

        canvas.drawText(text, x.toFloat(), y, paint)

        return markerBitmap
    }

    private fun getPaint(text: String): Paint {
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.typeface = Typeface.create("Helvetica", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = this.convertToPixels(MARKER_TEXT_SIZE)
        paint.getTextBounds(text, 0, text.length, Rect())
        return paint
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
        showSnackbarMessage(R.string.update_complete)
    }

    private fun showSnackbarMessage(messageId: Int) {
        val snackbar = Snackbar.make(coordinator_main, messageId, Snackbar.LENGTH_SHORT)
        snackbar.show()
    }

    private fun showErrorWithCode(errorStringId: Int) {
        val snackbar = Snackbar.make(coordinator_main, errorStringId, Snackbar.LENGTH_SHORT)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        snackbar.show()
    }

    override fun showError() {
        showErrorWithCode(R.string.update_error)
    }

    override fun showTimeOutError() {
        showErrorWithCode(R.string.network_error)
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
