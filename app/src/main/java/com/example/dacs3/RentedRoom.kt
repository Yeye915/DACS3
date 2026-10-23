package com.example.dacs3



data class RentedRoom(
    var id: String = "",
    val roomName: String = "",    // Tên phòng (vđ: Phòng 101)
    val address: String = "",     // Địa chỉ nhà trọ
    val price: Long = 0,          // Giá thuê
    val hostName: String = "",    // Tên chủ nhà
    val tenantUid: String = ""    // UID của người thuê để lọc đúng phòng của họ
)