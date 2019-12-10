package com.jones.mvireaktive.activity

import com.badoo.reaktive.observable.delay
import com.badoo.reaktive.observable.observableOf
import com.badoo.reaktive.scheduler.mainScheduler
import com.jones.mvireaktive.AbstractMviStore
import com.jones.mvireaktive.StoreConfigBuilder

data class State(
    val count: Int = 0,
    val loadingSlowAdd: Boolean = false,
    val loadingSlowRemove: Boolean = false
)

sealed class ExampleWish {
    object AddOne : ExampleWish()
    object RemoveOne : ExampleWish()
    object SlowAddOne : ExampleWish()
    object SlowRemoveOne : ExampleWish()
}

private object Reset : ExampleWish()
private class SlowRemoveOneReceived(val success: Boolean) : ExampleWish()
private class SlowAddOneReceived(val success: Boolean) : ExampleWish()

sealed class News {
    object ResetEvent : News()
    object SlowAddOneSuccess : News()
    object SlowRemoveOneSuccess : News()
}

class MviExample() : AbstractMviStore<State, ExampleWish, News>(
    initialState = State(),
    storeBuilder = {
        registerInit()
        registerResetEvents()

        on<ExampleWish.AddOne>()
            .reduce { state, event -> state.copy(count = state.count + 1) }

        on<ExampleWish.SlowAddOne>()
            .filter { state, event -> !state.loadingSlowAdd }
            .action { state, event ->
                observableOf(SlowAddOneReceived(success = true))
                    .delay(2_000, mainScheduler)
            }
            .reduce { state, event -> state.copy(loadingSlowAdd = true) }

        on<SlowAddOneReceived>()
            .reduce { state, event ->
                val count = if (event.success) state.count + 1 else state.count
                state.copy(count = count, loadingSlowAdd = false)
            }
            .news { state, slowAdd -> if (slowAdd.success) News.SlowAddOneSuccess else null }
            .post { state, slowAdd -> ExampleWish.AddOne }

        registerRemoveEvents()
    }
)

private fun StoreConfigBuilder<State, ExampleWish, News>.registerInit() {
    bootstrapWith(ExampleWish.SlowAddOne)
    bootstrap { state -> observableOf(ExampleWish.AddOne, ExampleWish.AddOne) }
}

private fun StoreConfigBuilder<State, ExampleWish, News>.registerResetEvents() {
    on<Reset>()
        .reduce { state, event -> State() }
        .news { state, event -> News.ResetEvent }

    post { state, event ->
        when (event) {
            is ExampleWish.AddOne -> if (state.count > 9) Reset else null
            is Reset,
            is SlowRemoveOneReceived,
            is SlowAddOneReceived,
            is ExampleWish.RemoveOne,
            is ExampleWish.SlowAddOne,
            is ExampleWish.SlowRemoveOne -> null
        }
    }
    postEvent<ExampleWish.RemoveOne> { state, removeOne -> if (state.count < -5) Reset else null }
}

private fun StoreConfigBuilder<State, ExampleWish, News>.registerRemoveEvents() {
    on<ExampleWish.RemoveOne>()
        .reduce { state, event -> state.copy(count = state.count - 1) }

    on<ExampleWish.SlowRemoveOne>()
        .filter { state, event -> !state.loadingSlowRemove }
        .action { state, event ->
            observableOf(SlowRemoveOneReceived(success = true))
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
