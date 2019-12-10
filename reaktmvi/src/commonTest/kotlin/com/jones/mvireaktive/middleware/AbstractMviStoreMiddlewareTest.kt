package com.jones.mvireaktive.middleware

import com.badoo.reaktive.test.observable.assertValue
import com.badoo.reaktive.test.observable.test
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class AbstractMviStoreMiddlewareTest {

    @AfterTest
    fun cleanup() {
        MiddlewareConfig.clearMiddleware()
    }

    @Test
    fun `when a local middleware is provided, then it is used to wrap the store config`() {
        val middleware = listOf(TestMiddleware(), TestMiddleware())

        createMviStore(middleware = middleware) {  }

        middleware.forEach { assertTrue { it.stores.isNotEmpty() } }
    }

    @Test
    fun `when global middleware is provided, then it is used to wrap the store config`() {
        val middleware = TestMiddleware()
        MiddlewareConfig.registerGlobalMiddleware(middleware)

        createMviStore { }

        assertTrue { middleware.stores.isNotEmpty() }
    }

    @Test
    fun `when both global and local middleware are used, then apply both`() {
        val local = TestMiddleware()
        val global = TestMiddleware()
        MiddlewareConfig.registerGlobalMiddleware(global)

        createMviStore(middleware = listOf(local)) { }

        assertTrue { local.stores.isNotEmpty() }
        assertTrue { global.stores.isNotEmpty() }
    }

    @Test
    fun `when the store is disposed, then it unregisters from all middleware`() {
        val local = TestMiddleware()
        val global = TestMiddleware()
        MiddlewareConfig.registerGlobalMiddleware(global)
        val store = createMviStore(middleware = listOf(local)) { }

        store.dispose()

        assertTrue { local.stores.isEmpty() }
        assertTrue { global.stores.isEmpty() }
    }

    @Test
    fun `when the store has middleware, then all middleware may receive input`() {
        val local = TestMiddleware()
        val global = TestMiddleware()
        MiddlewareConfig.registerGlobalMiddleware(global)
        val store = createMviStore(middleware = listOf(local)) { on<Event.AddOne>() }

        val localTest = local.stores.values.map { it.inputPublisher.test() }
        val globalTest = global.stores.values.map { it.inputPublisher.test() }
        store.onNext(Event.AddOne)

        localTest.forEach { it.assertValue(Event.AddOne) }
        globalTest.forEach { it.assertValue(Event.AddOne) }
    }

    @Test
    fun `unregistering global middleware disposes it by default`() {
        val global = TestMiddleware()
        MiddlewareConfig.registerGlobalMiddleware(global)

        MiddlewareConfig.unregisterGlobalMiddleware(global)

        assertTrue(global.isDisposed)
    }
}
