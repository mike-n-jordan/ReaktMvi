package com.jones.mvireaktive

import com.badoo.reaktive.disposable.CompositeDisposable
import com.badoo.reaktive.disposable.plusAssign
import com.badoo.reaktive.observable.*
import com.badoo.reaktive.scheduler.Scheduler
import com.jones.mvireaktive.middleware.Middleware
import com.jones.mvireaktive.middleware.MiddlewareConfig
import com.jones.mvireaktive.middleware.MiddlewareConfig.createStoreConfig
import com.jones.mvireaktive.middleware.MviAction

open class AbstractMviStore<State, Event : Any, News>(
    initialState: State,
    storeBuilder: StoreConfigBuilder<State, Event, News>.() -> Unit,
    internalScheduler: Scheduler = MiddlewareConfig.defaultInternalScheduler,
    localMiddleware: List<Middleware> = emptyList()
) : MviStore<State, Event, News> {

    internal val config: StoreConfig<State, Event, News> =
        StoreConfigBuilder<State, Event, News>()
            .apply { storeBuilder.invoke(this) }
            .createStoreConfig(
                key = this::class,
                initialState = initialState,
                localMiddleware = localMiddleware
            )

    private val inputPublisher = config.inputPublisher
    private val statePublisher = config.statePublisher
    private val newsPublisher = config.newsPublisher
    private val disposable = CompositeDisposable()

    override val state: State
        get() = statePublisher.value
    override val news: Observable<News> = newsPublisher

    init {
        disposable += inputPublisher
            .observeOn(internalScheduler)
            .flatMap { processInput(it) }
            .doOnBeforeError { statePublisher.onError(it) }
            .subscribe { inputPublisher.onNext(it) }

        disposable += config.bootstrappers
            .asObservable()
            .observeOn(internalScheduler)
            .flatMap { it.invoke(state) }
            .subscribe { inputPublisher.onNext(it) }
    }

    private fun processInput(event: Event): Observable<Event> {
        val config = config.actions[event::class] as MviAction<State, Event, Event, News>?
            ?: throw IllegalArgumentException("Unregistered class: ${event::class}")
        val shouldHandle = config.filter?.invoke(state, event) ?: true
        return if (shouldHandle) executeEvent(state, event, config) else observableOfEmpty()
    }

    private fun executeEvent(
        oldState: State,
        event: Event,
        mviAction: MviAction<State, Event, Event, News>
    ): Observable<Event> {
        val newState = mviAction.reducer?.invoke(oldState, event) ?: oldState
        if (newState !== oldState) {
            statePublisher.onNext(newState)
        }

        mviAction.news?.invoke(oldState, event)?.let { newsPublisher.onNext(it) }
        val actions = mviAction.action?.invoke(oldState, event) ?: observableOfEmpty()
        val eventPostActions = mviAction.post?.let {
            it.invoke(state, event)?.let { event -> observableOf(event) }
        } ?: observableOfEmpty()
        val postActions = config.postActions
            .mapNotNull { it.invoke(newState, event) }
            .asObservable()
        return eventPostActions.concatWith(postActions).concatWith(actions)
    }

    override fun subscribe(observer: ObservableObserver<State>) {
        statePublisher.subscribe(observer)
    }

    override fun onNext(value: Event) {
        if (!isDisposed) {
            config.inputPublisher.onNext(value)
        }
    }

    override val isDisposed: Boolean
        get() = disposable.isDisposed

    override fun dispose() {
        config.middleware.forEach { it.unregisterStore(config) }
        disposable.dispose()
    }
}
