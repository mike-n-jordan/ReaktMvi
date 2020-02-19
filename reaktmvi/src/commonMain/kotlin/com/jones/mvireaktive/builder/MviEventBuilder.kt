package com.jones.mvireaktive.builder

import com.jones.mvireaktive.*
import kotlin.reflect.KClass


class MviEventBuilder<State, Event : Any, BoundEvent : Event, NewsEvent>(
    private val eventClass: KClass<BoundEvent>
) : FullEventBuilder<State, Event, BoundEvent, NewsEvent> {

    internal var filter: Filter<State, BoundEvent>? = null
    internal var action: Action<State, BoundEvent, Event>? = null
    internal var reducer: Reducer<State, BoundEvent>? = null
    internal var news: NewsProcessor<State, BoundEvent, NewsEvent>? = null
    internal var post: PostProcessor<State, Event, Event>? = null

    override fun filter(filter: Filter<State, BoundEvent>?): ReduceEventBuilder<State, Event, BoundEvent, NewsEvent> {
        this.filter = filter
        return this
    }

    override fun reduce(reducer: Reducer<State, BoundEvent>?): ActionEventBuilder<State, Event, BoundEvent, NewsEvent> {
        this.reducer = reducer
        return this
    }

    override fun action(action: Action<State, BoundEvent, Event>?): NewsEventBuilder<State, Event, BoundEvent, NewsEvent> {
        this.action = action
        return this
    }

    override fun news(news: NewsProcessor<State, BoundEvent, NewsEvent>?): PostEventBuilder<State, Event, BoundEvent, NewsEvent> {
        this.news = news
        return this
    }

    override fun post(postEvent: PostProcessor<State, BoundEvent, Event>?): FinishedEventBuilder {
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
