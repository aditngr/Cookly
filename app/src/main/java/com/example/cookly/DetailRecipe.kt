package com.example.cookly

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import androidx.core.view.drawToBitmap
import android.view.View
import android.view.WindowManager

class DetailRecipe : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var viewNama: TextView
    private lateinit var viewAuthor: TextView
    private lateinit var viewBahan: TextView
    private lateinit var viewLangkah: TextView
    private lateinit var btnDownload: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_recipe)

        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.orange)

        // Inisialisasi View
        imgPreview = findViewById(R.id.imgPreview)
        viewNama = findViewById(R.id.viewNama)
        viewAuthor = findViewById(R.id.viewAuthor)
        viewBahan = findViewById(R.id.viewBahan)
        viewLangkah = findViewById(R.id.viewLangkah)
        btnDownload = findViewById(R.id.btnDownload)

        // Ambil data ID recipe dari Intent
        val recipeId = intent.getStringExtra("RECIPE_ID")

        // Load data dari Firestore
        if (recipeId != null) {
            loadRecipeData(recipeId)
        } else {
            Toast.makeText(this, "Data recipe tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        // Fitur download halaman
        btnDownload.setOnClickListener {
            savePageAsImage()
        }
    }

    //Load data resep
    private fun loadRecipeData(recipeId: String) {
        firestore.collection("recipes").document(recipeId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val nama = document.getString("nama")
                    val author = document.getString("author")
                    val bahan = document.getString("bahan")
                    val langkah = document.getString("langkah")
                    val imageUrl = document.getString("imageUrl")

                    viewNama.text = nama
                    viewAuthor.text = if (!author.isNullOrEmpty()) "by $author" else ""

                    viewBahan.text = bahan
                    viewLangkah.text = langkah

                    if (!imageUrl.isNullOrEmpty()) {
                        Picasso.get().load(imageUrl).into(imgPreview)
                    }
                } else {
                    Toast.makeText(this, "Recipe tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    //Fungsi simpan resep ke galeri
    private fun savePageAsImage() {
        btnDownload.visibility = View.GONE

        val rootView = window.decorView.rootView
        val bitmap = rootView.drawToBitmap()

        btnDownload.visibility = View.VISIBLE

        val contentResolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "recipe_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val outputStream = contentResolver.openOutputStream(it)

            outputStream?.let { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                Toast.makeText(this, "Resep berhasil diunduh!", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Gagal mengunduh resep", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
