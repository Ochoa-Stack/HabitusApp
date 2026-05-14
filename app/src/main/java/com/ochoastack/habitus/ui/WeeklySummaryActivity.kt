package com.ochoastack.habitus.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.HabitRepository
import com.ochoastack.habitus.data.ResumenSemanal
import com.ochoastack.habitus.databinding.ActivityWeeklySummaryBinding
import kotlinx.coroutines.launch

class WeeklySummaryActivity : AppCompatActivity() {
    // Declaramos las variables necesarias
    private lateinit var binding: ActivityWeeklySummaryBinding
    private val habitRepository = HabitRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeeklySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        cargarResumen()
    }
    // Cargamos el resumen semanal
    private fun cargarResumen() {
        binding.progressBarSemanal.visibility = android.view.View.VISIBLE
        binding.scrollViewContenido.visibility = android.view.View.GONE

        lifecycleScope.launch {
            val resultado = habitRepository.obtenerResumenSemanal()
            binding.progressBarSemanal.visibility = android.view.View.GONE
            resultado.fold(
                onSuccess = { resumen -> 
                    binding.scrollViewContenido.visibility = android.view.View.VISIBLE
                    mostrarResumen(resumen) 
                },
                onFailure = {
                    android.widget.Toast.makeText(
                        this@WeeklySummaryActivity,
                        getString(R.string.error_cargar_datos),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            )
        }
    }
    // Mostramos el resumen semanal
    private fun mostrarResumen(resumen: ResumenSemanal) {
        binding.tvPorcentajeSemana.text = "${resumen.porcentajeSemana}%"

        binding.tvCompletacionesDetalle.text = getString(
            R.string.weekly_detail_completaciones,
            resumen.completacionesSemana,
            resumen.totalProgramadasSemana
        )

        binding.tvRachaMaxima.text = resumen.rachaMaxima.toString()

        binding.tvHabitoMejorRacha.text =
            resumen.habitoMejorRacha.ifEmpty { getString(R.string.weekly_none) }

        binding.tvHabitoDescuidado.text =
            resumen.habitoMasDescuidado ?: getString(R.string.weekly_all_good)

        binding.tvMensajeSemanal.text = generarMensajeSemanal(resumen)
    }
    // Generamos el mensaje semanal en función del resumen
    private fun generarMensajeSemanal(resumen: ResumenSemanal): String = when {
        resumen.porcentajeSemana == 100 ->
            getString(R.string.weekly_msg_perfect)
        resumen.porcentajeSemana >= 80 ->
            getString(R.string.weekly_msg_great, resumen.porcentajeSemana)
        resumen.porcentajeSemana >= 50 ->
            getString(R.string.weekly_msg_good)
        resumen.totalHabitos == 0 ->
            getString(R.string.weekly_msg_no_habits)
        else ->
            getString(R.string.weekly_msg_keep_going)
    }
}
