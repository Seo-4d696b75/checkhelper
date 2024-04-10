package com.seo4d696b75.android.ekisagasu.data.database.station

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "root_node")
data class RootStationNodeEntity(
    @PrimaryKey
    val code: Int,
)
