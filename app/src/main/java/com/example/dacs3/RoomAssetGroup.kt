package com.example.dacs3

data class RoomAssetGroup(
    val roomCode: String,
    val status: String,
    val assets: List<Asset>
)