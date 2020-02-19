package com.jones.mvireaktive.middleware.timetravel

import com.badoo.reaktive.observable.observableOf
import com.badoo.reaktive.observable.observableOfEmpty
import com.jones.mvireaktive.BaseMviStore
import com.jones.mvireaktive.StoreConfig
import com.jones.mvireaktive.StoreConfigBuilder
import com.jones.mvireaktive.middleware.timetravel.TimeTravelMiddleware.TimeTravelInputRelay

data class State(
    val isRecording: Boolean = true,
    val isPlayingBack: Boolean = false,
    val registeredRelays: Map<Any, TimeTravelInputRelay<Any>> = emptyMap(),
    val registeredStores: List<StoreConfig<Any, Any, *>> = emptyList(),
    val recordedEvents: List<Pair<Any, Any>> = emptyList(),
    val nextEventIndex: Int = -1
)

sealed class TimeTravelWish {
    object StartPlayback : TimeTravelWish()
    object PlayNext : TimeTravelWish()
}

internal class RecordEvent<T : Any>(val key: Any, val event: T) : TimeTravelWish()
internal class RegisterStore(
    val store: StoreConfig<Any, Any, *>,
    val relay: TimeTravelInputRelay<Any>
) : TimeTravelWish()
internal class UnregisterStore(val store: StoreConfig<*, *, *>) : TimeTravelWish()
private class EmitEvent(val key: Any, val event: Any) : TimeTravelWish()
private object StartRecording : TimeTravelWish()
private object StopRecording : TimeTravelWish()
private object StopPlayback : TimeTravelWish()

sealed class News {
    internal data class Playback(val relay: TimeTravelInputRelay<Any>, val event: Any) : News()
    internal class ResetStore(val stores: List<StoreConfig<Any, *, *>>) : News()
}

class TimeTravelStore : BaseMviStore<State, TimeTravelWish, News>(
    initialState = State(),
    storeBuilder = {
        wireRegisterEvents()
        wireRecordingEvents()
        wirePlaybackEvents()
    }
)

private fun StoreConfigBuilder<State, TimeTravelWish, News>.wireRegisterEvents() {
    on<RegisterStore>()
        .reducer { state, registerStore ->
            state.copy(
                registeredRelays = state.registeredRelays.plus(
                    registerStore.store.key to registerStore.relay
                ),
                registeredStores = state.registeredStores.plus(registerStore.store)
            )
        }
    on<UnregisterStore>()
        .reducer { state, unregisterStore ->
            state.copy(
                registeredRelays = state.registeredRelays.minus(unregisterStore.store.key),
                registeredStores = state.registeredStores
                    .filter { it.key !== unregisterStore.store.key}
            )
        }
}

private fun StoreConfigBuilder<State, TimeTravelWish, News>.wireRecordingEvents() {
    on<StartRecording>()
        .reducer { state, startRecording ->
            state.copy(
                isRecording = true
            )
        }
    on<StopRecording>()
        .reducer { state, stopRecording -> state.copy(isRecording = false) }
    on<RecordEvent<Any>>()
        .filter { state, recordEvent -> !(recordEvent.event is StopPlayback) }
        .reducer { state, recordEvent ->
            state.copy(
                recordedEvents = state.recordedEvents
                    .plus(recordEvent.key to recordEvent.event)
            )
        }
}

private fun StoreConfigBuilder<State, TimeTravelWish, News>.wirePlaybackEvents() {
    on<TimeTravelWish.StartPlayback>()
        .filter { state, startPlayback -> !state.isPlayingBack }
        .reducer { state, startPlayback ->
            state.copy(
                isPlayingBack = true,
                nextEventIndex = 0
            )
        }
        .actor { state, startPlayback -> observableOf(StopRecording) }
        .newsPublisher { state, startPlayback -> News.ResetStore(state.registeredStores) }

    on<TimeTravelWish.PlayNext>()
        .filter { state, playNext -> state.isPlayingBack }
        .actor { state, playNext ->
            val index = if (state.nextEventIndex < 0) 0 else state.nextEventIndex
            if (index < state.recordedEvents.size) {
                val event = state.recordedEvents[index]
                observableOf(EmitEvent(event.first, event.second))
            } else {
                observableOfEmpty()
            }
        }

    on<EmitEvent>()
        .reducer { state, emitEvent -> state.copy(nextEventIndex = state.nextEventIndex + 1) }
        .newsPublisher { state, emitEvent ->
            val relay = state.registeredRelays[emitEvent.key]
            relay?.let { News.Playback(it, emitEvent.event) }
        }
        .postEventPublisher { state, emitEvent -> StopPlayback.takeIf { state.nextEventIndex >= state.recordedEvents.size } }
    
    on<StopPlayback>()
        .filter { state, stopPlayback -> state.isPlayingBack }
        .reducer { state, stopPlayback ->
            state.copy(
                isPlayingBack = false,
                nextEventIndex = -1
            )
        }
        .actor { state, stopPlayback -> observableOf(StartRecording) }
        .postEventPublisher { state, stopPlayback -> StartRecording }
}
