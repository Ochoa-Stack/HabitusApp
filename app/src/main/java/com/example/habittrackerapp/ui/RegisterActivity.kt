package com.example.habittrackerapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.habittrackerapp.MainActivity
import com.example.habittrackerapp.R
import com.example.habittrackerapp.data.FirebaseAuthRepository
import com.example.habittrackerapp.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    // Creamos el binding para el layout
    private lateinit var binding: ActivityRegisterBinding
    // Declaramos el repositorio de autenticación
    private val authRepository = FirebaseAuthRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Declaramos evento 'onClick' del botón de registro
        binding.btnRegister.setOnClickListener {
            if (validarCampos()) {
                ejecutarRegistro()
            }
        }
        //Declaramos evento 'onClick' del enlace de login, regresamos al login
        binding.tvLoginLink.setOnClickListener {
            finish()
        }

        binding.btnGoogleRegister.visibility = android.view.View.VISIBLE

        binding.btnGoogleRegister.setOnClickListener {
            ejecutarRegistroGoogle()
        }
        // Limpiamos errores mientras el usuario escribe
        binding.edtName.setOnFocusChangeListener { _, _ ->
            binding.tilName.error = null
        }
        binding.edtCorreo.setOnFocusChangeListener { _, _ ->
            binding.tilEmail.error = null
        }
        binding.edtPassword.setOnFocusChangeListener { _, _ ->
            binding.tilPassword.error = null
        }
        binding.edtConfirmPassword.setOnFocusChangeListener { _, _ ->
            binding.tilConfirmPassword.error = null
        }
    }
    // Validamos todos los campos del formulario
    private fun validarCampos(): Boolean {
        val nombre     = binding.edtName.text.toString().trim()
        val correo     = binding.edtCorreo.text.toString().trim()
        val contraseña = binding.edtPassword.text.toString()
        val confirmar  = binding.edtConfirmPassword.text.toString()
        var esValido   = true

        if (nombre.isEmpty()) {
            binding.tilName.error = getString(R.string.error_empty_field)
            esValido = false
        } else {
            binding.tilName.error = null
        }

        if (correo.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_empty_field)
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            esValido = false
        } else {
            binding.tilEmail.error = null
        }

        if (contraseña.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_empty_field)
            esValido = false
        } else if (contraseña.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            esValido = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmar.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.error_empty_field)
            esValido = false
        } else if (contraseña != confirmar) {
            binding.tilConfirmPassword.error = getString(R.string.error_passwords_dont_match)
            esValido = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return esValido
    }
    // Llamamos a 'Firebase Auth' para crear la cuenta
    private fun ejecutarRegistro() {
        val nombre     = binding.edtName.text.toString().trim()
        val correo     = binding.edtCorreo.text.toString().trim()
        val contraseña = binding.edtPassword.text.toString()
        // Mostramos estado de carga
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text      = getString(R.string.btn_register_loading)

        lifecycleScope.launch {
            val resultado = authRepository.registrarUsuario(nombre, correo, contraseña)

            resultado.fold(
                onSuccess = {
                    // Si el registro es exitoso, navegamos a 'Home' directamente
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                },
                onFailure = { error ->
                    // Si se produce un error, restauramos el botón y mostramos el mensaje
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text      = getString(R.string.btn_register)
                    
                    val mensajeError = when {
                        error.message?.contains("email address is already in use", ignoreCase = true) == true -> 
                            "Este correo ya está registrado"
                        error.message?.contains("network error", ignoreCase = true) == true ->
                            "Error de red. Revisa tu conexión"
                        else -> error.message ?: "Error al crear la cuenta"
                    }
                    
                    android.widget.Toast.makeText(this@RegisterActivity, mensajeError, android.widget.Toast.LENGTH_LONG).show()
                    binding.tilEmail.error = mensajeError
                }
            )
        }
    }

    private fun ejecutarRegistroGoogle() {
        binding.btnGoogleRegister.isEnabled = false

        val webClientId = getString(R.string.default_web_client_id)

        lifecycleScope.launch {
            val resultado = authRepository.iniciarSesionConGoogle(
                context     = this@RegisterActivity,
                webClientId = webClientId
            )

            resultado.fold(
                onSuccess = {
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                   Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                },
                onFailure = { error ->
                    binding.btnGoogleRegister.isEnabled = true
                    android.widget.Toast.makeText(
                        this@RegisterActivity,
                        error.message ?: getString(R.string.error_invalid_credentials),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}
