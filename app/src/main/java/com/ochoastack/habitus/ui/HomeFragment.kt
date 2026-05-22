package com.ochoastack.habitus.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.TipoCognitivo
import com.ochoastack.habitus.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import java.util.Calendar

/* Fragment de la pantalla de inicio */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // viewModels() crea el VM con el scope del Fragment; sobrevive a rotaciones
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarSaludo()
        configurarFecha()
        configurarNavegacion()
        observarEstado()
    }

    // Navegación

    private fun configurarNavegacion() {
        binding.tvBtnResumenSemanal.setOnClickListener {
            startActivity(Intent(requireContext(), WeeklySummaryActivity::class.java))
        }
        binding.fabFocus.setOnClickListener {
            startActivity(Intent(requireContext(), FocusModeActivity::class.java))
        }
    }

    // Observación de estado

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle cancela y relanza la colección según el ciclo de vida,
            // evitando actualizaciones de UI cuando el Fragment está en background
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado -> renderEstado(estado) }
            }
        }
    }

    private fun renderEstado(estado: HomeUiState) {
        when (estado) {
            is HomeUiState.Loading -> mostrarCarga()
            is HomeUiState.Success -> mostrarExito(estado)
            is HomeUiState.Error -> mostrarError(estado.mensaje)
        }
    }

    private fun mostrarCarga() {
        binding.tvMensajeMotivacional.text = getString(R.string.home_msg_loading)
    }

    private fun mostrarExito(estado: HomeUiState.Success) {
        // Estadísticas
        binding.tvStatActivos.text = estado.estadisticas.totalHabitos.toString()
        binding.tvStatCompletados.text = estado.estadisticas.completadosHoy.toString()
        binding.tvStatRacha.text = estado.estadisticas.rachaMaxima.toString()

        // Mensaje motivacional
        binding.tvMensajeMotivacional.text = when (estado.tipoMensaje) {
            TipoMensaje.PERFECTO -> getString(R.string.home_msg_perfect)
            TipoMensaje.EXCELENTE -> getString(R.string.home_msg_great)
            TipoMensaje.BIEN -> getString(R.string.home_msg_good)
            TipoMensaje.RACHA -> getString(R.string.home_msg_streak, estado.estadisticas.rachaMaxima)
            TipoMensaje.COMENZAR -> getString(R.string.home_msg_start)
        }

        // Balance cognitivo
        if (estado.tieneBalanceCognitivo) {
            binding.cardBalanceCognitivo.visibility = View.VISIBLE
            mostrarBarrasBalance(estado)
        } else {
            binding.cardBalanceCognitivo.visibility = View.GONE
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.tvStatActivos.text = "0"
        binding.tvStatCompletados.text = "0"
        binding.tvStatRacha.text = "0"
        binding.tvMensajeMotivacional.text = getString(R.string.home_msg_start)
        Toast.makeText(requireContext(), getString(R.string.error_cargar_datos), Toast.LENGTH_SHORT).show()
    }

    // Barras de balance cognitivo

    private fun mostrarBarrasBalance(estado: HomeUiState.Success) {
        val llBarras = binding.llBalanceBars
        llBarras.removeAllViews()

        val total = estado.balanceCognitivo.values.sum()
        if (total == 0) return

        estado.tiposActivos.forEach { (tipo, cantidad) ->
            val porcentaje = (cantidad * 100f / total).toInt()
            val colorRes = TipoCognitivo.colorRes(tipo)
            val emoji = TipoCognitivo.emoji(tipo)
            llBarras.addView(crearFilaBalance(tipo, cantidad, porcentaje, colorRes, emoji))
        }
    }

    private fun crearFilaBalance(
        tipo: String,
        cantidad: Int,
        porcentaje: Int,
        colorRes: Int,
        emoji: String,
    ): View {
        val fila = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = 10 }
        }

        val etiqueta = TextView(requireContext()).apply {
            text = "$emoji $tipo — $cantidad"
            textSize = 12f
            setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val barraContenedor = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ALTURA_BARRA_DP,
            ).apply { topMargin = 4 }
            background = requireContext().getDrawable(R.drawable.bg_progress_track)
        }

        val barraRelleno = View(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(0, ALTURA_BARRA_DP)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(requireContext().getColor(colorRes))
                cornerRadius = RADIO_BARRA_PX
            }
            viewTreeObserver.addOnGlobalLayoutListener(
                object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        val anchoTotal = barraContenedor.width
                        layoutParams.width = (anchoTotal * porcentaje / 100f).toInt()
                        requestLayout()
                    }
                },
            )
        }

        barraContenedor.addView(barraRelleno)
        fila.addView(etiqueta)
        fila.addView(barraContenedor)
        return fila
    }

    // Presentación local (sin datos de red)

    private fun configurarSaludo() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hora < 12 -> getString(R.string.home_greeting_morning)
            hora < 18 -> getString(R.string.home_greeting_afternoon)
            else -> getString(R.string.home_greeting_evening)
        }
    }

    private fun configurarFecha() {
        val cal = Calendar.getInstance()
        val diasSemana = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val meses = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        )
        val diaSemana = diasSemana[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val dia = cal.get(Calendar.DAY_OF_MONTH)
        val mes = meses[cal.get(Calendar.MONTH)]
        binding.tvDate.text = "$diaSemana, $dia de $mes"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ALTURA_BARRA_DP = 8
        const val RADIO_BARRA_PX = 8f
    }
}
