package com.example.cookly

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cookly.databinding.ActivityAddRecipeBinding
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import android.view.WindowManager
import android.os.Handler

class AddRecipe : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecipeBinding
    private var selectedImageUri: Uri? = null
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.orange)

        // Inisialisasi View Binding
        binding = ActivityAddRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menetapkan efek dim pada semua form saat aplikasi dimulai
        setInitialBackground()

        // Menambahkan listener untuk setiap form
        addFocusListeners()

        // Klik pada ImageView untuk memilih gambar
        binding.imgPreview.setOnClickListener {
            chooseImage()
        }

        // Klik pada tombol upload untuk mengunggah gambar dan teks
        binding.btnUpload.setOnClickListener {
            animateButton(binding.btnUpload)
            handleUpload()
        }

        // Menambahkan delay 200ms pada perpindahan halaman
        binding.navHome.setOnClickListener {
            animateButton(binding.navHome)
            Handler().postDelayed({
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }, 200)
        }

        binding.navProfile.setOnClickListener {
            animateButton(binding.navProfile) // Menambahkan animasi pada tombol Profile
            Handler().postDelayed({
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }, 200)
        }
    }

    // Fungsi untuk menambahkan animasi pada tombol
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

    private fun setInitialBackground() {
        // Menetapkan efek dim (samar) untuk semua form saat pertama kali dimuat
        applyDimEffect(binding.edtNama, true)
        applyDimEffect(binding.edtBahan, true)
        applyDimEffect(binding.edtLangkah, true)
        applyDimEffect(binding.btnUpload, true)
        binding.btnUpload.isEnabled = false
    }

    private fun addFocusListeners() {
        // Menambahkan listener untuk EditText
        binding.edtNama.setOnFocusChangeListener { _, hasFocus ->
            applyDimEffect(binding.edtNama, !hasFocus && binding.edtNama.text.isEmpty())
            checkFormAndImageValidity()
        }

        binding.edtBahan.setOnFocusChangeListener { _, hasFocus ->
            applyDimEffect(binding.edtBahan, !hasFocus && binding.edtBahan.text.isEmpty())
            checkFormAndImageValidity()
        }

        binding.edtLangkah.setOnFocusChangeListener { _, hasFocus ->
            applyDimEffect(binding.edtLangkah, !hasFocus && binding.edtLangkah.text.isEmpty())
            checkFormAndImageValidity()
        }
    }

    // Fungsi untuk menerapkan efek dim pada view
    private fun applyDimEffect(view: View, dim: Boolean) {
        view.alpha = if (dim) 0.3f else 1.0f
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.imgPreview.setImageURI(selectedImageUri)
            checkFormAndImageValidity()
        }
    }

    private fun handleUpload() {
        val isFormComplete = binding.edtNama.text.toString().trim().isNotEmpty() &&
                binding.edtBahan.text.toString().trim().isNotEmpty() &&
                binding.edtLangkah.text.toString().trim().isNotEmpty()
        val isImageSelected = selectedImageUri != null

        if (isFormComplete && isImageSelected) {
            selectedImageUri?.let { uri ->
                uploadImageToServer(uri)
            }
        } else {
            Toast.makeText(this, "Lengkapi semua input dan pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getFileFromUri(uri: Uri): File {
        val filePath = applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
            val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        }
        return filePath ?: throw IllegalArgumentException("Unable to create file")
    }

    private fun uploadImageToServer(uri: Uri) {
        val file = getFileFromUri(uri)
        val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        val apiService = ApiClient.retrofit.create(ApiService::class.java)
        apiService.uploadImage(body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val imageUrl = response.body()?.string() ?: ""
                    saveDataToFirestore(imageUrl)
                } else {
                    Toast.makeText(this@AddRecipe, "Gagal mengunggah gambar.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@AddRecipe, "Kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveDataToFirestore(apiResponse: String) {
        // Mengambil ID pengguna yang sedang login dari Firebase Authentication
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId != null) {
            // Ambil nama pengguna dari koleksi 'users' berdasarkan userId
            val usersRef = FirebaseFirestore.getInstance().collection("users")
            usersRef.document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("name") ?: "Anonim"
                        saveRecipeData(userName, apiResponse)
                    } else {
                        Toast.makeText(this, "Pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal mengambil data pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveRecipeData(userName: String, apiResponse: String) {
        // Parsing JSON dari respons API
        val imageUrl = try {
            val json = JSONObject(apiResponse)
            json.getString("file_url")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        val validImageUrl = if (imageUrl.isNotEmpty()) {
            imageUrl.replace("http://localhost", "http://192.168.1.2")
        } else {
            ""
        }

        if (validImageUrl.isEmpty()) {
            Toast.makeText(this, "URL gambar tidak valid.", Toast.LENGTH_SHORT).show()
            return
        }

        // Mendapatkan data dari form
        val nama = binding.edtNama.text.toString().trim()
        val bahan = binding.edtBahan.text.toString().trim()
        val langkah = binding.edtLangkah.text.toString().trim()

        // Mendapatkan ID pengguna yang sedang login
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "ID pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        // Membuat data untuk disimpan di Firestore
        val data = hashMapOf(
            "nama" to nama,
            "bahan" to bahan,
            "langkah" to langkah,
            "imageUrl" to validImageUrl,
            "author" to userName,
            "uid" to userId
        )

        // Menyimpan data ke Firestore
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("recipes")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkFormAndImageValidity() {
        val isFormComplete = binding.edtNama.text.toString().trim().isNotEmpty() &&
                binding.edtBahan.text.toString().trim().isNotEmpty() &&
                binding.edtLangkah.text.toString().trim().isNotEmpty()
        val isImageSelected = selectedImageUri != null

        applyDimEffect(binding.btnUpload, !(isFormComplete && isImageSelected))
        binding.btnUpload.isEnabled = isFormComplete && isImageSelected
    }
}