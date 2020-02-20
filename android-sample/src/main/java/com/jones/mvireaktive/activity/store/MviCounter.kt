package com.jones.mvireaktive.activity.store

interface MviCounterWish {
    object AddOne : MviCounterWish
    object RemoveOne : MviCounterWish
    object SlowAddOne : MviCounterWish
    object SlowRemoveOne : MviCounterWish
}

data class MviCounterState(
    val count: Int = 0,
    val loadingSlowAdd: Boolean = false,
    val loadingSlowRemove: Boolean = false
)

sealed class News {
    object ResetEvent : News()
    object SlowAddOneSuccess : News()
    object SlowRemoveOneSuccess : News()
}
