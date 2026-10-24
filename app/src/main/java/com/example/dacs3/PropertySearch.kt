package com.example.dacs3

import java.text.Normalizer
import java.util.Locale

data class RoomFilters(val query: String="",val minPrice: Long=0,val maxPrice: Long=RentalValidation.MAX_MONEY,
    val minArea: Double=0.0,val vacantOnly: Boolean=false,val sort: Int=0,val ownedOnly: Boolean=false) : java.io.Serializable {
    fun apply(rooms: List<Property>, uid: String?=null): List<Property> {
        val words=normalize(query).split(Regex("\\s+")).filter { it.isNotBlank() }
        val result=rooms.filter { p ->
            val text=normalize("${p.title} ${p.address}")
            words.all { text.contains(it) } && p.rent in minPrice..maxPrice && (p.rent>0 || minPrice==0L && maxPrice==RentalValidation.MAX_MONEY) &&
                (p.area.replace(",",".").substringBefore("m").trim().toDoubleOrNull() ?: 0.0)>=minArea &&
                (!vacantOnly || p.status=="Trống" && p.tenantUid.isBlank()) && (!ownedOnly || uid!=null && p.hostId==uid)
        }
        return when(sort) { 1 -> result.sortedBy { it.rent }; 2 -> result.sortedByDescending { it.rent }; else -> result.sortedBy { normalize(it.title) } }
    }
    companion object {
        fun normalize(s: String)=Normalizer.normalize(s,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace('đ','d')
    }
}
