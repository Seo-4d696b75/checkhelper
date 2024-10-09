package com.seo4d696b75.android.ekisagasu.ui.permission

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Parcelize
@TypeParceler<PermissionRationale, PermissionRationaleParceler>
data class PermissionRationaleArg(
    val value: PermissionRationale,
) : Parcelable

class PermissionRationaleParceler : Parceler<PermissionRationale> {
    override fun create(parcel: Parcel): PermissionRationale {
        val str = requireNotNull(parcel.readString())
        return Json.decodeFromString(str)
    }

    override fun PermissionRationale.write(parcel: Parcel, flags: Int) {
        val str = Json.encodeToString(this)
        parcel.writeString(str)
    }
}
