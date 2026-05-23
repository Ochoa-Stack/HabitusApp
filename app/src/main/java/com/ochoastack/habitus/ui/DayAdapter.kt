package com.ochoastack.habitus.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.DayState
import com.ochoastack.habitus.data.DayStatus
import com.ochoastack.habitus.databinding.ItemDayBinding

class DayAdapter(private val days: List<DayStatus>) :
    RecyclerView.Adapter<DayAdapter.DayVH>() {
    inner class DayVH(val binding: ItemDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = DayVH(ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = days.size

    override fun onBindViewHolder(
        holder: DayVH,
        position: Int,
    ) {
        val day = days[position]
        holder.binding.tvDayLabel.text = day.label
        holder.binding.tvDayNumber.text = day.dayNumber.toString()

        val ctx = holder.binding.root.context
        when (day.status) {
            DayState.COMPLETED -> {
                holder.binding.tvDayNumber.setBackgroundResource(R.drawable.bg_day_completed)
                holder.binding.tvDayNumber.setTextColor(ctx.getColor(R.color.on_accent))
            }
            DayState.TODAY -> {
                holder.binding.tvDayNumber.setBackgroundResource(R.drawable.bg_day_today)
                holder.binding.tvDayNumber.setTextColor(ctx.getColor(R.color.text_primary))
            }
            DayState.MISSED -> {
                holder.binding.tvDayNumber.setBackgroundResource(R.drawable.bg_day_missed)
                holder.binding.tvDayNumber.setTextColor(ctx.getColor(R.color.text_secondary))
            }
            DayState.NOT_APPLICABLE -> {
                holder.binding.tvDayNumber.background = null
                holder.binding.tvDayNumber.setTextColor(ctx.getColor(R.color.text_hint))
            }
        }
    }
}
