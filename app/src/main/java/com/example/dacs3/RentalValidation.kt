package com.example.dacs3

import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale

object RentalValidation {
    const val MAX_MONEY = 1_000_000_000_000L
    fun money(input: String): Long? {
        val value = input.trim().lowercase(Locale.ROOT)
            .replace(Regex("\\s*(vnđ|vnd|đ|₫)(/tháng)?\\s*$"), "").trim()
        if (!value.matches(Regex("[0-9]+|[0-9]{1,3}([., ][0-9]{3})+"))) return null
        return value.replace(Regex("[., ]"), "").toLongOrNull()?.takeIf { it in 0..MAX_MONEY }
    }
    fun total(vararg values: Long): Long? = runCatching {
        require(values.all { it in 0..MAX_MONEY })
        values.fold(0L, Math::addExact).also { require(it <= MAX_MONEY) }
    }.getOrNull()
    fun month(input: String): String? {
        val m = Regex("^(0?[1-9]|1[0-2])/(20[0-9]{2})$").matchEntire(input.trim()) ?: return null
        return "${m.groupValues[1].toInt().toString().padStart(2, '0')}/${m.groupValues[2]}"
    }
    fun coordinates(lat: Double?, lon: Double?) = lat != null && lon != null &&
        lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0
    fun id(vararg parts: String): String = MessageDigest.getInstance("SHA-256")
        .digest(parts.joinToString("").toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun requestId(propertyId: String,tenantUid: String) = "$propertyId~$tenantUid"
    fun billId(propertyId: String,tenantUid: String,month: String): String {
        val period=requireNotNull(this.month(month))
        return "$propertyId~$tenantUid~${period.substring(3)}-${period.substring(0,2)}"
    }
    fun conversation(propertyId: String, a: String, b: String): String {
        require(propertyId.isNotBlank() && a.isNotBlank() && b.isNotBlank() && a != b)
        return id(propertyId, *listOf(a, b).sorted().toTypedArray())
    }
    fun format(amount: Long) = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " đ"
}
