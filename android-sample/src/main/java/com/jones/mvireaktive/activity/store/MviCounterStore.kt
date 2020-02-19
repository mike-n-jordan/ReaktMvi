package com.jones.mvireaktive.activity.store

import com.badoo.reaktive.observable.delay
import com.badoo.reaktive.observable.observableOf
import com.badoo.reaktive.scheduler.mainScheduler
import com.jones.mvireaktive.BaseMviStore
import com.jones.mvireaktive.StoreConfigBuilder

private object Reset : MviCounterWish
private class SlowRemoveOneReceived(val success: Boolean) : MviCounterWish
private class SlowAddOneReceived(val success: Boolean) : MviCounterWish

typealias MviCounterBuilder = StoreConfigBuilder<MviCounterState, MviCounterWish, News>

class MviCounterStore : BaseMviStore<MviCounterState, MviCounterWish, News>(
    initialState = MviCounterState(),
    storeBuilder = {
        registerInit()
        registerResetEvents()
        registerAddEvents()
        registerRemoveEvents()
    }
)

private fun MviCounterBuilder.registerInit() {
    bootstrapWith(MviCounterWish.SlowAddOne)
    bootstrap { state -> observableOf(MviCounterWish.AddOne, MviCounterWish.AddOne) }
}

private fun MviCounterBuilder.registerResetEvents() {
    on<Reset>()
        .reducer { state, event -> MviCounterState() }
        .newsPublisher { state, event -> News.ResetEvent }

    post { state, event ->
        when (event) {
            is MviCounterWish.AddOne -> if (state.count > 9) Reset else null
            is MviCounterWish.RemoveOne -> if (state.count < -5) Reset else null
            else -> null
        }
    }
}

private fun MviCounterBuilder.registerRemoveEvents() {
    on<MviCounterWish.RemoveOne>()
        .filter { exampleState, removeOne -> true }
        .reducer { state, removeOne -> state.copy(count = state.count - 1) }
        .newsPublisher { exampleState, removeOne -> null }

    on<MviCounterWish.SlowRemoveOne>()
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

private fun MviCounterBuilder.registerAddEvents() {
    on<MviCounterWish.AddOne>()
        .reducer { state, event -> state.copy(count = state.count + 1) }

    on<MviCounterWish.SlowAddOne>()
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
        .postEventPublisher { state, slowAdd -> MviCounterWish.AddOne }
}
