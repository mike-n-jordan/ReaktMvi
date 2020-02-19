package com.jones.mvireaktive.builder

import com.jones.mvireaktive.*

interface FullEventBuilder<State, Event : Any, BoundEvent : Event, NewsEvent>
    : FilterEventBuilder<State, Event, BoundEvent, NewsEvent>

interface FilterEventBuilder<State, Event : Any, BoundEvent : Event, NewsEvent>
    : ReduceEventBuilder<State, Event, BoundEvent, NewsEvent> {

    fun filter(filter: Filter<State, BoundEvent>?): ReduceEventBuilder<State, Event, BoundEvent, NewsEvent>
}

interface ReduceEventBuilder<State, Event : Any, BoundEvent : Event, NewsEvent>
    : ActionEventBuilder<State, Event, BoundEvent, NewsEvent> {

    fun reducer(reduce: Reducer<State, BoundEvent>?): ActionEventBuilder<State, Event, BoundEvent, NewsEvent>
}

interface ActionEventBuilder<State, Event : Any, BoundEvent : Event, NewsEvent>
    : NewsEventBuilder<State, Event, BoundEvent, NewsEvent> {

    fun actor(action: Action<State, BoundEvent, Event>?): NewsEventBuilder<State, Event, BoundEvent, NewsEvent>
}

interface NewsEventBuilder<State, Event : Any, BoundEvent : Event, NewsEvent>
    : PostEventBuilder<State, Event, BoundEvent, NewsEvent> {

    fun newsPublisher(news: NewsProcessor<State, BoundEvent, NewsEvent>?): PostEventBuilder<State, Event, BoundEvent, NewsEvent>
}

interface PostEventBuilder<State, Event : Any, BoundEvent : Event, NewsEvent> : FinishedEventBuilder {

    fun postEventPublisher(postEvent: PostProcessor<State, BoundEvent, Event>?): FinishedEventBuilder
}

interface FinishedEventBuilder
