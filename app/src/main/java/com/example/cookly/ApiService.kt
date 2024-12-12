package com.example.cookly

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    // Mengunggah gambar (POST)
    @Multipart
    @POST("upload.php")
    fun uploadImage(
        @Part image: MultipartBody.Part
    ): Call<ResponseBody>

    // Mendapatkan daftar gambar (GET)
    @GET("upload.php")
    fun getImages(): Call<ApiResponse>

    // Menghapus gambar (DELETE)
    @DELETE("upload.php")
    fun deleteImage(
        @Query("file_name") fileName: String
    ): Call<ResponseBody>

    // Memperbarui image
    @Multipart
    @POST("upload.php")
    fun updateImage(
        @Part("method") method: RequestBody,
        @Part("old_file_name") oldFileName: RequestBody,
        @Part newImage: MultipartBody.Part
    ): Call<ResponseBody>
}
