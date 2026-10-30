package com.linx3141.mexpa.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MemoryData(
    val inuse: Long = 0,
    val oslimit: Long = 0,
)
