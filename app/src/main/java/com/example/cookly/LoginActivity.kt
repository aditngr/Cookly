package com.example.cookly

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.cookly.databinding.ActivityLoginBinding
import android.view.WindowManager

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.orange)

        // Inisialisasi View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        // Set initial dim effect to all elements
        applyDimEffect(binding.btnLogin, true)
        applyDimEffect(binding.etEmail, true)
        applyDimEffect(binding.etPassword, true)

        // Add focus listeners for email and password EditTexts
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) applyDimEffect(binding.etEmail, false)
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) applyDimEffect(binding.etPassword, false)
        }

        // Add TextWatcher for both email and password EditTexts
        binding.etEmail.addTextChangedListener(inputWatcher)
        binding.etPassword.addTextChangedListener(inputWatcher)

        // Setup login button click listener
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        // Setup forgot password click listener
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Setup register click listener
        binding.tvDaftar.setOnClickListener {
            openRegisterForm()
        }
    }

    private fun openRegisterForm() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun loginUser(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // TextWatcher to monitor changes in etEmail and etPassword
    private val inputWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val isEmailFilled = binding.etEmail.text.toString().trim().isNotEmpty()
            val isPasswordFilled = binding.etPassword.text.toString().trim().isNotEmpty()

            // Apply dim effect based on input status
            applyDimEffect(binding.etEmail, !isEmailFilled)
            applyDimEffect(binding.etPassword, !isPasswordFilled)

            // Enable and brighten the login button if both fields are filled
            binding.btnLogin.isEnabled = isEmailFilled && isPasswordFilled
            applyDimEffect(binding.btnLogin, !(isEmailFilled && isPasswordFilled))
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    // Function to apply dim effect on a view with transparency
    private fun applyDimEffect(view: View, dim: Boolean) {
        view.alpha = if (dim) 0.3f else 1.0f
    }
}
