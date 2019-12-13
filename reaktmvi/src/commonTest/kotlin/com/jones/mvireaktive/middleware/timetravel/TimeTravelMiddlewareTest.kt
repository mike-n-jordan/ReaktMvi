package com.jones.mvireaktive.middleware.timetravel

import com.badoo.reaktive.scheduler.trampolineScheduler
import com.badoo.reaktive.test.observable.test
import com.jones.mvireaktive.BaseMviStore
import com.jones.mvireaktive.middleware.MiddlewareConfig
import kotlin.test.BeforeTest
import kotlin.test.Test

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
    ) : BaseMviStore<Any, Any, Any>(
        Any(),
        {
            on<Any>()
                .reduce { any, any2 -> Any() }
        }
    )

    private class DummyStoreTwo(
    ) : BaseMviStore<Any, Any, Any>(
        Any(),
        {
            on<Any>()
                .reduce { any, any2 -> Any() }
        }
    )
}
