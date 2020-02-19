package com.jones.mvireaktive

import kotlin.reflect.KClass


class MviEventBuilder<State, Event : Any, BoundEvent : Event, NewsEvent>(
    private val eventClass: KClass<BoundEvent>
) {

    internal var filter: Filter<State, BoundEvent>? = null
    internal var action: Action<State, BoundEvent, Event>? = null
    internal var reducer: Reducer<State, BoundEvent>? = null
    internal var news: NewsProcessor<State, BoundEvent, NewsEvent>? = null
    internal var post: PostProcessor<State, Event, Event>? = null

    fun reduce(reducer: Reducer<State, BoundEvent>?): MviEventBuilder<State, Event, BoundEvent, NewsEvent> {
        this.reducer = reducer
        return this
    }

    fun filter(filter: Filter<State, BoundEvent>?): MviEventBuilder<State, Event, BoundEvent, NewsEvent> {
        this.filter = filter
        return this
    }

    fun action(action: Action<State, BoundEvent, Event>?): MviEventBuilder<State, Event, BoundEvent, NewsEvent> {
        this.action = action
        return this
    }

    fun news(news: NewsProcessor<State, BoundEvent, NewsEvent>?): MviEventBuilder<State, Event, BoundEvent, NewsEvent> {
        this.news = news
        return this
    }

    fun post(postEvent: PostProcessor<State, BoundEvent, Event>?): MviEventBuilder<State, Event, BoundEvent, NewsEvent> {
        if (postEvent != null) {
            this.post = { state, event ->
                if (eventClass.isInstance(event)) postEvent(
                    state,
                    event as BoundEvent
                ) else null
            }
        }
        return this
    }
}
