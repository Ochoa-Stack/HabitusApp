package com.example.habittrackerapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.habittrackerapp.MainActivity
import com.example.habittrackerapp.R
import com.example.habittrackerapp.data.FirebaseAuthRepository
import com.example.habittrackerapp.databinding.ActivityIniciosesioninterfazBinding
import kotlinx.coroutines.launch

class InicioSesionInterfaz : AppCompatActivity() {

    // Creamos el binding para el layout
    private lateinit var binding: ActivityIniciosesioninterfazBinding
    // Declaramos el repositorio de autenticación
    private val authRepository = FirebaseAuthRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityIniciosesioninterfazBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Si ya hay una sesión activa, vamos directo a 'Home'
        if (authRepository.obtenerUsuarioActual() != null) {
            navegarAlHome()
            return
        }
        // Declaramos evento 'onClick' del botón de 'login'
        binding.btnLogin.setOnClickListener {
            if (validarCampos()) {
                ejecutarLogin()
            }
        }
        // Navegamos a 'registro'
        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        // Navegamos a 'recuperación de contraseña'
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.llDivider.visibility = android.view.View.VISIBLE
        binding.btnGoogle.visibility = android.view.View.VISIBLE

        binding.btnGoogle.setOnClickListener {
            ejecutarLoginGoogle()
        }
        // Limpiamos errores cuando el usuario empieza a escribir
        binding.edtCorreo.setOnFocusChangeListener { _, _ ->
            binding.tilEmail.error = null
        }
        binding.edtContrasena.setOnFocusChangeListener { _, _ ->
            binding.tilPassword.error = null
        }
    }
    // Validamos el formato de correo y longitud de contraseña
    private fun validarCampos(): Boolean {
        val correo     = binding.edtCorreo.text.toString().trim()
        val contraseña = binding.edtContrasena.text.toString()
        var esValido   = true

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

        return esValido
    }
    // Llamamos a 'Firebase Auth' con las credenciales del usuario
    private fun ejecutarLogin() {
        val correo     = binding.edtCorreo.text.toString().trim()
        val contraseña = binding.edtContrasena.text.toString()
        // Mostramos el estado de carga
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text      = getString(R.string.btn_loading)
        // Usamos 'lifecycleScope' para corrutinas vinculadas al ciclo de vida
        lifecycleScope.launch {
            val resultado = authRepository.iniciarSesion(correo, contraseña)

            resultado.fold(
                onSuccess = {
                    // En caso de login existoso, navegamos a 'Home'
                    navegarAlHome()
                },
                onFailure = { error ->
                    // En caso de login fallido, mostramos error y restauramos el botón
                    val mensajeError = when {
                        error.message?.contains("invalid-credential", ignoreCase = true) == true || 
                        error.message?.contains("wrong-password", ignoreCase = true) == true ||
                        error.message?.contains("no user", ignoreCase = true) == true -> 
                            getString(R.string.error_invalid_credentials)
                        error.message?.contains("network error", ignoreCase = true) == true ->
                            "Error de red. Revisa tu conexión"
                        else -> error.message ?: getString(R.string.error_invalid_credentials)
                    }

                    binding.tilEmail.error    = mensajeError
                    binding.tilPassword.error = mensajeError
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text      = getString(R.string.btn_login)
                    
                    android.widget.Toast.makeText(this@InicioSesionInterfaz, mensajeError, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    // Navegamos al 'Home' limpiando el back stack
    private fun navegarAlHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun ejecutarLoginGoogle() {
        binding.btnGoogle.isEnabled = false

        val webClientId = getString(R.string.default_web_client_id)

        lifecycleScope.launch {
            val resultado = authRepository.iniciarSesionConGoogle(
                context     = this@InicioSesionInterfaz,
                webClientId = webClientId
            )

            resultado.fold(
                onSuccess = {
                    navegarAlHome()
                },
                onFailure = { error ->
                    binding.btnGoogle.isEnabled = true
                    android.widget.Toast.makeText(
                        this@InicioSesionInterfaz,
                        error.message ?: getString(R.string.error_invalid_credentials),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}
