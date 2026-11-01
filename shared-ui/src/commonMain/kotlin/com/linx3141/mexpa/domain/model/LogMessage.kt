package com.linx3141.mexpa.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LogMessage(
    val type: String = "",
    val payload: String = "",
)
