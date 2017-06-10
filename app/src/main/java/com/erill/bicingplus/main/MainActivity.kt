package com.erill.bicingplus.main

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.erill.bicingplus.R
import com.erill.bicingplus.app
import com.erill.bicingplus.main.di.MainModule
import javax.inject.Inject

class MainActivity : AppCompatActivity(), MainView {

    @Inject lateinit var presenter: MainPresenter

    val component by lazy { app.component.plus(MainModule(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        component.inject(this)
        presenter.test();
    }

    override fun showProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideProgress() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
