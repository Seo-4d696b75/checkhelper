package com.seo4d696b75.android.ekisagasu.ui.utils

import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import java.util.Date

val previewLine = Line(
    id = "id",
    code = 10001,
    name = "東海道新幹線",
    closed = false,
    nameKana = "とうかいどうしんかんせん",
    stationSize = 17,
    stationList = emptyArray(),
)

val previewLines = listOf(
    Line(
        id = "0096b0",
        code = 10001,
        name = "東海道新幹線",
        closed = false,
        nameKana = "とうかいどうしんかんせん",
        stationSize = 17,
        stationList = emptyArray(),
        color = "#0000FF",
    ),
    Line(
        id = "a719bd",
        code = 1004,
        name = "東北新幹線",
        closed = false,
        nameKana = "とうほくしんかんせん",
        stationSize = 23,
        stationList = emptyArray(),
        color = "#008000",
    ),
    Line(
        id = "ff8c6a",
        code = 11301,
        name = "JR東海道本線(東京～熱海)",
        closed = false,
        nameKana = "じぇいあーるとうかいどうほんせん",
        stationSize = 21,
        stationList = emptyArray(),
        color = "#F68B1E",
        symbol = "JT",
    ),
    Line(
        id = "69d2d0",
        code = 11332,
        name = "JR京浜東北線",
        closed = false,
        nameKana = "じぇいあーるけいひんとうほくせん",
        stationSize = 36,
        stationList = emptyArray(),
        color = "#00B2E5",
        symbol = "JK",
    ),
    Line(
        id = "39b88f",
        code = 11302,
        name = "JR山手線",
        closed = false,
        nameKana = "じぇいあーるやまのてせん",
        stationSize = 36,
        stationList = emptyArray(),
        color = "#9ACD32",
        symbol = "JY",
    ),
)

val previewStation = Station(
    id = "1",
    code = 1,
    name = "東京",
    originalName = "東京",
    nameKana = "とうきょう",
    lines = listOf(1),
    lat = 45.5,
    lng = 135.0,
    prefecture = 13,
    closed = false,
    voronoi = "",
    attr = "",
)

val previewNearStation = NearStation(
    station = previewStation,
    distance = 123f,
    time = Date(),
    lines = previewLines,
)
