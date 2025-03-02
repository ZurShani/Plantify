package com.example.plantify

data class Plant(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    var price: String = "",
    val imagePath: String = "",
    val userId: String = "",
    val type: String = "",
    var createdBy: String =""
)
