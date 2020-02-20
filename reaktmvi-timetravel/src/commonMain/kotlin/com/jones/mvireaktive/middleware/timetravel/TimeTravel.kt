package com.jones.mvireaktive.middleware.timetravel

import com.jones.mvireaktive.StoreConfig

interface TimeTravelWish {
    object StartPlayback : TimeTravelWish
    object PlayNext : TimeTravelWish
}

data class State(
    val isRecording: Boolean = true,
    val isPlayingBack: Boolean = false,
    val registeredRelays: Map<Any, TimeTravelMiddleware.TimeTravelInputRelay<Any>> = emptyMap(),
    val registeredStores: List<StoreConfig<Any, Any, *>> = emptyList(),
    val recordedEvents: List<Pair<Any, Any>> = emptyList(),
    val nextEventIndex: Int = -1
)

sealed class News {
    internal data class Playback(val relay: TimeTravelMiddleware.TimeTravelInputRelay<Any>, val event: Any) : News()
    internal class ResetStore(val stores: List<StoreConfig<Any, *, *>>) : News()
}
