package com.example.cookly

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecipeAdapterUser(
    private val recipeList: MutableList<Recipe>,
    private val onEditClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapterUser.RecipeViewHolder>() {

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgRecipe: ImageView = view.findViewById(R.id.imgRecipe)
        val title: TextView = view.findViewById(R.id.title)
        val txtRecipeAuthor: TextView = view.findViewById(R.id.txtRecipeAuthor)
        val editRecipe: ImageView = view.findViewById(R.id.edit_recipe)
        val deleteRecipe: ImageView = view.findViewById(R.id.delete_recipe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe_user, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.title.text = recipe.title
        holder.txtRecipeAuthor.text = "by ${recipe.author}"

        val imageUrl = recipe.imageUrl
        if (imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.imgRecipe)
        } else {
            holder.imgRecipe.setImageResource(R.drawable.dummy)
        }

        holder.editRecipe.setOnClickListener {
            onEditClick(recipe)
        }

        holder.deleteRecipe.setOnClickListener {
            val context = holder.itemView.context
            confirmAndDeleteRecipe(context, recipe, position)
        }
    }

    override fun getItemCount(): Int = recipeList.size

    private fun confirmAndDeleteRecipe(context: Context, recipe: Recipe, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Hapus Resep")
            .setMessage("Apakah Anda yakin ingin menghapus resep ini?")
            .setPositiveButton("Ya") { _, _ ->
                deleteImageFromServer(context, recipe, position)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun deleteImageFromServer(context: Context, recipe: Recipe, position: Int) {
        val apiService = ApiClient.apiService
        val fileName = recipe.imageUrl.substringAfterLast("/")

        apiService.deleteImage(fileName).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    deleteRecipeFromFirestore(context, recipe, position)
                } else {
                    Toast.makeText(context, "Gagal menghapus gambar di server.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(context, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteRecipeFromFirestore(context: Context, recipe: Recipe, position: Int) {
        val db = FirebaseFirestore.getInstance()
        db.collection("recipes").document(recipe.id)
            .delete()
            .addOnSuccessListener {
                recipeList.removeAt(position)
                notifyItemRemoved(position)
                Toast.makeText(context, "Resep berhasil dihapus.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal menghapus resep", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun updateRecipeData(
            recipeId: String,
            newNama: String,
            newBahan: String,
            newLangkah: String,
            imageUri: Uri?,
            context: Context,
            recipeList: MutableList<Recipe>,
            adapter: RecipeAdapterUser
        ) {
            val firestore = FirebaseFirestore.getInstance()
            val recipeRef = firestore.collection("recipes").document(recipeId)

            val updatedData: MutableMap<String, Any> = hashMapOf(
                "nama" to newNama,
                "bahan" to newBahan,
                "langkah" to newLangkah
            )

            imageUri?.let { uri -> updatedData["imageUrl"] = uri.toString() }

            // Perbarui data resep di Firestore
            recipeRef.update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Resep berhasil diperbarui.", Toast.LENGTH_SHORT).show()

                    // Update recipeList di adapter
                    val updatedRecipe = Recipe(recipeId, newNama, "by Author", imageUri?.toString() ?: "")
                    val index = recipeList.indexOfFirst { it.id == recipeId }
                    if (index != -1) {
                        recipeList[index] = updatedRecipe
                        adapter.notifyItemChanged(index)
                    }

                    if (context is EditActivity) {
                        context.finish()
                    }

                    if (context is ProfileActivity) {
                        context.fetchRecipes()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal memperbarui resep: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
