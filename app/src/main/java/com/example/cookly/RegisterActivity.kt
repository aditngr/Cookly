package com.example.cookly

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cookly.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.WindowManager


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.orange)

        // Inisialisasi View Binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        firestore = FirebaseFirestore.getInstance()
        // Terapkan efek dim pada semua elemen awalnya
        applyDimEffect(binding.etName, true)
        applyDimEffect(binding.etEmail, true)
        applyDimEffect(binding.etPassword, true)
        applyDimEffect(binding.confirmPassword, true)
        applyDimEffect(binding.btnRegister, true)

        // Tambahkan TextWatcher untuk setiap form
        binding.etName.addTextChangedListener(inputWatcher)
        binding.etEmail.addTextChangedListener(inputWatcher)
        binding.etPassword.addTextChangedListener(inputWatcher)
        binding.confirmPassword.addTextChangedListener(inputWatcher)

        // Setup focus listener for each EditText
        setFocusListener(binding.etName)
        setFocusListener(binding.etEmail)
        setFocusListener(binding.etPassword)
        setFocusListener(binding.confirmPassword)

        // Set listener untuk tombol Register
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.confirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Kata sandi tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Buat pengguna baru dengan email dan kata sandi
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Dapatkan UID user yang baru dibuat
                        val userId = mAuth.currentUser?.uid

                        if (userId != null) {
                            // Data yang akan disimpan di Firestore
                            val userMap = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "uid" to userId
                            )

                            // Simpan data ke koleksi "users" di Firestore
                            firestore.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                                    openLoginForm() // Pindah ke form login
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        val errorMessage = task.exception?.message ?: "Registrasi gagal"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Set listener untuk tombol Masuk
        binding.btnLogin.setOnClickListener {
            openLoginForm()
        }
    }

    // Fungsi untuk pindah ke aktivitas Login
    private fun openLoginForm() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private val inputWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val isNameFilled = binding.etName.text.toString().trim().isNotEmpty()
            val isEmailFilled = binding.etEmail.text.toString().trim().isNotEmpty()
            val isPasswordFilled = binding.etPassword.text.toString().trim().isNotEmpty()
            val isConfirmPasswordFilled = binding.confirmPassword.text.toString().trim().isNotEmpty()

            // Apply dim effect based on input status
            applyDimEffect(binding.etName, !isNameFilled)
            applyDimEffect(binding.etEmail, !isEmailFilled)
            applyDimEffect(binding.etPassword, !isPasswordFilled)
            applyDimEffect(binding.confirmPassword, !isConfirmPasswordFilled)

            // Enable and brighten the register button if all fields are filled
            val allFieldsFilled = isNameFilled && isEmailFilled && isPasswordFilled && isConfirmPasswordFilled
            binding.btnRegister.isEnabled = allFieldsFilled
            applyDimEffect(binding.btnRegister, !allFieldsFilled)
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private fun setFocusListener(view: View) {
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                applyDimEffect(view, false)
            }
        }
    }

    private fun applyDimEffect(view: View, dim: Boolean) {
        view.alpha = if (dim) 0.5f else 1.0f
    }
}
