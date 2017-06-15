package com.erill.bicingplus.main

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.erill.bicingplus.*
import com.erill.bicingplus.main.di.MainModule
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import javax.inject.Inject



class MainActivity : AppCompatActivity(), MainView {


    @Inject lateinit var presenter: MainPresenter

    val component by lazy { app.component.plus(MainModule(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        component.inject(this)

        presenter.loadStations()

        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{
            googleMap -> loadMarker(googleMap)
        }
    }

    private fun loadMarker(googleMap: GoogleMap?) {
        val bcn = LatLng(DEFAULT_LAT, DEFAULT_LON)
        googleMap?.addMarker(MarkerOptions().position(bcn)
                .title("My marker"))
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(bcn, DEFAULT_ZOOM))
    }


    override fun showProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
