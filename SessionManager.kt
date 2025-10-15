package com.example.saweatherplus.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.saweatherplus.model.User

class SessionManager(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUser(user: User) {
        with(sharedPref.edit()) {
            putString("user_id", user.id)
            putString("username", user.name)
            putString("email", user.email)
            putInt("age", user.age)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    fun getCurrentUser(): User? {
        return if (isLoggedIn()) {
            User(
                id = sharedPref.getString("user_id", "") ?: "",
                name = sharedPref.getString("username", "") ?: "",
                email = sharedPref.getString("email", "") ?: "",
                age = sharedPref.getInt("age", 0)
            )
        } else {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPref.getBoolean("is_logged_in", false)
    }

    fun logout() {
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }
}