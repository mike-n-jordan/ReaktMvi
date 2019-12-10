package com.jones.mvireaktive.middleware

import com.badoo.reaktive.scheduler.Scheduler
import com.badoo.reaktive.scheduler.singleScheduler
import com.jones.mvireaktive.StoreConfig
import com.jones.mvireaktive.StoreConfigBuilder

object MiddlewareConfig {

    private val globalMiddleware = mutableListOf<Middleware>()

    var defaultInternalScheduler: Scheduler = singleScheduler

    fun registerGlobalMiddleware(middleware: Middleware) {
        globalMiddleware.add(middleware)
    }

    fun unregisterGlobalMiddleware(middleware: Middleware, dispose: Boolean = true) {
        globalMiddleware.remove(middleware)
        if (dispose) {
            middleware.dispose()
        }
    }

    fun clearMiddleware() {
        for (middleware in globalMiddleware) {
            middleware.dispose()
        }
        globalMiddleware.clear()
    }

    private fun <State, Input: Any, BoundInput : Input, Output>
            MviActionBuilder<State, Input, BoundInput, Output>.createAction() =
        MviAction(
            filter = filter,
            action = action,
            reducer = reducer,
            news = news,
            post = post
        )

    internal fun <State, Input : Any, Output> StoreConfigBuilder<State, Input, Output>.createStoreConfig(
        key: Any,
        initialState: State,
        localMiddleware: List<Middleware>
    ): StoreConfig<State, Input, Output> =
        StoreConfig(
            initialState = initialState,
            actions = actions.mapValues { it.value.createAction() },
            postActions = postActions,
            bootstrappers = bootstrappers,
            key = key,
            middleware = localMiddleware.plus(globalMiddleware)
        ).let {
            var store = it
            for (middleware in localMiddleware) {
                store = middleware.wrapStore(store)
            }
            for (middleware in globalMiddleware) {
                store = middleware.wrapStore(store)
            }
            store
        }
}
