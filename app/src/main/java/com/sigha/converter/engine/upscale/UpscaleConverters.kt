package com.sigha.converter.engine.upscale

import android.app.Application
import com.sigha.converter.core.Converter

/**
 * AI upscaling is deliberately not part of this build.
 *
 * The screen advertises it as "coming soon", and enabling it needs more than a model file:
 * a commercially usable multi-file ONNX model, tiled inference, a dedicated UI choice and a
 * way to tell "scale this image" apart from an ordinary image->PNG conversion in the route
 * planner. Shipping a half-wired edge would either hijack normal PNG conversions or fail at
 * the last step of a long route, so this entry point stays empty until that work lands.
 *
 * The verified model candidate, licence, URLs, sizes and SHA-256 values are recorded in
 * docs/models/integration.md; when upscaling is enabled, this function returns the
 * image->PNG converter instead of an empty list and nothing else has to change.
 */
@Suppress("UNUSED_PARAMETER")
fun upscaleConverters(app: Application): List<Converter> = emptyList()
