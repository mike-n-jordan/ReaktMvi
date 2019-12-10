package com.jones.mvireaktive

import com.badoo.reaktive.observable.asObservable
import com.badoo.reaktive.observable.observableOf
import com.jones.mvireaktive.middleware.MviActionBuilder
import kotlin.reflect.KClass

class StoreConfigBuilder<State, Event : Any, News> {

    internal val actions =
        mutableMapOf<KClass<out Event>, MviActionBuilder<State, Event, out Event, News>>()
    internal val postActions = mutableListOf<PostProcessor<State, Event, Event>>()
    internal val bootstrappers = mutableListOf<Bootstrapper<State, Event>>()

    inline fun <reified T : Event> on() = onClass(T::class)

    fun <T : Event> onClass(clazz: KClass<T>): MviActionBuilder<State, Event, T, News> {
        if (actions.contains(clazz)) {
            throw RuntimeException("Class: $clazz is already registered")
        }
        return MviActionBuilder<State, Event, T, News>(clazz)
            .also { actions[clazz] = it }
    }

    inline fun <reified T : Event> postEvent(crossinline onPost: PostProcessor<State, T, Event>) {
        post { state, event -> if (event is T) onPost(state, event) else null }
    }

    fun post(post: PostProcessor<State, Event, Event>) {
        postActions.add(post)
    }

    fun bootstrapWith(events: List<Event>) {
        bootstrap { events.asObservable() }
    }

    fun bootstrapWith(event: Event) {
        bootstrap { observableOf(event) }
    }

    fun bootstrap(bootstrapper: Bootstrapper<State, Event>) {
        bootstrappers.add(bootstrapper)
    }
}
