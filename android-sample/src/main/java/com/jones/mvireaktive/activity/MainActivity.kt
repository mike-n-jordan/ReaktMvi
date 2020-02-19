package com.jones.mvireaktive.activity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.badoo.reaktive.disposable.CompositeDisposable
import com.badoo.reaktive.disposable.plusAssign
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.observable.subscribe
import com.badoo.reaktive.scheduler.mainScheduler
import com.jones.mvireaktive.activity.store.MviCounterStore
import com.jones.mvireaktive.activity.store.MviCounterWish
import com.jones.mvireaktive.middleware.MiddlewareConfig
import com.jones.mvireaktive.middleware.timetravel.TimeTravelMiddleware

class MainActivity : AppCompatActivity() {

    private lateinit var mviStore: MviCounterStore
    private val disposable: CompositeDisposable = CompositeDisposable()
    private lateinit var counter: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.counter_screen)
        val timeTravel = TimeTravelMiddleware()
        MiddlewareConfig.registerGlobalMiddleware(timeTravel)

        mviStore = MviCounterStore()
        counter = findViewById(R.id.counter_value)

        findViewById<View>(R.id.add_one).setOnClickListener {
            mviStore.onNext(MviCounterWish.AddOne)
        }
        findViewById<View>(R.id.add_one_slow).setOnClickListener {
            mviStore.onNext(MviCounterWish.SlowAddOne)
        }
        findViewById<View>(R.id.remove_one).setOnClickListener {
            mviStore.onNext(MviCounterWish.RemoveOne)
        }
        findViewById<View>(R.id.remove_one_slow).setOnClickListener {
            mviStore.onNext(MviCounterWish.SlowRemoveOne)
        }
        findViewById<View>(R.id.start_playback).setOnClickListener {
            timeTravel.startPlayback()
        }
    }

    override fun onStart() {
        super.onStart()
        disposable += mviStore.observeOn(mainScheduler).subscribe {
            counter.text = it.count.toString()
        }
        disposable += mviStore.news.observeOn(mainScheduler).subscribe {
            Toast.makeText(this, it::class.java.simpleName, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        disposable.clear()
    }
}
