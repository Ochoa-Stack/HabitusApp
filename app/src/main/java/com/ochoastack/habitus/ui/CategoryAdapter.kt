package com.ochoastack.habitus.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ochoastack.habitus.data.HabitCategory
import com.ochoastack.habitus.databinding.ItemCategoryBinding

// Creamos el adaptador para las categorías
class CategoryAdapter(
    private val categorias: List<HabitCategory>,
    private val onClick: (HabitCategory) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.CategoryVH>() {
    inner class CategoryVH(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = CategoryVH(
        ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        ),
    )

    override fun getItemCount() = categorias.size

    override fun onBindViewHolder(
        holder: CategoryVH,
        position: Int,
    ) {
        val categoria = categorias[position]
        with(holder.binding) {
            tvCategoryName.text = categoria.nombre
            tvCategoryCount.text = "${categoria.totalHabitos} hábitos"
            // Aplicamos el color real de la categoría
            val colorInt =
                try {
                    Color.parseColor(categoria.color)
                } catch (e: Exception) {
                    Color.parseColor("#C8614A")
                }
            ivCategoryIcon.backgroundTintList =
                android.content.res.ColorStateList.valueOf(colorInt)
            ivCategoryIcon.backgroundTintList =
                android.content.res.ColorStateList.valueOf(colorInt)
            ivCategoryIcon.setImageResource(
                com.ochoastack.habitus.R.drawable.ic_leaf,
            )

            root.setOnClickListener { onClick(categoria) }
        }
    }
}
