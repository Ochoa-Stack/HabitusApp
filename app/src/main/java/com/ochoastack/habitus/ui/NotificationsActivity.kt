package com.ochoastack.habitus.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ochoastack.habitus.R
import com.ochoastack.habitus.databinding.ActivityNotificationsBinding
import com.ochoastack.habitus.utils.NotificationHelper

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding

    private var horaSeleccionada = 8
    private var minutoSeleccionado = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        cargarPreferencias()
        configurarSwitch()
        configurarSelectorHora()
        configurarBotonGuardar()
    }

    private fun cargarPreferencias() {
        val (hora, minuto, habilitado) =
            NotificationHelper.leerPreferenciaRecordatorio(this)
        horaSeleccionada = hora
        minutoSeleccionado = minuto

        binding.switchRecordatorio.isChecked = habilitado
        actualizarTextoHora(hora, minuto)
        actualizarVisibilidadSelector(habilitado)
    }

    private fun configurarSwitch() {
        binding.switchRecordatorio.setOnCheckedChangeListener { _, isChecked ->
            actualizarVisibilidadSelector(isChecked)
        }
    }

    private fun configurarSelectorHora() {
        binding.btnSeleccionarHora.setOnClickListener {
            val timePicker =
                android.app.TimePickerDialog(
                    this,
                    { _, hora, minuto ->
                        horaSeleccionada = hora
                        minutoSeleccionado = minuto
                        actualizarTextoHora(hora, minuto)
                    },
                    horaSeleccionada,
                    minutoSeleccionado,
                    // formato 24h
                    true,
                )
            timePicker.show()
        }
    }

    private fun configurarBotonGuardar() {
        binding.btnGuardarRecordatorio.setOnClickListener {
            val habilitado = binding.switchRecordatorio.isChecked

            NotificationHelper.guardarPreferenciaRecordatorio(
                this,
                horaSeleccionada,
                minutoSeleccionado,
                habilitado,
            )

            if (habilitado) {
                NotificationHelper.programarRecordatorioDiario(
                    this,
                    horaSeleccionada,
                    minutoSeleccionado,
                )
            } else {
                NotificationHelper.cancelarRecordatorioDiario(this)
            }

            android.widget.Toast.makeText(
                this,
                getString(R.string.notif_guardado_ok),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun actualizarTextoHora(
        hora: Int,
        minuto: Int,
    ) {
        val horaFormateada = String.format("%02d:%02d", hora, minuto)
        binding.btnSeleccionarHora.text = horaFormateada
    }

    private fun actualizarVisibilidadSelector(visible: Boolean) {
        binding.llSelectorHora.visibility =
            if (visible) View.VISIBLE else View.GONE
        binding.btnGuardarRecordatorio.visibility =
            if (visible) View.VISIBLE else View.GONE

        if (!visible) {
            NotificationHelper.cancelarRecordatorioDiario(this)
            NotificationHelper.guardarPreferenciaRecordatorio(
                this,
                horaSeleccionada,
                minutoSeleccionado,
                false,
            )
        }
    }
}
