package com.example.saweatherplus.network

import com.example.saweatherplus.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String = "1c3b809a39f5e539cf830512e2683a"  // Your API key
    ): WeatherResponse

    @GET("weather")
    suspend fun getWeatherByLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String = "1c3b809a39f5e539cf830512e2683a"  // Your API key
    ): WeatherResponse
}