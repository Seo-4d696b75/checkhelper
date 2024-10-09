package com.seo4d696b75.android.ekisagasu.ui.update

import android.os.Parcel
import android.os.Parcelable
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Parcelize
@TypeParceler<LatestDataVersion, LatestDataVersionParceler>
data class LatestDataVersionArg(
    val value: LatestDataVersion,
) : Parcelable

class LatestDataVersionParceler : Parceler<LatestDataVersion> {
    override fun create(parcel: Parcel): LatestDataVersion {
        val str = requireNotNull(parcel.readString())
        return Json.decodeFromString(str)
    }

    override fun LatestDataVersion.write(parcel: Parcel, flags: Int) {
        val str = Json.encodeToString(this)
        parcel.writeString(str)
    }
}
