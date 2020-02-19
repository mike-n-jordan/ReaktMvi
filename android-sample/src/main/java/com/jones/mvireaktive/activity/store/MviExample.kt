package com.jones.mvireaktive.activity.store

import com.badoo.reaktive.observable.delay
import com.badoo.reaktive.observable.observableOf
import com.badoo.reaktive.scheduler.mainScheduler
import com.jones.mvireaktive.BaseMviStore
import com.jones.mvireaktive.StoreConfigBuilder
import com.jones.mvireaktive.activity.store.ExampleWish.Wish

interface ExampleWish {

    sealed class Wish : ExampleWish {
        object AddOne : Wish()
        object RemoveOne : Wish()
        object SlowAddOne : Wish()
        object SlowRemoveOne : Wish()
    }
}

private object Reset : ExampleWish
private class SlowRemoveOneReceived(val success: Boolean) : ExampleWish
private class SlowAddOneReceived(val success: Boolean) : ExampleWish

typealias ExampleBuilder = StoreConfigBuilder<ExampleState, ExampleWish, News>

class MviExample : BaseMviStore<ExampleState, ExampleWish, News>(
    initialState = ExampleState(),
    storeBuilder = {
        registerInit()
        registerResetEvents()
        registerAddEvents()
        registerRemoveEvents()
    }
)

private fun ExampleBuilder.registerInit() {
    bootstrapWith(Wish.SlowAddOne)
    bootstrap { state -> observableOf(Wish.AddOne, Wish.AddOne) }
}

private fun ExampleBuilder.registerResetEvents() {
    on<Reset>()
        .reducer { state, event -> ExampleState() }
        .newsPublisher { state, event -> News.ResetEvent }

    post { state, event ->
        when (event) {
            is Wish -> when (event) {
                Wish.AddOne -> if (state.count > 9) Reset else null
                Wish.RemoveOne,
                Wish.SlowAddOne,
                Wish.SlowRemoveOne -> null
            }
            is Reset,
            is SlowRemoveOneReceived,
            is SlowAddOneReceived -> null
            else -> null
        }
    }
    postEvent<Wish.RemoveOne> { state, removeOne -> if (state.count < -5) Reset else null }
}

private fun ExampleBuilder.registerRemoveEvents() {
    on<Wish.RemoveOne>()
        .filter { exampleState, removeOne -> true }
        .newsPublisher { exampleState, removeOne -> null }

    on<Wish.SlowRemoveOne>()
        .filter { state, event ->
            !state.loadingSlowRemove
        }
        .reducer { state, event ->
            state.copy(loadingSlowRemove = true)
        }
        .actor { state, event ->
            observableOf(
                SlowRemoveOneReceived(
                    success = true
                )
            ).delay(2_000, mainScheduler)
        }

    on<SlowRemoveOneReceived>()
        .reducer { state, event ->
            val count = if (event.success) state.count - 1 else state.count
            state.copy(count = count, loadingSlowRemove = false)
        }
        .newsPublisher { state, slowRemoved ->
            if (slowRemoved.success) News.SlowRemoveOneSuccess else null
        }
}

private fun ExampleBuilder.registerAddEvents() {
    on<Wish.AddOne>()
        .reducer { state, event -> state.copy(count = state.count + 1) }
    on<Wish.SlowAddOne>()
        .filter { state, event -> !state.loadingSlowAdd }
        .reducer { state, event -> state.copy(loadingSlowAdd = true) }
        .actor { state, event ->
            observableOf(
                SlowAddOneReceived(
                    success = true
                )
            )
                .delay(2_000, mainScheduler)
        }
    on<SlowAddOneReceived>()
        .reducer { state, event ->
            val count = if (event.success) state.count + 1 else state.count
            state.copy(count = count, loadingSlowAdd = false)
        }
        .newsPublisher { state, slowAdd -> if (slowAdd.success) News.SlowAddOneSuccess else null }
        .postEventPublisher { state, slowAdd -> Wish.AddOne }
}
