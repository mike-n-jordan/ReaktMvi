package com.jones.mvireaktive

import com.badoo.reaktive.observable.Observable

typealias Reducer<State, BoundEvent> = (State, BoundEvent) -> State
typealias Filter<State, BoundEvent> = (State, BoundEvent) -> Boolean
typealias Action<State, BoundEvent, Event> = (State, BoundEvent) -> Observable<Event>
typealias NewsProcessor<State, BoundEvent, NewsEvent> = (State, BoundEvent) -> NewsEvent?

data class MviAction<State, Event, BoundEvent, NewsEvent>(
    val filter: Filter<State, BoundEvent>?,
    val action: Action<State, BoundEvent, Event>?,
    val reducer: Reducer<State, BoundEvent>?,
    val news: NewsProcessor<State, BoundEvent, NewsEvent>?,
    val post: PostProcessor<State, Event, Event>?
)
