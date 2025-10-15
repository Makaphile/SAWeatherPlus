package com.example.saweatherplus

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.saweatherplus.databinding.ActivitySignupBinding
import com.example.saweatherplus.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignup: Button
    private lateinit var tvLogin: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()
        setupFirebase()
        setupClickListeners()
        setupLoginLink()
    }

    private fun initializeViews() {
        etUsername = binding.etUsername
        etEmail = binding.etEmail
        etPassword = binding.etPassword
        btnSignup = binding.btnSignup
        tvLogin = binding.tvLogin
    }

    private fun setupFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun setupClickListeners() {
        btnSignup.setOnClickListener {
            handleSignup()
        }
    }

    private fun setupLoginLink() {
        val spannableString = SpannableString("Already have an account? Login")
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigateToLogin()
            }
        }

        spannableString.setSpan(
            clickableSpan,
            spannableString.length - 5,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvLogin.text = spannableString
        tvLogin.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun handleSignup() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (validateInputs(username, email, password)) {
            btnSignup.isEnabled = false
            btnSignup.text = "Creating Account..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign up success
                        val user = auth.currentUser

                        // Update user profile with username
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()

                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    // Save user to Realtime Database
                                    saveUserToDatabase(user.uid, username, email)

                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    navigateToMain()
                                } else {
                                    // Profile update failed but user was created
                                    saveUserToDatabase(user.uid, username, email)
                                    Toast.makeText(this, "Account created! Profile update failed.", Toast.LENGTH_SHORT).show()
                                    navigateToMain()
                                }
                            }
                    } else {
                        // Sign up failed
                        Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        btnSignup.isEnabled = true
                        btnSignup.text = "Sign Up"
                    }
                }
        }
    }

    private fun validateInputs(username: String, email: String, password: String): Boolean {
        return when {
            username.isEmpty() -> {
                showToast("Please enter username")
                etUsername.requestFocus()
                false
            }
            username.length < 3 -> {
                showToast("Username must be at least 3 characters")
                etUsername.requestFocus()
                false
            }
            email.isEmpty() -> {
                showToast("Please enter email")
                etEmail.requestFocus()
                false
            }
            !isValidEmail(email) -> {
                showToast("Please enter a valid email address")
                etEmail.requestFocus()
                false
            }
            password.isEmpty() -> {
                showToast("Please enter password")
                etPassword.requestFocus()
                false
            }
            password.length < 6 -> {
                showToast("Password must be at least 6 characters")
                etPassword.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(emailRegex.toRegex())
    }

    private fun saveUserToDatabase(userId: String, username: String, email: String) {
        val user = User(
            id = userId,
            name = username,
            email = email
        )

        val usersRef = database.getReference("users")
        usersRef.child(userId).setValue(user)
            .addOnSuccessListener {
                // User saved successfully
                Toast.makeText(this, "User profile saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Failed to save user
                Toast.makeText(this, "Failed to save user profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}