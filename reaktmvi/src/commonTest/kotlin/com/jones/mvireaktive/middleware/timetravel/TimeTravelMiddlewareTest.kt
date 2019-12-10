package com.jones.mvireaktive.middleware.timetravel

import com.badoo.reaktive.scheduler.trampolineScheduler
import com.badoo.reaktive.test.observable.assertValue
import com.badoo.reaktive.test.observable.assertValues
import com.badoo.reaktive.test.observable.test
import com.jones.mvireaktive.AbstractMviStore
import com.jones.mvireaktive.middleware.MiddlewareConfig
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TimeTravelMiddlewareTest {

    private val timeTravelMiddleware = TimeTravelMiddleware()

    @BeforeTest
    fun setup() {
        MiddlewareConfig.defaultInternalScheduler = trampolineScheduler
        MiddlewareConfig.clearMiddleware()
        MiddlewareConfig.registerGlobalMiddleware(timeTravelMiddleware)
    }

    @Test
    fun `test`() {
        val store = DummyStoreOne()

        store.onNext(Any())
        val observer = store.test().also { it.reset() }
        store.dispose()


    }

    private class DummyStoreOne(
    ) : AbstractMviStore<Any, Any, Any>(
        Any(),
        {
            on<Any>()
                .reduce { any, any2 -> Any() }
        }
    )

    private class DummyStoreTwo(
    ) : AbstractMviStore<Any, Any, Any>(
        Any(),
        {
            on<Any>()
                .reduce { any, any2 -> Any() }
        }
    )
}
