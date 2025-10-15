package com.example.saweatherplus.model

data class WeatherData(
    val cityName: String = "",
    val temperature: Double = 0.0,
    val condition: String = "",
    val description: String = "",
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val pressure: Double = 0.0,
    val icon: String = ""
)