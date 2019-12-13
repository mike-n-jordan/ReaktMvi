package com.jones.mvireaktive

import com.badoo.reaktive.base.Consumer
import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.Observable

interface MviStore<out State, in Event, out News> : Observable<State>, Consumer<Event>, Disposable {

    val state: State
    val news: Observable<News>
}
