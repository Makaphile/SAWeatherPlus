package com.example.saweatherplus.model

data class User(
    var id: String = "",
    val name: String = "",
    val age: Int = 0,
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)