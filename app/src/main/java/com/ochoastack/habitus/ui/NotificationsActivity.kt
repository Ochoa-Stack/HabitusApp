package com.ochoastack.habitus.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ochoastack.habitus.databinding.ActivityNotificationsBinding
import com.ochoastack.habitus.utils.NotificationHelper

class NotificationsActivity : AppCompatActivity() {    // Declaramos el fragmento de notificaciones
    // Declaramos las variables de estado
    private lateinit var binding: ActivityNotificationsBinding
    // Declaramos las constantes de Intent
    companion object {
        const val PREFS_NAME        = "habitus_prefs"
        const val KEY_NOTIF_ENABLED = "notif_enabled"
        const val KEY_NOTIF_TIME    = "notif_time"
        const val TIME_MORNING      = "morning"
        const val TIME_AFTERNOON    = "afternoon"
        const val TIME_EVENING      = "evening"
    }
    // Ignoramos el listener del switch cuando lo movemos programáticamente
    private var ignorarCambioSwitch = false
    /* Si el usuario concede el permiso, programamos el recordatorio;
    si lo deniega, revertimos el switch al estado apagado */
    private val solicitarPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            guardarYProgramar(activado = true)
        } else {
            ignorarCambioSwitch = true
            binding.switchNotifications.isChecked = false
            ignorarCambioSwitch = false
            guardarYProgramar(activado = false)
        }
    }
    // Configuramos el layout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Recuperamos el estado de las preferencias
        val prefs          = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifActivadas = prefs.getBoolean(KEY_NOTIF_ENABLED, false)
        val horaGuardada   = prefs.getString(KEY_NOTIF_TIME, TIME_MORNING) ?: TIME_MORNING
        // Configuramos el switch de notificaciones
        binding.switchNotifications.isChecked = notifActivadas
        actualizarSeleccionHora(horaGuardada)
        actualizarEstadoOpciones(notifActivadas)
        // Configuramos el listener del switch
        binding.switchNotifications.setOnCheckedChangeListener { _, activado ->
            if (ignorarCambioSwitch) return@setOnCheckedChangeListener

            if (activado && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permiso = android.Manifest.permission.POST_NOTIFICATIONS
                if (checkSelfPermission(permiso) != PackageManager.PERMISSION_GRANTED) {
                    solicitarPermiso.launch(permiso)
                    return@setOnCheckedChangeListener
                }
            }
            guardarYProgramar(activado)
        }
        // Configuramos los botones de hora
        binding.optionMorning.setOnClickListener {
            guardarHora(TIME_MORNING)
            actualizarSeleccionHora(TIME_MORNING)
            reprogramarSiActivo()
        }
        binding.optionAfternoon.setOnClickListener {
            guardarHora(TIME_AFTERNOON)
            actualizarSeleccionHora(TIME_AFTERNOON)
            reprogramarSiActivo()
        }
        binding.optionEvening.setOnClickListener {
            guardarHora(TIME_EVENING)
            actualizarSeleccionHora(TIME_EVENING)
            reprogramarSiActivo()
        }
        // Configuramos el botón de retroceso
        binding.btnBack.setOnClickListener { finish() }
    }
    // Guardamos el estado del toggle y programa o cancela el recordatorio según corresponda
    private fun guardarYProgramar(activado: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NOTIF_ENABLED, activado).apply()

        actualizarEstadoOpciones(activado)

        if (activado) {
            val hora = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_NOTIF_TIME, TIME_MORNING) ?: TIME_MORNING
            NotificationHelper.programarRecordatorio(this, hora)
        } else {
            NotificationHelper.cancelarRecordatorio(this)
        }
    }
    // Reprogramamos el recordatorio con la nueva hora si las notificaciones están activas
    private fun reprogramarSiActivo() {
        val activo = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIF_ENABLED, false)
        if (!activo) return

        val hora = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NOTIF_TIME, TIME_MORNING) ?: TIME_MORNING
        NotificationHelper.programarRecordatorio(this, hora)
    }
    // Guardamos la hora seleccionada
    private fun guardarHora(hora: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_NOTIF_TIME, hora).apply()
    }
    // Actualizamos la selección de hora
    private fun actualizarSeleccionHora(horaSeleccionada: String) {
        binding.checkMorning.visibility   =
            if (horaSeleccionada == TIME_MORNING)   View.VISIBLE else View.GONE
        binding.checkAfternoon.visibility =
            if (horaSeleccionada == TIME_AFTERNOON) View.VISIBLE else View.GONE
        binding.checkEvening.visibility   =
            if (horaSeleccionada == TIME_EVENING)   View.VISIBLE else View.GONE
    }
    // Actualizamos el estado de las opciones de hora
    private fun actualizarEstadoOpciones(activado: Boolean) {
        val alpha = if (activado) 1.0f else 0.4f
        binding.optionMorning.alpha   = alpha
        binding.optionAfternoon.alpha = alpha
        binding.optionEvening.alpha   = alpha
        binding.optionMorning.isEnabled   = activado
        binding.optionAfternoon.isEnabled = activado
        binding.optionEvening.isEnabled   = activado
    }
}
