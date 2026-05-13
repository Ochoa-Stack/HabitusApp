package com.ochoastack.habitus.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.DayState
import com.ochoastack.habitus.databinding.ItemCalendarDayBinding

data class CalendarDayItem(    // Declaramos el modelo de datos para el calendario
    val dayOfMonth: Int?,
    val fecha: String?,
    val estado: DayState
)

class MonthCalendarAdapter(    // Declaramos el adaptador para el calendario
    private var items: List<CalendarDayItem>
) : RecyclerView.Adapter<MonthCalendarAdapter.DayVH>() {

    inner class DayVH(val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DayVH(ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: DayVH, position: Int) {
        val item = items[position]

        if (item.dayOfMonth == null) {
            holder.binding.tvCalDayNumber.text       = ""
            holder.binding.tvCalDayNumber.background = null
            return
        }

        val ctx = holder.binding.root.context
        holder.binding.tvCalDayNumber.text = item.dayOfMonth.toString()

        when (item.estado) {
            DayState.COMPLETED -> {
                holder.binding.tvCalDayNumber
                    .setBackgroundResource(R.drawable.bg_day_completed)
                holder.binding.tvCalDayNumber
                    .setTextColor(ctx.getColor(R.color.on_accent))
            }
            DayState.TODAY -> {
                holder.binding.tvCalDayNumber
                    .setBackgroundResource(R.drawable.bg_day_today)
                holder.binding.tvCalDayNumber
                    .setTextColor(ctx.getColor(R.color.text_primary))
            }
            DayState.MISSED -> {
                holder.binding.tvCalDayNumber
                    .setBackgroundResource(R.drawable.bg_day_missed)
                holder.binding.tvCalDayNumber
                    .setTextColor(ctx.getColor(R.color.text_secondary))
            }
            DayState.NOT_APPLICABLE -> {
                holder.binding.tvCalDayNumber.background = null
                holder.binding.tvCalDayNumber
                    .setTextColor(ctx.getColor(R.color.text_hint))
            }
        }
    }
    // Actualizamos los datos del adaptador
    fun actualizar(nuevosItems: List<CalendarDayItem>) {
        items = nuevosItems
        notifyDataSetChanged()
    }
}
