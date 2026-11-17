package com.sigha.converter.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide scope shared by the preferences stores and the conversion queue.
 * It lives as long as the process: conversions must survive the ViewModel that started them.
 */
internal val AppScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
