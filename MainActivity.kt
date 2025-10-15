package com.example.saweatherplus

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.saweatherplus.databinding.ActivityMainBinding
import com.example.saweatherplus.model.WeatherData
import com.example.saweatherplus.model.WeatherResponse
import com.example.saweatherplus.network.RetrofitClient
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tvCityName: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeatherCondition: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvDate: TextView
    private lateinit var btnRefreshWeather: Button
    private lateinit var btnLogout: Button
    private lateinit var tvWelcomeUser: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearchPlace: EditText
    private lateinit var btnSearchPlace: Button

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val locationPermissionCode = 1001
    private val OPENWEATHER_API_KEY = "1c3b8b809a39f5e539cf830512e2683a"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        initializeViews()
        setupFirebase()
        setupLocationServices()
        setupClickListeners()
        displayUserInfo()
        updateDateTime()

        // Load weather data based on current location
        loadWeatherData()
    }

    private fun isUserLoggedIn(): Boolean {
        auth = FirebaseAuth.getInstance()
        return auth.currentUser != null
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun initializeViews() {
        tvCityName = binding.tvCityName
        tvTemperature = binding.tvTemperature
        tvWeatherCondition = binding.tvWeatherCondition
        tvHumidity = binding.tvHumidity
        tvWind = binding.tvWind
        tvDate = binding.tvDate
        btnRefreshWeather = binding.btnRefreshWeather
        btnLogout = binding.btnLogout
        tvWelcomeUser = binding.tvWelcomeUser
        progressBar = binding.progressBar
        etSearchPlace = binding.etSearchPlace
        btnSearchPlace = binding.btnSearchPlace

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupFirebase() {
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun setupLocationServices() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // We got the location, now fetch weather
                    fetchWeatherByLocation(location.latitude, location.longitude)

                    // Stop location updates after getting one location
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnRefreshWeather.setOnClickListener {
            loadWeatherData()
        }

        btnLogout.setOnClickListener {
            logoutUser()
        }

        btnSearchPlace.setOnClickListener {
            searchWeatherByPlace()
        }
    }

    private fun searchWeatherByPlace() {
        val placeName = etSearchPlace.text.toString().trim()

        if (placeName.isEmpty()) {
            showToast("Please enter a place name")
            return
        }

        progressBar.visibility = ProgressBar.VISIBLE
        fetchWeatherForCity(placeName)
    }

    private fun displayUserInfo() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val welcomeText = "Welcome, ${user.email?.split("@")?.first() ?: "User"}!"
            tvWelcomeUser.text = welcomeText
        }
    }

    private fun updateDateTime() {
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val formattedDateTime = dateFormat.format(Date(currentTime))
        tvDate.text = formattedDateTime
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun loadWeatherData() {
        progressBar.visibility = ProgressBar.VISIBLE
        if (checkLocationPermissions()) {
            getCurrentLocation()
        } else {
            requestLocationPermissions()
        }
    }

    private fun checkLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }

    private fun getCurrentLocation() {
        if (checkLocationPermissions()) {
            try {
                // First try to get last known location (faster)
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            // Use last known location
                            fetchWeatherByLocation(location.latitude, location.longitude)
                        } else {
                            // If no last known location, request new location
                            requestNewLocation()
                        }
                    }
                    .addOnFailureListener { e ->
                        // If failed, request new location
                        requestNewLocation()
                    }
            } catch (e: SecurityException) {
                showToast("Location permission denied")
                fetchWeatherForCity("Johannesburg")
            }
        }
    }

    private fun requestNewLocation() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )

            // Set a timeout in case location doesn't come
            Handler(Looper.getMainLooper()).postDelayed({
                fusedLocationClient.removeLocationUpdates(locationCallback)
                showToast("Location timeout, using default city")
                fetchWeatherForCity("Johannesburg")
            }, 15000) // 15 second timeout

        } catch (e: SecurityException) {
            showToast("Location permission denied")
            fetchWeatherForCity("Johannesburg")
        }
    }

    private fun fetchWeatherByLocation(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getWeatherByLocation(
                    lat, lon, OPENWEATHER_API_KEY, "metric"
                )
                withContext(Dispatchers.Main) {
                    updateWeatherUI(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Unable to get weather for your location")
                    fetchWeatherForCity("Johannesburg") // Fallback to default city
                }
            }
        }
    }

    private fun fetchWeatherForCity(city: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getCurrentWeather(
                    city, OPENWEATHER_API_KEY, "metric"
                )
                withContext(Dispatchers.Main) {
                    updateWeatherUI(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Failed to fetch weather for $city")
                    setDefaultWeatherData()
                }
            }
        }
    }

    private fun updateWeatherUI(response: WeatherResponse) {
        progressBar.visibility = ProgressBar.GONE

        tvCityName.text = response.name
        tvTemperature.text = "${response.main.temp.toInt()}°C"
        tvWeatherCondition.text = response.weather.firstOrNull()?.description ?: "Unknown"
        tvHumidity.text = "${response.main.humidity}%"
        tvWind.text = "${response.wind.speed.toInt()} km/h"

        // Update background based on weather condition
        updateBackground(response.weather.firstOrNull()?.main ?: "Clear")

        // Save weather data to Firebase
        saveWeatherToFirebase(response)
    }

    private fun setDefaultWeatherData() {
        progressBar.visibility = ProgressBar.GONE
        tvCityName.text = "Johannesburg"
        tvTemperature.text = "22°C"
        tvWeatherCondition.text = "Sunny"
        tvHumidity.text = "45%"
        tvWind.text = "15 km/h"
        updateBackground("Clear")
    }

    private fun updateBackground(weatherCondition: String) {
        val backgroundRes = when (weatherCondition.toLowerCase(Locale.ROOT)) {
            "clear" -> R.drawable.bg_sunny
            "clouds" -> R.drawable.bg_cloudy
            "rain", "drizzle" -> R.drawable.bg_rainy
            "thunderstorm" -> R.drawable.bg_storm
            else -> R.drawable.bg_default
        }
        binding.root.setBackgroundResource(backgroundRes)
    }

    private fun saveWeatherToFirebase(response: WeatherResponse) {
        val currentUser = auth.currentUser
        val weatherData = WeatherData(
            cityName = response.name,
            temperature = response.main.temp,
            condition = response.weather.firstOrNull()?.main ?: "",
            description = response.weather.firstOrNull()?.description ?: "",
            humidity = response.main.humidity,
            windSpeed = response.wind.speed,
            pressure = response.main.pressure,
            icon = response.weather.firstOrNull()?.icon ?: ""
        )

        val weatherRef = database.getReference("weather_data")
        val weatherId = weatherRef.push().key!!

        weatherRef.child(weatherId).setValue(weatherData)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == locationPermissionCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            showToast("Location permission denied. Using default city.")
            fetchWeatherForCity("Johannesburg")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up location updates
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore if no updates were requested
        }
    }
}