package com.jones.mvireaktive

import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.subject.Relay
import com.badoo.reaktive.subject.behavior.BehaviorSubject
import com.badoo.reaktive.subject.behavior.behaviorSubject
import com.badoo.reaktive.subject.publish.publishSubject
import com.jones.mvireaktive.middleware.Middleware
import com.jones.mvireaktive.middleware.MviAction
import kotlin.reflect.KClass

typealias PostProcessor<State, InputEvent, OutputEvent> = (State, InputEvent) -> OutputEvent?
typealias Bootstrapper<State, Event> = (State) -> Observable<Event>

data class StoreConfig<State, Event : Any, News>(
    val initialState: State,
    val events: Map<KClass<out Event>, MviAction<State, Event, out Event, News>>,
    val postActions: List<PostProcessor<State, Event, Event>>,
    val bootstrappers: List<Bootstrapper<State, Event>>,
    val statePublisher: BehaviorSubject<State> = behaviorSubject(initialState),
    val inputPublisher: Relay<Event> = publishSubject(),
    val newsPublisher: Relay<News> = publishSubject(),
    val middleware: List<Middleware>,
    val key: Any
)
