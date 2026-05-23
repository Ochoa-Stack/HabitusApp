package com.ochoastack.habitus.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.ochoastack.habitus.R
import com.ochoastack.habitus.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {
    // Creamos el binding para el layout
    private lateinit var binding: ActivityOnboardingBinding

    // Declaramos una lista de 'dots' visuales para los indicadores de página
    private val dots = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Configuramos el ViewPager con las tres páginas
        val paginas =
            listOf(
                OnboardingPage(
                    titulo = getString(R.string.onboarding_title_1),
                    cuerpo = getString(R.string.onboarding_body_1),
                    iconRes = R.drawable.ic_leaf,
                ),
                OnboardingPage(
                    titulo = getString(R.string.onboarding_title_2),
                    cuerpo = getString(R.string.onboarding_body_2),
                    iconRes = R.drawable.ic_onboarding_habit,
                ),
                OnboardingPage(
                    titulo = getString(R.string.onboarding_title_3),
                    cuerpo = getString(R.string.onboarding_body_3),
                    iconRes = R.drawable.ic_onboarding_streak,
                ),
            )
        binding.viewPager.adapter = OnboardingAdapter(paginas)
        // Construimos los 'dots' indicadores dinámicamente
        construirDots(paginas.size)
        actualizarDots(0)
        // Escuchamos los cambios de página para actualizar dots y botón
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    actualizarDots(position)
                    actualizarBoton(position, paginas.size)
                    // Ocultamos el botón omitir en la última página
                    binding.tvSkip.visibility =
                        if (position == paginas.size - 1) View.GONE else View.VISIBLE
                }
            },
        )
        // Definimos el evento del botón 'siguiente / comenzar'
        binding.btnNext.setOnClickListener {
            val paginaActual = binding.viewPager.currentItem
            if (paginaActual < paginas.size - 1) {
                binding.viewPager.currentItem = paginaActual + 1 // Avanzamos a la siguiente página
            } else {
                // Marcamos el onboarding como visto y navegamos al login (ultima página)
                marcarOnboardingVisto()
                navegarAlLogin()
            }
        }
        // Declaramos el evento de 'Omitir', va directo al login desde cualquier página
        binding.tvSkip.setOnClickListener {
            marcarOnboardingVisto()
            navegarAlLogin()
        }
    }

    // Construimos los 'dots' como ImageViews de forma dinámica
    private fun construirDots(cantidad: Int) {
        dots.clear()
        binding.layoutDots.removeAllViews()

        repeat(cantidad) { index ->
            val dot =
                ImageView(this).apply {
                    setImageResource(R.drawable.bg_day_today)
                    layoutParams =
                        android.widget.LinearLayout.LayoutParams(
                            12, 12,
                        ).apply {
                            setMargins(6, 0, 6, 0)
                        }
                }
            dots.add(dot)
            binding.layoutDots.addView(dot)
        }
    }

    // Actualizamos el color de los dots según la página activa
    private fun actualizarDots(paginaActiva: Int) {
        dots.forEachIndexed { index, dot ->
            dot.setImageResource(
                if (index == paginaActiva) {
                    R.drawable.bg_day_completed
                } else {
                    R.drawable.bg_day_today
                },
            )
        }
    }

    // Cambiamos el texto del botón en la última página
    private fun actualizarBoton(
        posicion: Int,
        total: Int,
    ) {
        binding.btnNext.text =
            getString(
                if (posicion == total - 1) {
                    R.string.onboarding_start
                } else {
                    R.string.onboarding_next
                },
            )
    }

    // Guardamos en 'SharedPreferences' que el onboarding ya fue visto
    private fun marcarOnboardingVisto() {
        getSharedPreferences("habitus_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completado", true)
            .apply()
    }

    // Navegamos al login limpiando el back stack
    private fun navegarAlLogin() {
        val intent = Intent(this, InicioSesionInterfaz::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
