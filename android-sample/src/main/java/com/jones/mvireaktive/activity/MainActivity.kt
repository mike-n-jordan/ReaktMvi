package com.jones.mvireaktive.activity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.badoo.reaktive.disposable.scope.DisposableScope
import com.badoo.reaktive.disposable.scope.disposableScope
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.mainScheduler
import com.jones.mvireaktive.activity.store.ExampleWish.Wish
import com.jones.mvireaktive.activity.store.MviExample
import com.jones.mvireaktive.middleware.MiddlewareConfig
import com.jones.mvireaktive.middleware.timetravel.TimeTravelMiddleware

@Suppress("EXPERIMENTAL_API_USAGE")
class MainActivity : AppCompatActivity(), DisposableScope by DisposableScope() {

    private lateinit var mviStore: MviExample
    private var startStopScope: DisposableScope? = null
    private lateinit var counter: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.counter_screen)
        val timeTravel = TimeTravelMiddleware()
        MiddlewareConfig.registerGlobalMiddleware(timeTravel)

        mviStore = MviExample().scope()
        counter = findViewById(R.id.counter_value)

        findViewById<View>(R.id.add_one).setOnClickListener {
            mviStore.onNext(Wish.AddOne)
        }
        findViewById<View>(R.id.add_one_slow).setOnClickListener {
            mviStore.onNext(Wish.SlowAddOne)
        }
        findViewById<View>(R.id.remove_one).setOnClickListener {
            mviStore.onNext(Wish.RemoveOne)
        }
        findViewById<View>(R.id.remove_one_slow).setOnClickListener {
            mviStore.onNext(Wish.SlowRemoveOne)
        }
        findViewById<View>(R.id.start_playback).setOnClickListener {
            timeTravel.startPlayback()
        }
    }

    override fun onStart() {
        super.onStart()

        startStopScope = disposableScope {
            mviStore.observeOn(mainScheduler).subscribeScoped {
                counter.text = it.count.toString()
            }

            mviStore.news.observeOn(mainScheduler).subscribeScoped {
                Toast.makeText(this@MainActivity, it::class.java.simpleName, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        startStopScope?.dispose()
        startStopScope = null

        super.onStop()
    }

    override fun onDestroy() {
        dispose()

        super.onDestroy()
    }
}
