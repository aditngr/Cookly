package com.example.cookly

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.view.WindowManager
import android.view.View
import androidx.appcompat.app.AlertDialog

class ProfileActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var recipeAdapterUser: RecipeAdapterUser
    private var recipeList: MutableList<Recipe> = mutableListOf()

    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var editProfileButton: TextView
    private lateinit var logoutButton: TextView

    private lateinit var navHome: LinearLayout
    private lateinit var navAdd: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.orange)

        // Inisialisasi komponen UI
        profileImage = findViewById(R.id.profile_image)
        profileName = findViewById(R.id.profile_name)
        recyclerView = findViewById(R.id.recyclerView)
        editProfileButton = findViewById(R.id.edit_profile_button)
        logoutButton = findViewById(R.id.logout_button)

        // Inisialisasi Navbar
        navHome = findViewById(R.id.nav_home)
        navAdd = findViewById(R.id.nav_add)
        navProfile = findViewById(R.id.nav_profile)

        // Inisialisasi RecyclerView dengan listener edit
        recipeAdapterUser = RecipeAdapterUser(recipeList) { recipe ->
            val intent = Intent(this, EditActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.id)
            intent.putExtra("RECIPE_TITLE", recipe.title)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recipeAdapterUser

        // Ambil data pengguna dan resep dari Firestore
        fetchUserProfile()
        fetchRecipes()

        // Set click listener untuk tombol Edit Profile
        editProfileButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Maaf, fitur ini belum tersedia.")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        // Set click listener untuk tombol Keluar
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        // Set click listener untuk tombol Keluar
        logoutButton.setOnClickListener {
            // Membuat AlertDialog untuk konfirmasi keluar
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Apakah Anda yakin ingin keluar?")
                .setCancelable(false)
                .setPositiveButton("Ya") { dialog, id ->
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Tidak") { dialog, id ->
                    dialog.dismiss()
                }

            // Menampilkan dialog konfirmasi
            val alert = builder.create()
            alert.show()
        }

        // Logika navigasi navbar
        setupNavbar()
    }

    // Animasi untuk button
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

    // Fungsi setup untuk Navbar
    private fun setupNavbar() {
        val buttons = listOf(navHome to MainActivity::class.java, navAdd to AddRecipe::class.java)

        buttons.forEach { (button, targetActivity) ->
            button.setOnClickListener {
                animateButton(it)
                it.postDelayed({
                    val intent = Intent(this, targetActivity)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                }, 200)
            }
        }

        navProfile.setOnClickListener {
            animateButton(it)
        }
    }



    // Fungsi untuk mengambil data profil pengguna dari Firestore
    private fun fetchUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)
            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"

                    profileName.text = name

                    profileImage.setImageResource(R.drawable.default_profile)
                }
            }
        }
    }

    fun fetchRecipes() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("recipes")
                .whereEqualTo("uid", currentUser.uid)
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
                    recipeAdapterUser.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        } else {
            Log.e("ProfileActivity", "User not logged in")
        }
    }
}