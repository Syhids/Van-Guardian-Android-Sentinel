package io.syhids.vanguardian.sentinel.ml

/** Abstraction object that wraps a classification output in an easy to parse way */
data class Recognition(val id: String, val title: String, val confidence: Float)