package com.jones.mvireaktive.middleware

import com.badoo.reaktive.disposable.Disposable
import com.jones.mvireaktive.StoreConfig

interface Middleware : Disposable {

    fun <State, Input : Any, Output> wrapStore(store: StoreConfig<State, Input, Output>): StoreConfig<State, Input, Output>

    fun <State, Input : Any, Output> unregisterStore(store: StoreConfig<State, Input, Output>)
}
