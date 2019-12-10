package com.jones.mvireaktive.middleware

import com.jones.mvireaktive.StoreConfig

 class TestMiddleware : Middleware {

    val stores = mutableMapOf<Any, StoreConfig<*, *, *>>()
    private var disposed = false

    override fun <State, Input : Any, Output> wrapStore(
        store: StoreConfig<State, Input, Output>
    ): StoreConfig<State, Input, Output> {
        stores[store.key] = store
        return store
    }

    override fun <State, Input : Any, Output> unregisterStore(
        store: StoreConfig<State, Input, Output>
    ) {
        stores.remove(store.key)
    }

    override val isDisposed: Boolean
        get() = disposed

    override fun dispose() {
        disposed = true
    }

}
