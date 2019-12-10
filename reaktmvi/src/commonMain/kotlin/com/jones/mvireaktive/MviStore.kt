package com.jones.mvireaktive

import com.badoo.reaktive.base.Consumer
import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.Observable

interface MviStore<State, Event, News> : Observable<State>, Consumer<Event>, Disposable {

    val state: State
    val news: Observable<News>
}
