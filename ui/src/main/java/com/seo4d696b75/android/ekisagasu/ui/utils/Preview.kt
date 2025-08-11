package com.seo4d696b75.android.ekisagasu.ui.utils

import com.seo4d696b75.android.ekisagasu.domain.dataset.ColorInt
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Prefecture
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.dataset.StationRegistration
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import java.util.Date

val previewLine = Line(
    id = 1,
    code = 10001,
    name = "東海道新幹線",
    closed = false,
    nameKana = "とうかいどうしんかんせん",
    stationSize = 17,
    color = ColorInt.from("#0000FF"),
    stationList = listOf(
        StationRegistration(1),
        StationRegistration(2),
        StationRegistration(3),
    ),
)

val previewLines = listOf(
    Line(
        id = 1,
        code = 10001,
        name = "東海道新幹線",
        closed = false,
        nameKana = "とうかいどうしんかんせん",
        stationSize = 17,
        stationList = emptyList(),
        color = ColorInt.from("#0000FF"),
    ),
    Line(
        id = 2,
        code = 1004,
        name = "東北新幹線",
        closed = false,
        nameKana = "とうほくしんかんせん",
        stationSize = 23,
        stationList = emptyList(),
        color = ColorInt.from("#008000"),
    ),
    Line(
        id = 3,
        code = 11301,
        name = "JR東海道本線(東京～熱海)",
        closed = false,
        nameKana = "じぇいあーるとうかいどうほんせん",
        stationSize = 21,
        stationList = emptyList(),
        color = ColorInt.from("#F68B1E"),
        symbol = "JT",
    ),
    Line(
        id = 4,
        code = 11332,
        name = "JR京浜東北線",
        closed = false,
        nameKana = "じぇいあーるけいひんとうほくせん",
        stationSize = 36,
        stationList = emptyList(),
        color = ColorInt.from("#00B2E5"),
        symbol = "JK",
    ),
    Line(
        id = 5,
        code = 11302,
        name = "JR山手線",
        closed = false,
        nameKana = "じぇいあーるやまのてせん",
        stationSize = 36,
        stationList = emptyList(),
        color = ColorInt.from("#9ACD32"),
        symbol = "JY",
    ),
)

val previewStation = Station(
    id = 1,
    code = 100101,
    name = "東京",
    originalName = "東京",
    nameKana = "とうきょう",
    lines = previewLines,
    lat = 45.5,
    lng = 135.0,
    prefecture = Prefecture(13, "東京都"),
    closed = false,
    voronoi = "",
)

val previewStation2 = Station(
    id = 2,
    code = 100102,
    name = "品川",
    originalName = "品川",
    nameKana = "しながわ",
    lines = previewLines,
    lat = 45.5,
    lng = 135.0,
    prefecture = Prefecture(13, "東京都"),
    closed = false,
    voronoi = "",
)

val previewStation3 = Station(
    id = 3,
    code = 100103,
    name = "新横浜",
    originalName = "新横浜",
    nameKana = "しんよこはま",
    lines = previewLines,
    lat = 45.5,
    lng = 135.0,
    prefecture = Prefecture(14, "神奈川県"),
    closed = false,
    voronoi = "",
)

val previewStations = listOf(
    previewStation,
    previewStation2,
    previewStation3,
)

val previewNearStation = NearStation(
    station = previewStation,
    distance = 123f,
    time = Date(),
)
