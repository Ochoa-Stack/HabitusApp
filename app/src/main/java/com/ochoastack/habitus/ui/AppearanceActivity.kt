package com.ochoastack.habitus.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.ochoastack.habitus.databinding.ActivityAppearanceBinding

class AppearanceActivity : AppCompatActivity() {
    // Creamos el binding
    private lateinit var binding: ActivityAppearanceBinding

    // Declaramos las claves de 'SharedPreferences' para el tema
    companion object {
        const val PREFS_NAME = "habitus_prefs"
        const val KEY_THEME = "app_theme"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppearanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Cargamos el tema guardado y marcamos la opción correcta
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val temaGuardado = prefs.getString(KEY_THEME, THEME_LIGHT) ?: THEME_LIGHT
        actualizarSeleccion(temaGuardado)
        // Declaramos los eventos de las opciones de tema
        binding.optionLight.setOnClickListener { aplicarTema(THEME_LIGHT) }
        binding.optionDark.setOnClickListener { aplicarTema(THEME_DARK) }
        binding.optionSystem.setOnClickListener { aplicarTema(THEME_SYSTEM) }

        binding.btnBack.setOnClickListener { finish() }
    }

    // Aplicamos el tema seleccionado inmediatamente y lo guardamos
    private fun aplicarTema(tema: String) {
        // Guardamos la preferencia
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, tema)
            .apply()
        // Aplicamos el tema con 'AppCompatDelegate', se refleja instantáneamente
        val modo =
            when (tema) {
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        AppCompatDelegate.setDefaultNightMode(modo)
        // Actualizamos el check visual
        actualizarSeleccion(tema)
    }

    // Actualizamos el check visual según el tema activo
    private fun actualizarSeleccion(temaActivo: String) {
        binding.checkLight.visibility =
            if (temaActivo == THEME_LIGHT) View.VISIBLE else View.GONE
        binding.checkDark.visibility =
            if (temaActivo == THEME_DARK) View.VISIBLE else View.GONE
        binding.checkSystem.visibility =
            if (temaActivo == THEME_SYSTEM) View.VISIBLE else View.GONE
    }
}
