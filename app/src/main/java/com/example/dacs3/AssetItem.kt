package com.example.dacs3

data class AssetItem(
    var id: String = "",
    val assetName: String = "",
    val roomName: String = "",

    // ĐỔI TỪ val SANG var TẠI ĐÂY:
    var quantity: Int = 1,

    val condition: String = "Tốt"
)