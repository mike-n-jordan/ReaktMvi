package com.jones.mvireaktive.middleware

import com.jones.mvireaktive.PostProcessor
import kotlin.reflect.KClass


class MviActionBuilder<State, Event : Any, BoundEvent : Event, NewsEvent>(
    private val eventClass: KClass<BoundEvent>
) {

    internal var filter: Filter<State, BoundEvent>? = null
    internal var action: Action<State, BoundEvent, Event>? = null
    internal var reducer: Reducer<State, BoundEvent>? = null
    internal var news: NewsProcessor<State, BoundEvent, NewsEvent>? = null
    internal var post: PostProcessor<State, Event, Event>? = null

    fun reduce(reducer: Reducer<State, BoundEvent>): MviActionBuilder<State, Event, BoundEvent, NewsEvent> {
        this.reducer = reducer
        return this
    }

    fun filter(filter: Filter<State, BoundEvent>): MviActionBuilder<State, Event, BoundEvent, NewsEvent> {
        this.filter = filter
        return this
    }

    fun action(action: Action<State, BoundEvent, Event>): MviActionBuilder<State, Event, BoundEvent, NewsEvent> {
        this.action = action
        return this
    }

    fun news(news: NewsProcessor<State, BoundEvent, NewsEvent>): MviActionBuilder<State, Event, BoundEvent, NewsEvent> {
        this.news = news
        return this
    }

    fun post(postEvent: PostProcessor<State, BoundEvent, Event>): MviActionBuilder<State, Event, BoundEvent, NewsEvent> {
        this.post = { state, event -> if (eventClass.isInstance(event)) postEvent(state, event as BoundEvent) else null }
        return this
    }
}
