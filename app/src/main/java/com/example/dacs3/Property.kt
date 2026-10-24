package com.example.dacs3

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude

data class Property(
    var id: String = "", val hostId: String = "", val tenantUid: String = "",
    val title: String = "", val rental: String = "", val price: String = "",
    val location: String = "", val bedroom: Int = 0, val bathroom: Int = 0,
    val area: String = "", val imageUrl: String = "", val rating: Float = 0f,
    val status: String = "Trống", val latitude: Double? = null, val longitude: Double? = null,
    val monthlyPrice: Long = 0, val maxPeople: Int = 1
) : java.io.Serializable {
    @get:Exclude val address: String get() = location.ifBlank { rental }
    @get:Exclude val hasCoordinates: Boolean get() = RentalValidation.coordinates(latitude, longitude)
    @get:Exclude val rent: Long get() = monthlyPrice.takeIf { it > 0 } ?: RentalValidation.money(price) ?: 0L
    companion object {
        fun from(doc: DocumentSnapshot): Property = (doc.toObject(Property::class.java) ?: Property()).copy(
            id = doc.id, hostId = doc.getString("hostId").orEmpty().ifBlank { doc.getString("hostUid").orEmpty() }
        )
    }
}
