package com.example.cookly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import com.example.cookly.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // Inisialisasi binding, Firestore, FirebaseAuth, dan daftar resep
    private lateinit var binding: ActivityMainBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val recipeList = mutableListOf<Recipe>()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur status bar transparan dan mengganti warnanya
        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.orange)

        // Inisialisasi View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Konfigurasi RecyclerView untuk menampilkan daftar resep
        setupRecyclerView()

        // Menambahkan animasi pada tombol navigasi
        setupButtonAnimations()

        // Mengambil nama pengguna yang sedang login dari Firestore
        fetchUserName()

        // Mengambil data resep dari Firestore
        fetchRecipes()

        // Menambahkan fitur pencarian menggunakan SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Memfilter daftar resep berdasarkan input pencarian
                recipeAdapter.filter.filter(newText)
                return false
            }
        })

        binding.searchView.setOnClickListener {
            if (binding.searchView.isIconified) {
                binding.searchView.setIconified(false)
            }
        }
    }

    // Menambahkan animasi pada tombol navigasi
    private fun setupButtonAnimations() {
        val buttons = listOf(binding.navProfile, binding.navHome, binding.navAdd)

        buttons.forEach { button ->
            button.setOnClickListener {
                animateButton(it)
                it.postDelayed({
                    // Berpindah ke activity yang sesuai setelah animasi selesai
                    when (button) {
                        binding.navProfile -> {
                            val intent = Intent(this, ProfileActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(0, 0)
                        }
                        binding.navAdd -> {
                            val intent = Intent(this, AddRecipe::class.java)
                            startActivity(intent)
                            overridePendingTransition(0, 0)
                        }
                    }
                }, 200)
            }
        }
    }

    // Implementasi animasi skala untuk tombol
    private fun animateButton(button: View) {
        button.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    // Konfigurasi RecyclerView dengan adapter dan layout manager
    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(recipeList)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recipeAdapter
        }
    }

    // Mengambil data resep dari Firestore
    private fun fetchRecipes() {
        firestore.collection("recipes")
            .get()
            .addOnSuccessListener { result ->
                recipeList.clear()
                for (document in result) {
                    val recipe = Recipe(
                        id = document.id,
                        title = document.getString("nama") ?: "Unknown",
                        author = document.getString("author") ?: "Anonymous",
                        imageUrl = document.getString("imageUrl") ?: ""
                    )
                    recipeList.add(recipe)
                    Log.d("Firestore", "Fetched imageUrl: ${recipe.imageUrl}")
                }
                recipeAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    // Mengambil nama pengguna yang sedang login dari Firestore
    private fun fetchUserName() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userName = document.getString("name") ?: "User"
                        binding.tvWelcome.text = "Selamat Datang"
                        binding.tvUserName.text = userName
                        Log.d("Firestore", "User name fetched: $userName")
                    } else {
                        binding.tvWelcome.text = "Selamat Datang"
                        binding.tvUserName.text = "User"
                        Log.d("Firestore", "No user document found for UID: $userId")
                    }
                }
                .addOnFailureListener { e ->
                    // Menampilkan pesan default jika ada error
                    binding.tvWelcome.text = "Selamat Datang"
                    binding.tvUserName.text = "User"
                    Log.e("Firestore", "Error fetching user name", e)
                }
        } else {
            binding.tvWelcome.text = "Selamat Datang"
            binding.tvUserName.text = "User"
            Log.d("Auth", "No user logged in")
        }
    }

    // Memperbarui data saat activity kembali ke tampilan aktif
    override fun onResume() {
        super.onResume()
        fetchRecipes()
        fetchUserName()
    }
}
