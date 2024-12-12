package com.example.cookly

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import okhttp3.*

class EditActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var edtNama: EditText
    private lateinit var edtBahan: EditText
    private lateinit var edtLangkah: EditText
    private lateinit var btnUpdate: Button
    private lateinit var navHome: LinearLayout
    private lateinit var navAdd: LinearLayout
    private lateinit var navProfile: LinearLayout

    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    private var selectedImageUri: Uri? = null
    private var recipeId: String? = null
    private val PICK_IMAGE_REQUEST = 1
    private var isUpdated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_recipe)

        // Inisialisasi view
        imgPreview = findViewById(R.id.imgPreview)
        edtNama = findViewById(R.id.edtNama)
        edtBahan = findViewById(R.id.edtBahan)
        edtLangkah = findViewById(R.id.edtLangkah)
        btnUpdate = findViewById(R.id.btnUpdate)
        navHome = findViewById(R.id.nav_home)
        navAdd = findViewById(R.id.nav_add)
        navProfile = findViewById(R.id.nav_profile)

        // Ambil ID recipe dari intent
        recipeId = intent.getStringExtra("RECIPE_ID")

        // Jika ID tidak null, load data
        recipeId?.let {
            loadRecipeData(it)
        } ?: run {
            Toast.makeText(this, "ID resep tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        // Action untuk update data
        btnUpdate.setOnClickListener {
            recipeId?.let { id ->
                updateRecipeData(id)
            }
        }

        // Action untuk memilih gambar
        imgPreview.setOnClickListener {
            openGallery()
        }

        setupNavbarWithConfirmation()
    }

    private fun setupNavbarWithConfirmation() {
        navHome.setOnClickListener {
            showConfirmationDialog {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        navAdd.setOnClickListener {
            showConfirmationDialog {
                startActivity(Intent(this, AddRecipe::class.java))
                finish()
            }
        }
        navProfile.setOnClickListener {
            showConfirmationDialog {
                startActivity(Intent(this, ProfileActivity::class.java))
                finish()
            }
        }
    }

    private fun showConfirmationDialog(navigateAction: () -> Unit) {
        if (!isUpdated) {
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Batal update resep?")
                .setPositiveButton("Ya") { _, _ -> navigateAction() }
                .setNegativeButton("Tidak", null)
                .show()
        } else {
            navigateAction()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            imgPreview.setImageURI(selectedImageUri)
        }
    }

    private fun loadRecipeData(recipeId: String) {
        firestore.collection("recipes").document(recipeId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    edtNama.setText(document.getString("nama"))
                    edtBahan.setText(document.getString("bahan"))
                    edtLangkah.setText(document.getString("langkah"))
                    val imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Picasso.get().load(imageUrl).into(imgPreview)
                    }
                } else {
                    Toast.makeText(this, "Resep tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data resep", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecipeData(recipeId: String) {
        val nama = edtNama.text.toString().trim()
        val bahan = edtBahan.text.toString().trim()
        val langkah = edtLangkah.text.toString().trim()

        if (nama.isEmpty() || bahan.isEmpty() || langkah.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            uploadImageToServer(selectedImageUri!!) { imageUrl ->
                if (imageUrl != null) {
                    saveRecipeData(recipeId, nama, bahan, langkah, imageUrl)
                }
            }
        } else {
            saveRecipeData(recipeId, nama, bahan, langkah, null)
        }
    }

    private fun uploadImageToServer(uri: Uri, callback: (String?) -> Unit) {

    }

    private fun saveRecipeData(recipeId: String, nama: String, bahan: String, langkah: String, imageUrl: String?) {
        val recipeData = hashMapOf(
            "nama" to nama,
            "bahan" to bahan,
            "langkah" to langkah
        )
        if (imageUrl != null) {
            recipeData["imageUrl"] = imageUrl
        }

        firestore.collection("recipes").document(recipeId).update(recipeData as Map<String, Any>)
            .addOnSuccessListener {
                isUpdated = true
                Toast.makeText(this, "Resep berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui resep", Toast.LENGTH_SHORT).show()
            }
    }
}