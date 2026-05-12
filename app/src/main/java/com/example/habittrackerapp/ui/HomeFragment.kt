package com.example.habittrackerapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.habittrackerapp.R
import com.example.habittrackerapp.data.HabitRepository
import com.example.habittrackerapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val habitRepository = HabitRepository()
    private var statsYaCargadas = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarSaludo()
        configurarFecha()
        cargarEstadisticas()

        binding.tvBtnResumenSemanal.setOnClickListener {
            startActivity(Intent(requireContext(), WeeklySummaryActivity::class.java))
        }

        binding.fabFocus.setOnClickListener {
            startActivity(Intent(requireContext(), FocusModeActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!statsYaCargadas) cargarEstadisticas()
    }

    fun invalidarStats() {
        statsYaCargadas = false
    }

    private fun configurarSaludo() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hora < 12 -> getString(R.string.home_greeting_morning)
            hora < 18 -> getString(R.string.home_greeting_afternoon)
            else      -> getString(R.string.home_greeting_evening)
        }
    }

    private fun configurarFecha() {
        val cal        = Calendar.getInstance()
        val diasSemana = listOf("Domingo", "Lunes", "Martes", "Miércoles",
            "Jueves", "Viernes", "Sábado")
        val meses = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        val diaSemana = diasSemana[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val dia       = cal.get(Calendar.DAY_OF_MONTH)
        val mes       = meses[cal.get(Calendar.MONTH)]
        binding.tvDate.text = "$diaSemana, $dia de $mes"
    }

    private fun cargarEstadisticas() {
        binding.tvMensajeMotivacional.text = getString(R.string.home_msg_loading)

        viewLifecycleOwner.lifecycleScope.launch {
            if (_binding == null) return@launch
            // Carga estadísticas del resumen principal
            val resultStats = habitRepository.obtenerEstadisticas()
            resultStats.fold(
                onSuccess = { stats ->
                    binding.tvStatActivos.text     = stats.totalHabitos.toString()
                    binding.tvStatCompletados.text = stats.completadosHoy.toString()
                    binding.tvStatRacha.text       = stats.rachaMaxima.toString()
                    statsYaCargadas = true
                },
                onFailure = {
                    binding.tvStatActivos.text     = "0"
                    binding.tvStatCompletados.text = "0"
                    binding.tvStatRacha.text       = "0"
                }
            )

            // Carga mensaje motivacional dinámico con datos semanales
            val resultSemana = habitRepository.obtenerPorcentajeSemana()
            resultStats.fold(
                onSuccess = { stats ->
                    val pct    = resultSemana.getOrElse { 0 }
                    val racha  = stats.rachaMaxima

                    binding.tvMensajeMotivacional.text = when {
                        pct == 100       -> getString(R.string.home_msg_perfect)
                        pct >= 80        -> getString(R.string.home_msg_great)
                        pct >= 50        -> getString(R.string.home_msg_good)
                        racha > 0        -> getString(R.string.home_msg_streak, racha)
                        else             -> getString(R.string.home_msg_start)
                    }
                },
                onFailure = {
                    binding.tvMensajeMotivacional.text = getString(R.string.home_msg_start)
                }
            )

            val balanceResult = habitRepository.obtenerBalanceCognitivo()
            balanceResult.fold(
                onSuccess = { balance -> mostrarBalanceCognitivo(balance) },
                onFailure = {
                    if (_binding == null) return@fold
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(R.string.error_cargar_datos),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun mostrarBalanceCognitivo(balance: Map<String, Int>) {
        val llBarras = binding.llBalanceBars
        llBarras.removeAllViews()

        val total = balance.values.sum()
        if (total == 0) {
            binding.cardBalanceCognitivo.visibility = View.GONE
            return
        }
        binding.cardBalanceCognitivo.visibility = View.VISIBLE

        com.example.habittrackerapp.data.TipoCognitivo.todos.forEach { tipo ->
            val cantidad = balance[tipo] ?: 0
            if (cantidad == 0) return@forEach

            val porcentaje = (cantidad * 100f / total).toInt()
            val colorRes   = com.example.habittrackerapp.data.TipoCognitivo.colorRes(tipo)
            val emoji      = com.example.habittrackerapp.data.TipoCognitivo.emoji(tipo)

            val fila = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 10 }
            }

            val etiqueta = android.widget.TextView(requireContext()).apply {
                text = "$emoji $tipo — $cantidad"
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                typeface = android.graphics.Typeface.create("dm_sans_medium", android.graphics.Typeface.NORMAL)
            }

            val barraContenedor = android.widget.FrameLayout(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, 8
                ).apply { topMargin = 4 }
                background = requireContext().getDrawable(R.drawable.bg_progress_track)
            }

            val barraRelleno = android.view.View(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(0, 8)
                setBackgroundColor(requireContext().getColor(colorRes))
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    setColor(requireContext().getColor(colorRes))
                    cornerRadius = 8f
                }
                viewTreeObserver.addOnGlobalLayoutListener(
                    object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            viewTreeObserver.removeOnGlobalLayoutListener(this)
                            val anchoTotal = barraContenedor.width
                            layoutParams.width = (anchoTotal * porcentaje / 100f).toInt()
                            requestLayout()
                        }
                    }
                )
            }

            barraContenedor.addView(barraRelleno)
            fila.addView(etiqueta)
            fila.addView(barraContenedor)
            llBarras.addView(fila)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
