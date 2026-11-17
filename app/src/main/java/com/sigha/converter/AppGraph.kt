package com.sigha.converter

import android.app.Application
import com.sigha.converter.core.ConverterRegistry
import com.sigha.converter.core.FormatRegistry
import com.sigha.converter.core.RoutePlanner
import com.sigha.converter.engine.buildBuiltInConverters
import com.sigha.converter.model.ModelPacks

/**
 * Tiny manual DI graph (no reflection, fast cold start, testable).
 */
object AppGraph {
    lateinit var app: Application
        private set

    lateinit var formats: FormatRegistry
        private set

    lateinit var registry: ConverterRegistry
        private set

    fun init(application: Application) {
        app = application
        ModelPacks.attach(application)
        formats = FormatRegistry()
        registry = ConverterRegistry(formats).also { reg ->
            buildBuiltInConverters(application).forEach(reg::register)
        }
    }

    val planner: RoutePlanner by lazy { RoutePlanner(registry) }
}
