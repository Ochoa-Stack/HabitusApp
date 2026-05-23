package com.ochoastack.habitus.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ochoastack.habitus.data.Habit
import com.ochoastack.habitus.databinding.ItemHabitBinding

class HabitAdapter(
    private val onMoreClick: (Habit) -> Unit,
) : ListAdapter<Habit, HabitAdapter.HabitVH>(DIFF_CALLBACK) {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Habit>() {
                override fun areItemsTheSame(
                    old: Habit,
                    new: Habit,
                ) = old.id == new.id

                override fun areContentsTheSame(
                    old: Habit,
                    new: Habit,
                ) = old == new
            }
    }

    inner class HabitVH(val binding: ItemHabitBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = HabitVH(ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        holder: HabitVH,
        position: Int,
    ) {
        val habit = getItem(position)
        with(holder.binding) {
            tvHabitName.text = habit.nombre
            tvFrequency.text = habit.frecuencia
            tvStreak.text = "🔗 ${habit.racha}"
            tvPercent.text = "✓ ${habit.porcentaje}%"
            // Mostramos el chip de categoría solo si tiene una asignada
            if (habit.categoriaNombre.isNotEmpty()) {
                tvCategoryChip.visibility = android.view.View.VISIBLE
                tvCategoryChip.text = habit.categoriaNombre
                // Aplicamos el color de la categoría al fondo del chip
                try {
                    val color = android.graphics.Color.parseColor(habit.categoriaColor)
                    tvCategoryChip.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(color)
                } catch (e: Exception) {
                    tvCategoryChip.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#C8614A"),
                        )
                }
            } else {
                tvCategoryChip.visibility = android.view.View.GONE
            }

            if (habit.estaCompletadoHoy) {
                ivHabitIcon.background =
                    root.context.getDrawable(
                        com.ochoastack.habitus.R.drawable.bg_circle_completed,
                    )
                ivHabitIcon.imageTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.WHITE,
                    )
            } else {
                ivHabitIcon.background =
                    root.context.getDrawable(
                        com.ochoastack.habitus.R.drawable.bg_circle_empty,
                    )
                ivHabitIcon.imageTintList =
                    android.content.res.ColorStateList.valueOf(
                        root.context.getColor(com.ochoastack.habitus.R.color.accent),
                    )
            }

            rvWeekDays.layoutManager =
                LinearLayoutManager(root.context, LinearLayoutManager.HORIZONTAL, false)
            rvWeekDays.adapter = DayAdapter(habit.weekDays)

            root.setOnClickListener { onMoreClick(habit) }
        }
    }
}
