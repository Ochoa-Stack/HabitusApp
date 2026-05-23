package com.ochoastack.habitus.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ochoastack.habitus.data.FirebaseAuthRepository
import com.ochoastack.habitus.data.HabitRepository
import com.ochoastack.habitus.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authRepository = FirebaseAuthRepository()

    // Configuramos la inyección para acceder a los datos del usuario
    @Inject
    lateinit var habitRepository: HabitRepository
    private var statsYaCargadas = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val usuario = authRepository.obtenerUsuarioActual()

        if (usuario != null) {
            binding.tvUserEmail.text = usuario.email ?: ""

            viewLifecycleOwner.lifecycleScope.launch {
                if (_binding == null) return@launch
                val resultado = authRepository.obtenerNombreUsuario()
                resultado.fold(
                    onSuccess = { nombre ->
                        binding.tvUserName.text = nombre
                        binding.tvAvatar.text = nombre.firstOrNull()?.uppercase() ?: "U"
                    },
                    onFailure = {
                        binding.tvUserName.text = "Usuario"
                        binding.tvAvatar.text = "U"
                    },
                )
            }
        }
        configurarListeners()
    }

    override fun onResume() {
        super.onResume()
        if (!statsYaCargadas) {
            cargarEstadisticas()
        }
    }

    private fun cargarEstadisticas() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (_binding == null) return@launch
            val resultado = habitRepository.obtenerEstadisticas()
            resultado.fold(
                onSuccess = { stats ->
                    binding.tvStatHabits.text = stats.totalHabitos.toString()
                    binding.tvStatStreak.text = stats.rachaMaxima.toString()
                    binding.tvStatCompleted.text = stats.totalCompletaciones.toString()
                    statsYaCargadas = true
                },
                onFailure = {
                    binding.tvStatHabits.text = "0"
                    binding.tvStatStreak.text = "0"
                    binding.tvStatCompleted.text = "0"
                },
            )
        }
    }

    private fun configurarListeners() {
        binding.optionNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
        binding.optionTheme.setOnClickListener {
            startActivity(Intent(requireContext(), AppearanceActivity::class.java))
        }
        binding.optionPrivacy.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyActivity::class.java))
        }
        binding.optionAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            authRepository.cerrarSesion()
            val intent = Intent(requireContext(), InicioSesionInterfaz::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
