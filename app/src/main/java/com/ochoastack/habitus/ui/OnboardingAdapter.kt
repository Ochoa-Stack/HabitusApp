package com.ochoastack.habitus.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ochoastack.habitus.databinding.FragmentOnboardingPageBinding

// Declaramos los datos de cada página del onboarding
data class OnboardingPage(
    val titulo: String,
    val cuerpo: String,
    val iconRes: Int,
)

class OnboardingAdapter(
    private val paginas: List<OnboardingPage>,
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingVH>() {
    inner class OnboardingVH(val binding: FragmentOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = OnboardingVH(
        FragmentOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        ),
    )

    override fun getItemCount() = paginas.size

    override fun onBindViewHolder(
        holder: OnboardingVH,
        position: Int,
    ) {
        val pagina = paginas[position]
        with(holder.binding) {
            tvTitle.text = pagina.titulo
            tvBody.text = pagina.cuerpo
            ivIllustration.setImageResource(pagina.iconRes)
        }
    }
}
