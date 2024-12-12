package com.example.cookly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.content.Intent

// Adapter untuk menampilkan daftar resep dalam RecyclerView
class RecipeAdapter(private val recipeList: List<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>(), Filterable {

    private var filteredRecipeList: List<Recipe> = recipeList

    // ViewHolder untuk memetakan elemen di layout item_recipe
    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgRecipe: ImageView = view.findViewById(R.id.imgRecipe)
        val title: TextView = view.findViewById(R.id.title)
        val txtRecipeAuthor: TextView = view.findViewById(R.id.txtRecipeAuthor)
    }

    // Membuat tampilan untuk setiap item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    // Menghubungkan data resep ke tampilan
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = filteredRecipeList[position]

        holder.title.text = recipe.title
        holder.txtRecipeAuthor.text = "by ${recipe.author}"

        val imageUrl = recipe.imageUrl
        if (imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(imageUrl).into(holder.imgRecipe)
        } else {
            holder.imgRecipe.setImageResource(R.drawable.dummy)
        }

        // Navigasi ke detail resep saat item diklik
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val recipeId = recipe.id
            val intent = Intent(context, DetailRecipe::class.java)
            intent.putExtra("RECIPE_ID", recipeId)
            context.startActivity(intent)
        }
    }

    // Mendapatkan jumlah item dalam daftar resep
    override fun getItemCount(): Int = filteredRecipeList.size

    // Implementasi fitur pencarian atau penyaringan daftar resep
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val filteredList: MutableList<Recipe> = mutableListOf()

                if (constraint.isNullOrEmpty()) {
                    filteredList.addAll(recipeList)
                } else {
                    val filterPattern = constraint.toString().lowercase().trim()
                    for (recipe in recipeList) {
                        if (recipe.title.lowercase().contains(filterPattern)) {
                            filteredList.add(recipe)
                        }
                    }
                }

                filterResults.values = filteredList
                filterResults.count = filteredList.size
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                // Memperbarui daftar resep yang difilter
                filteredRecipeList = results?.values as List<Recipe>
                notifyDataSetChanged()
            }
        }
    }
}
