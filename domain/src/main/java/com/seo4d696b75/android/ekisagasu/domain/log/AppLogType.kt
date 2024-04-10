package com.seo4d696b75.android.ekisagasu.domain.log

enum class AppLogType(val value: Int) {
    System(0b001),
    Location(0b010),
    Station(0b100);

    enum class Filter(val value: Int) {
        All(0b111),
        System(0b001),
        Geo(0b110),
        Station(0b100)
    }
}
