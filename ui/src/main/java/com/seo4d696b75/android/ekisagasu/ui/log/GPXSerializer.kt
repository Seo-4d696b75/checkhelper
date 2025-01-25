package com.seo4d696b75.android.ekisagasu.ui.log

import com.seo4d696b75.android.ekisagasu.domain.config.AppConfig
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_ISO8601_EXTEND
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.domain.log.filter
import com.seo4d696b75.android.ekisagasu.domain.xml.XMLSerializer
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import java.util.Date
import javax.inject.Inject

class GPXSerializer @Inject constructor(
    private val config: AppConfig,
    private val xmlSerializer: XMLSerializer,
    private val dataRepository: DataRepository,
) {
    suspend operator fun invoke(log: List<AppLog>, dst: OutputStream) {
        val appName = config.appName
        val appVersionName = config.versionName
        val deviceName = config.deviceName
        val dataVersion =
            dataRepository.dataVersion.first()?.version ?: throw IllegalStateException("data version not found")
        val points = log.toTrackSegment()

        with(xmlSerializer) {
            dst.writeXML(
                encoding = "UTF-8",
                standalone = true,
                rootTagName = "gpx",
            ) {
                attribute(
                    "xmlns" to "http://www.topografix.com/GPX/1/1",
                    "creator" to appName,
                    "version" to "1.1",
                    "xmlns:xsi" to "http://www.w3.org/2001/XMLSchema-instance",
                    "xsi:schemaLocation" to "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd",
                )

                tag("metadata") {
                    tag("name") {
                        text("ログ出力")
                    }
                    tag("author") {
                        tag("name") {
                            text("$appName $appVersionName")
                        }
                    }
                    tag("time") {
                        val now = Date()
                        text(now.format(TIME_PATTERN_ISO8601_EXTEND))
                    }
                }

                tag("trk") {
                    tag("name") {
                        text("位置履歴")
                    }
                    tag("src") {
                        text(deviceName)
                    }
                    tag("desc") {
                        text("collected by Fused Location Provider(com.google.android.gms:play-services-location)")
                    }
                    tag("link") {
                        attribute(
                            "href" to "https://developers.google.com/location-context/fused-location-provider",
                        )
                    }
                    tag("extensions") {
                        tag("station") {
                            tag("version") {
                                text(dataVersion.toString())
                            }
                        }
                    }
                    tag("trkseg") {
                        points.forEach { p ->
                            tag("trkpt") {
                                attribute(
                                    "lat" to p.lat,
                                    "lon" to p.lng,
                                )
                                tag("time") {
                                    text(p.time)
                                }
                                p.station?.let {
                                    tag("extensions") {
                                        tag("station") {
                                            attribute("code" to it.code)
                                            text(it.name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<AppLog>.toTrackSegment(): Iterator<TrackPoint> =
    object : Iterator<TrackPoint> {
        private val list = this@toTrackSegment.filter(AppLogType.Filter.Geo)
        private var index = 0

        override fun hasNext(): Boolean {
            return index < list.size
        }

        override fun next(): TrackPoint {
            val i = index
            require(list[i].type == AppLogType.Location)
            val next = if (i + 1 < list.size) list[i + 1] else null
            return if (next != null && next.type == AppLogType.Station) {
                index += 2
                TrackPoint.fromLocationWithStation(
                    location = list[i],
                    station = list[i + 1],
                )
            } else {
                index += 1
                TrackPoint.fromLocation(list[i])
            }
        }
    }

private data class TrackPoint(
    val time: String,
    val lat: String,
    val lng: String,
    val station: StationExtension? = null,
) {
    companion object {
        val LOCATION_REGEX = Regex("\\((?<lat>[0-9\\.]+),(?<lng>[0-9\\.]+)\\)")
        val STATION_REGEX = Regex("(?<name>.+)\\((?<code>[0-9]+)\\)")

        fun fromLocation(log: AppLog): TrackPoint {
            val m = LOCATION_REGEX.matchEntire(log.message)
                ?: throw RuntimeException("can not parse location log: $log")
            return TrackPoint(
                time = log.timestamp.format(TIME_PATTERN_ISO8601_EXTEND),
                lat = m.groups["lat"]!!.value,
                lng = m.groups["lng"]!!.value,
            )
        }

        fun fromLocationWithStation(
            location: AppLog,
            station: AppLog,
        ): TrackPoint {
            val m =
                STATION_REGEX.matchEntire(station.message)
                    ?: throw RuntimeException("can not parse station log: $station")
            val s =
                StationExtension(
                    name = m.groups["name"]!!.value,
                    code = m.groups["code"]!!.value,
                )
            return fromLocation(location).copy(station = s)
        }
    }
}

private data class StationExtension(val name: String, val code: String)
