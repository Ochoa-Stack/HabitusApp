package com.example.habittrackerapp.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittrackerapp.R
import com.example.habittrackerapp.data.Habit
import com.example.habittrackerapp.data.HabitRepository
import com.example.habittrackerapp.data.TipoCognitivo
import com.example.habittrackerapp.databinding.ActivityFocusModeBinding
import com.example.habittrackerapp.databinding.ItemFocusHabitBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FocusModeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFocusModeBinding
    private val habitRepository = HabitRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ocultar status bar para inmersión total
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        binding.rvHabitosHoy.layoutManager = LinearLayoutManager(this)
        binding.btnCerrarEnfoque.setOnClickListener { finish() }

        // Subtítulo con fecha formateada
        val fechaFormateada = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es"))
            .format(Date())
            .replaceFirstChar { it.uppercase() }
        binding.tvEnfoqueSubtitulo.text = fechaFormateada

        cargarHabitosHoy()
    }

    private fun cargarHabitosHoy() {
        lifecycleScope.launch {
            val resultado = habitRepository.obtenerHabitos()
            resultado.fold(
                onSuccess = { habitos ->
                    // Filtrar hábitos programados para hoy
                    val hoy = SimpleDateFormat("EEEE", Locale("es"))
                        .format(Date())
                        .replaceFirstChar { it.uppercase() }
                        .take(3)    // "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"

                    // Mapeo del día en español a la etiqueta guardada en diasSemana
                    val diaSemanaHoy = obtenerEtiquetaDiaHoy()

                    val habitosHoy = habitos.filter { habito ->
                        habito.diasSemana.contains(diaSemanaHoy) ||
                        habito.frecuencia.contains("Diario", ignoreCase = true)
                    }

                    if (habitosHoy.isEmpty()) {
                        binding.llEnfoqueVacio.visibility = View.VISIBLE
                        binding.rvHabitosHoy.visibility   = View.GONE
                    } else {
                        binding.llEnfoqueVacio.visibility = View.GONE
                        binding.rvHabitosHoy.visibility   = View.VISIBLE
                        binding.rvHabitosHoy.adapter = FocusHabitAdapter(
                            habitosHoy.toMutableList(),
                            onCompletar = { habito -> completarHabito(habito) }
                        )
                    }
                },
                onFailure = { finish() }
            )
        }
    }

    // Devuelve la etiqueta del día de hoy tal como se guarda en Firestore
    private fun obtenerEtiquetaDiaHoy(): String {
        val cal = java.util.Calendar.getInstance()
        return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY    -> "Lun"
            java.util.Calendar.TUESDAY   -> "Mar"
            java.util.Calendar.WEDNESDAY -> "Mié"
            java.util.Calendar.THURSDAY  -> "Jue"
            java.util.Calendar.FRIDAY    -> "Vie"
            java.util.Calendar.SATURDAY  -> "Sáb"
            java.util.Calendar.SUNDAY    -> "Dom"
            else -> "Lun"
        }
    }

    private fun completarHabito(habito: Habit) {
        lifecycleScope.launch {
            habitRepository.completarHabito(habito.id)
            // El adapter se encarga de actualizar visualmente el ítem
        }
    }
}

class FocusHabitAdapter(
    private val habitos: MutableList<Habit>,
    private val onCompletar: (Habit) -> Unit
) : RecyclerView.Adapter<FocusHabitAdapter.VH>() {

    // Rastrear cuáles ya se completaron localmente
    private val completadosLocal = mutableSetOf<String>()

    inner class VH(val binding: ItemFocusHabitBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFocusHabitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))

    override fun getItemCount() = habitos.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val habito = habitos[position]
        val yaCompletado = habito.estaCompletadoHoy || completadosLocal.contains(habito.id)

        with(holder.binding) {
            tvFocusNombre.text = habito.nombre
            tvFocusTipo.text   = "${TipoCognitivo.emoji(habito.tipoCognitivo)} ${habito.tipoCognitivo}"

            // Punto de color según tipo cognitivo
            viewTipoColor.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(root.context.getColor(TipoCognitivo.colorRes(habito.tipoCognitivo)))
            }

            if (yaCompletado) {
                btnFocusCompletar.setColorFilter(
                    root.context.getColor(R.color.verde_salvia)
                )
                btnFocusCompletar.isEnabled    = false
                tvFocusNombre.alpha            = 0.5f
            } else {
                btnFocusCompletar.setColorFilter(
                    root.context.getColor(R.color.divider_color)
                )
                btnFocusCompletar.isEnabled    = true
                tvFocusNombre.alpha            = 1.0f

                btnFocusCompletar.setOnClickListener {
                    completadosLocal.add(habito.id)
                    onCompletar(habito)
                    notifyItemChanged(position)
                }
            }
        }
    }
}
