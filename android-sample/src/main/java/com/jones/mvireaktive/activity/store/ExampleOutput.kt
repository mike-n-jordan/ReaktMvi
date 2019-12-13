package com.jones.mvireaktive.activity.store

data class ExampleState(
    val count: Int = 0,
    val loadingSlowAdd: Boolean = false,
    val loadingSlowRemove: Boolean = false
)

sealed class News {
    object ResetEvent : News()
    object SlowAddOneSuccess : News()
    object SlowRemoveOneSuccess : News()
}
