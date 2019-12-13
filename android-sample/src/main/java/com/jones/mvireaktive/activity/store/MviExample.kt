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

class MviExample : BaseMviStore<ExampleState, ExampleWish, News>(
    initialState = ExampleState(),
    storeBuilder = {
        registerInit()
        registerResetEvents()

        on<Wish.AddOne>()
            .reduce { state, event -> state.copy(count = state.count + 1) }
        on<Wish.SlowAddOne>()
            .filter { state, event -> !state.loadingSlowAdd }
            .action { state, event ->
                observableOf(
                    SlowAddOneReceived(
                        success = true
                    )
                )
                    .delay(2_000, mainScheduler)
            }
            .reduce { state, event -> state.copy(loadingSlowAdd = true) }
        on<SlowAddOneReceived>()
            .reduce { state, event ->
                val count = if (event.success) state.count + 1 else state.count
                state.copy(count = count, loadingSlowAdd = false)
            }
            .news { state, slowAdd -> if (slowAdd.success) News.SlowAddOneSuccess else null }
            .post { state, slowAdd -> Wish.AddOne }

        registerRemoveEvents()
    }
)

private fun StoreConfigBuilder<ExampleState, ExampleWish, News>.registerInit() {
    bootstrapWith(Wish.SlowAddOne)
    bootstrap { state -> observableOf(Wish.AddOne, Wish.AddOne) }
}

private fun StoreConfigBuilder<ExampleState, ExampleWish, News>.registerResetEvents() {
    on<Reset>()
        .reduce { state, event -> ExampleState() }
        .news { state, event -> News.ResetEvent }

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

private fun StoreConfigBuilder<ExampleState, ExampleWish, News>.registerRemoveEvents() {
    on<Wish.RemoveOne>()
        .reduce { state, event -> state.copy(count = state.count - 1) }

    on<Wish.SlowRemoveOne>()
        .filter { state, event -> !state.loadingSlowRemove }
        .action { state, event ->
            observableOf(
                SlowRemoveOneReceived(
                    success = true
                )
            )
                .delay(2_000, mainScheduler)
        }
        .reduce { state, event -> state.copy(loadingSlowRemove = true) }

    on<SlowRemoveOneReceived>()
        .reduce { state, event ->
            val count = if (event.success) state.count - 1 else state.count
            state.copy(count = count, loadingSlowRemove = false)
        }
        .news { state, slowRemoved ->
            if (slowRemoved.success) News.SlowRemoveOneSuccess else null
        }
}
