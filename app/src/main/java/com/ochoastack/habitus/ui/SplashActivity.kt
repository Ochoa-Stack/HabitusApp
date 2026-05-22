package com.ochoastack.habitus.ui

import dagger.hilt.android.AndroidEntryPoint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ochoastack.habitus.databinding.ActivitySplashBinding

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    // Creamos el binding para el layout
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animarEntrada()
    }
    private fun animarEntrada() {
        binding.ivLogo.animate()    // Primeramente animamos el logo
            .alpha(1f)
            .translationYBy(-20f)
            .setDuration(600)
            .setStartDelay(200)
            .withEndAction {
                binding.tvAppName.animate()    // Seguido de ello, el nombre de la app
                    .alpha(1f)
                    .translationYBy(-12f)
                    .setDuration(500)
                    .setStartDelay(0)
                    .withEndAction {
                        binding.tvTagline.animate()    // Finalmente, el tagline
                            .translationYBy(-12f)
                            .alpha(1f)
                            .setDuration(400)
                            .setStartDelay(100)
                            .withEndAction {
                                binding.root.postDelayed({
                                    navegarSiguientePantalla()
                                }, 800)
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }
    // Decidimos a dónde ir según si el onboarding ya fue visto
    private fun navegarSiguientePantalla() {
        val prefs = getSharedPreferences("habitus_prefs", Context.MODE_PRIVATE)
        val onboardingVisto = prefs.getBoolean("onboarding_completado", false)
        val destino = if (onboardingVisto) {
            // Vamos directo al Login (usuario recurrente)
            InicioSesionInterfaz::class.java
        } else {
            // Visualizamos el onboarding (usuario primerizo)
            OnboardingActivity::class.java
        }
        val intent = Intent(this, destino)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
