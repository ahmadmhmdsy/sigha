package com.sigha.converter.data

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide scope shared by the preferences stores and the conversion queue.
 * It lives as long as the process: conversions must survive the ViewModel that started them.
 *
 * The handler is the last safety net: an exception that escapes an app-scoped coroutine (a
 * DataStore write, the batch runner, a callback) must not reach the default uncaught handler,
 * because that kills the whole app and leaves the queue without its cleanup path.
 */
internal val AppScope: CoroutineScope = CoroutineScope(
    SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        Log.e("Sigha", "Uncaught error in an app-scoped coroutine", error)
    }
)
