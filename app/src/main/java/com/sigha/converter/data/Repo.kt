package com.sigha.converter.data

import com.sigha.converter.AppGraph
import com.sigha.converter.work.ConversionQueue

/**
 * Single place where app-scoped services are created. Lazy singletons keep cold start cheap;
 * they are created only after [AppGraph] was initialised in Application.onCreate().
 */
object Repo {
    val settings: SettingsStore by lazy { SettingsStore(AppGraph.app) }
    val history: HistoryStore by lazy { HistoryStore(AppGraph.app) }
    val publisher: OutputPublisher by lazy { OutputPublisher(AppGraph.app) }
    val runner: ConversionRunner by lazy { ConversionRunner(AppGraph.app, publisher) }
    val queue: ConversionQueue by lazy { ConversionQueue(AppGraph.app, runner, history, settings) }
    val packs: ModelPackStore by lazy { ModelPackStore(AppGraph.app) }
}
