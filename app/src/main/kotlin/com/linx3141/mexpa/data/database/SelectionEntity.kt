package com.linx3141.mexpa.data.database

import androidx.room3.Entity

@Entity(tableName = "selections", primaryKeys = ["uuid", "proxy"])
data class SelectionEntity(
    val uuid: String,
    val proxy: String,
    val selected: String,
)
