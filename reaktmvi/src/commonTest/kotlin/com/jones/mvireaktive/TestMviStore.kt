package com.jones.mvireaktive.middleware

import com.badoo.reaktive.scheduler.Scheduler
import com.badoo.reaktive.scheduler.trampolineScheduler
import com.jones.mvireaktive.BaseMviStore
import com.jones.mvireaktive.StoreConfigBuilder


data class State(val count: Int = 0, val slowAddCount: Int = 0)

sealed class Event {
    object AddOne : Event()
    object Other : Event()
    object SlowAddOne : Event()
}

fun createMviStore(
    testScheduler: Scheduler = trampolineScheduler,
    middleware: List<Middleware> = emptyList(),
    builder: StoreConfigBuilder<State, Event, Nothing>.() -> Unit
) =
    object : BaseMviStore<State, Event, Nothing>(
        initialState = State(),
        storeBuilder = builder,
        internalScheduler = testScheduler,
        localMiddleware = middleware
    ) {}
