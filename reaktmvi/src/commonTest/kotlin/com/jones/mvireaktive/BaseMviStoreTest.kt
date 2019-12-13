package com.jones.mvireaktive

import com.badoo.reaktive.observable.observableOf
import com.badoo.reaktive.subject.publish.publishSubject
import com.badoo.reaktive.test.base.assertError
import com.badoo.reaktive.test.observable.assertNoValues
import com.badoo.reaktive.test.observable.assertValue
import com.badoo.reaktive.test.observable.assertValues
import com.badoo.reaktive.test.observable.test
import com.badoo.reaktive.test.scheduler.TestScheduler
import com.jones.mvireaktive.middleware.Event
import com.jones.mvireaktive.middleware.State
import com.jones.mvireaktive.middleware.createMviStore
import kotlin.test.Test
import kotlin.test.assertEquals

class BaseMviStoreTest {

    @Test
    fun `when first subscribing to state, emit current state`() {
        val store = createMviStore { }

        val observer = store.test()

        observer.assertValue(State())
    }

    @Test
    fun `when an unregistered event is passed to the store, then throw an exception`() {
        val store = createMviStore {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
        }

        val observer = store.test()
        store.onNext(Event.Other)

        observer.assertError()
    }

    @Test
    fun `when store is disposed, stop accepting input`() {
        val store = createMviStore {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
        }

        val observer = store.test().also { it.reset() }
        store.dispose()
        store.onNext(Event.AddOne)

        observer.assertNoValues()
    }

    @Test
    fun `when an event is sent to the store, it is processed on the internal scheduler`() {
        val testScheduler = TestScheduler(isManualProcessing = true)
        val store = createMviStore(testScheduler) {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.AddOne)
        observer.assertNoValues()
        testScheduler.process()

        observer.assertValue(State(count = 1))
    }

    @Test
    fun `when the wish is filtered out perform no action`() {
        val store = createMviStore {
            on<Event.AddOne>()
                .filter { state, addOne -> false }
                .reduce { state, addOne -> throw RuntimeException() }
                .action { state, addOne -> throw RuntimeException() }
            post { state, event -> throw RuntimeException() }
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.AddOne)

        observer.assertNoValues()
    }

    @Test
    fun `when the wish is not filtered out, state is updated through the reducer`() {
        val store = createMviStore {
            on<Event.AddOne>()
                .filter { state, addOne -> true }
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.AddOne)

        observer.assertValue(State(count = 1))
    }

    @Test
    fun `when a wish has an associated action, then perform the action`() {
        val store = createMviStore {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            on<Event.SlowAddOne>()
                .reduce { state, slowAddOne -> state.copy(slowAddCount = state.slowAddCount + 1) }
                .action { state, slowAddOne -> observableOf(Event.AddOne) }
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.SlowAddOne)

        observer.assertValues(State(slowAddCount = 1), State(count = 1, slowAddCount = 1))
    }

    @Test
    fun `when a wish has a delayed action, then emit state without waiting for action to complete`() {
        val eventPublisher = publishSubject<Event.AddOne>()
        val store = createMviStore {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            on<Event.SlowAddOne>()
                .reduce { state, slowAddOne -> state.copy(slowAddCount = state.slowAddCount + 1) }
                .action { state, slowAddOne -> eventPublisher }
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.SlowAddOne)
        store.onNext(Event.AddOne)

        observer.assertValues(State(slowAddCount = 1), State(slowAddCount = 1, count = 1))
    }

    @Test
    fun `when a wish has a delayed action, then emit state when it is published`() {
        val eventPublisher = publishSubject<Event.AddOne>()
        val store = createMviStore {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            on<Event.SlowAddOne>()
                .reduce { state, slowAddOne -> state.copy(slowAddCount = state.slowAddCount + 1) }
                .action { state, slowAddOne -> eventPublisher }
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.SlowAddOne)
        store.onNext(Event.AddOne)
        eventPublisher.onNext(Event.AddOne)

        observer.assertValues(
            State(slowAddCount = 1),
            State(slowAddCount = 1, count = 1),
            State(slowAddCount = 1, count = 2)
        )
    }

    @Test
    fun `when event specific post processor is set, then it fires for event`() {
        val store = createMviStore {
            on<Event.SlowAddOne>()
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            postEvent<Event.SlowAddOne> { state, event -> Event.AddOne }
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.SlowAddOne)

        observer.assertValues(State(count = 1))
    }

    @Test
    fun `when event specific post processor is set, then it isn't fired for other events`() {
        val store = createMviStore {
            on<Event.SlowAddOne>()
            on<Event.Other>()
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            postEvent<Event.SlowAddOne> { state, event -> Event.AddOne }
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.Other)

        observer.assertNoValues()
    }

    @Test
    fun `when general post event is set, then it fires for all events`() {
        val store = createMviStore {
            on<Event.Other>()
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            post { state, event -> Event.AddOne }
        }

        store.onNext(Event.Other)
        store.onNext(Event.AddOne)

        assertEquals(expected = 2, actual = store.state.count)
    }

    @Test
    fun `when builder post event is set, then it is fired for bound event`() {
        val store = createMviStore {
            on<Event.Other>()
                .post { state, other -> Event.AddOne }
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            on<Event.SlowAddOne>()
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.Other)

        observer.assertValues(
            listOf(
                State(count = 1)
            )
        )
    }

    @Test
    fun `when builder post event is set, then it isn't fired for all events`() {
        val store = createMviStore {
            on<Event.Other>()
                .post { state, other -> Event.AddOne }
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            on<Event.SlowAddOne>()
        }
        val observer = store.test().also { it.reset() }

        store.onNext(Event.SlowAddOne)

        observer.assertNoValues()
    }

    @Test
    fun `when bootstrapper is provided with a single event, fulfill action`() {
        val store = createMviStore {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            bootstrapWith(Event.AddOne)
        }

        val observer = store.test()

        observer.assertValue(State(count = 1))
    }

    @Test
    fun `when bootstrapper is set via observable, then fulfill action`() {
        val store = createMviStore {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            bootstrap { observableOf(Event.AddOne, Event.AddOne) }
        }

        val observer = store.test()

        observer.assertValue(State(count = 2))
    }

    @Test
    fun `when multiple bootstrappers are set, then fulfill all actions`() {
        val store = createMviStore {
            on<Event.AddOne>()
                .reduce { state, addOne -> state.copy(count = state.count + 1) }
            bootstrap { observableOf(Event.AddOne, Event.AddOne) }
            bootstrapWith(Event.AddOne)
            bootstrapWith(listOf(Event.AddOne, Event.AddOne))
            bootstrap { observableOf(Event.AddOne) }
        }

        val observer = store.test()

        observer.assertValue(State(count = 6))
    }
}
