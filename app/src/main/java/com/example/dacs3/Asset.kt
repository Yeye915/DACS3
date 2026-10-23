package com.example.dacs3

data class Asset(
    val name: String,
    var quantity: Int,
    var status: String,
    val buyDate: String,
    val value: String,
    val note: String,
    val quality: String
)