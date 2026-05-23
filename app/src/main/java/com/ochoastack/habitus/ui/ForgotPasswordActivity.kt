package com.ochoastack.habitus.ui

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.FirebaseAuthRepository
import com.ochoastack.habitus.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    // Creamos el binding para el layout
    private lateinit var binding: ActivityForgotPasswordBinding

    // Declaramos el repositorio de autenticación
    private val authRepository = FirebaseAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Declaramos evento 'onClick' del botón de envío
        binding.btnSend.setOnClickListener {
            if (validarCampo()) {
                ejecutarEnvio()
            }
        }
        // Declaramos evento 'onClick' del enlace de login, regresamos al login
        binding.tvBackLogin.setOnClickListener {
            finish()
        }
        // Limpiamos error mientras el usuario escribe
        binding.edtCorreo.setOnFocusChangeListener { _, _ ->
            binding.tilEmail.error = null
        }
    }

    // Validamos que el correo tenga formato válido
    private fun validarCampo(): Boolean {
        val correo = binding.edtCorreo.text.toString().trim()

        return if (correo.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_empty_field)
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            false
        } else {
            binding.tilEmail.error = null
            true
        }
    }

    // Llamamos a 'Firebase Auth' para enviar el correo de restablecimiento
    private fun ejecutarEnvio() {
        val correo = binding.edtCorreo.text.toString().trim()
        // Mostramos estado de carga
        binding.btnSend.isEnabled = false
        binding.btnSend.text = getString(R.string.btn_send_loading)

        lifecycleScope.launch {
            val resultado = authRepository.enviarCorreoRestablecimiento(correo)

            resultado.fold(
                onSuccess = {
                    mostrarConfirmacion() // Revelamos información de éxito
                },
                onFailure = {
                    // Restauramos el botón en caso de error de red
                    binding.btnSend.isEnabled = true
                    binding.btnSend.text = getString(R.string.btn_send_link)
                    binding.tilEmail.error = getString(R.string.error_invalid_email)
                },
            )
        }
    }

    // Ocultamos el formulario y mostramos el mensaje de confirmación
    private fun mostrarConfirmacion() {
        binding.tilEmail.visibility = View.GONE
        binding.btnSend.visibility = View.GONE
        binding.tvTitle.text = getString(R.string.forgot_success_title)
        binding.tvSubtitle.text = getString(R.string.forgot_success_message)
        binding.tvBackLogin.visibility = View.VISIBLE
    }
}
