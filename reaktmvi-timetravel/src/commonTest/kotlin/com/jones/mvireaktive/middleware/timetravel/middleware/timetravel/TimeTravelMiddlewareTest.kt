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
}
