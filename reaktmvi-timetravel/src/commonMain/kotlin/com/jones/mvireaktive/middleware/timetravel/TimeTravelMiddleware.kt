package com.jones.mvireaktive.middleware.timetravel

import com.badoo.reaktive.disposable.CompositeDisposable
import com.badoo.reaktive.disposable.plusAssign
import com.badoo.reaktive.observable.*
import com.badoo.reaktive.scheduler.mainScheduler
import com.badoo.reaktive.subject.Relay
import com.jones.mvireaktive.PostProcessor
import com.jones.mvireaktive.StoreConfig
import com.jones.mvireaktive.middleware.Middleware
import com.jones.mvireaktive.middleware.MviAction

class TimeTravelMiddleware() : Middleware {

    private val disposable = CompositeDisposable()
    internal val timeTravelStore = TimeTravelStore()

    init {
        disposable += timeTravelStore.news
            .subscribe {
                when (it) {
                    is News.ResetStore -> {
                        for (store in it.stores) {
                            val state = store.initialState
                            store.statePublisher.onNext(state)
                        }
                    }
                    is News.Playback -> {
                        it.relay.internalPublisher.onNext(it.event)
                    }
                }
            }
    }

    fun startPlayback() {
        disposable += observableOf(TimeTravelWish.StartPlayback)
            .concatWith(
                observableOf(TimeTravelWish.PlayNext)
                    .delay(500, mainScheduler)
                    .repeat()
                    .takeUntil { !timeTravelStore.state.isPlayingBack }
            )
            .subscribe { timeTravelStore.onNext(it) }
    }

    override fun <State, Input : Any, Output> wrapStore(
        store: StoreConfig<State, Input, Output>
    ): StoreConfig<State, Input, Output> {
        val timeTravelRelay = TimeTravelInputRelay(store)
        val newStore = store.copy(
            actions = store.actions.mapValues { wrapAction(it.value) },
            postActions = store.postActions.map { wrapPost(it) },
            inputPublisher = timeTravelRelay
        )
        timeTravelStore.onNext(
            RegisterStore(
                store as StoreConfig<Any, Any, *>,
                timeTravelRelay as TimeTravelInputRelay<Any>
            )
        )
        return newStore
    }

    override fun <State, Input : Any, Output> unregisterStore(
        store: StoreConfig<State, Input, Output>
    ) {
        timeTravelStore.onNext(UnregisterStore(store))
    }

    private fun <State, Input, BoundInput : Input, Output> wrapAction(
        action: MviAction<State, Input, BoundInput, Output>
    ) = action.copy(
        action = { state, event ->
            action.action?.invoke(state, event).takeIf { !isPlayingBack() }
                ?: observableOfEmpty()
        }
    )

    private fun <State, Input> wrapPost(post: PostProcessor<State, Input, Input>) =
        object : PostProcessor<State, Input, Input> {
            override fun invoke(state: State, input: Input): Input? =
                post.invoke(state, input).takeIf { !isPlayingBack() }
        }

    private fun isPlayingBack(): Boolean =
        !isDisposed && timeTravelStore.state.isPlayingBack

    private fun isRecording(): Boolean =
        !isDisposed && timeTravelStore.state.isRecording

    override val isDisposed: Boolean
        get() = disposable.isDisposed

    override fun dispose() {
        timeTravelStore.dispose()
        disposable.dispose()
    }

    inner class TimeTravelInputRelay<T : Any>(
        private val store: StoreConfig<*, T, *>
    ) : Relay<T> {

        val internalPublisher = store.inputPublisher

        override fun onNext(value: T) {
            if (!isPlayingBack()) {
                internalPublisher.onNext(value)
            }
            if (isRecording()) {
                timeTravelStore.onNext(RecordEvent(store.key, value))
            }
        }

        override fun subscribe(observer: ObservableObserver<T>) {
            internalPublisher.subscribe(observer)
        }
    }
}
