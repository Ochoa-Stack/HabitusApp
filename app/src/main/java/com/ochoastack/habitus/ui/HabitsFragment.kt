package com.ochoastack.habitus.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.DayState
import com.ochoastack.habitus.data.DayStatus
import com.ochoastack.habitus.data.Habit
import com.ochoastack.habitus.databinding.FragmentHabitsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import kotlinx.coroutines.launch

/* Fragment de la pantalla de lista de hábitos.
 *
 * Responsabilidades (solo UI):
 * - Observar [HabitsViewModel.uiState] y actualizar el RecyclerView.
 * - Gestionar el swipe-to-archive con undo via Snackbar.
 * - Navegar a pantallas de detalle y creación.
 *
 * El adapter se crea una sola vez y solo recibe [submitList] en recargas.
 * Toda lógica de datos y negocio vive en [HabitsViewModel]. */
@AndroidEntryPoint
class HabitsFragment : Fragment() {
    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HabitsViewModel by viewModels()

    // Adapter creado una vez — no se recrea en cada carga.
    private val habitAdapter: HabitAdapter by lazy {
        HabitAdapter { habit ->
            startActivity(
                Intent(requireContext(), HabitDetailActivity::class.java).apply {
                    putExtra(HabitDetailActivity.EXTRA_HABIT_ID, habit.id)
                    putExtra(HabitDetailActivity.EXTRA_HABIT_NAME, habit.nombre)
                    putExtra(HabitDetailActivity.EXTRA_HABIT_FREQUENCY, habit.frecuencia)
                    putExtra(HabitDetailActivity.EXTRA_HABIT_STREAK, habit.racha)
                    putExtra(HabitDetailActivity.EXTRA_HABIT_PERCENT, habit.porcentaje)
                    putExtra(HabitDetailActivity.EXTRA_HABIT_CATEGORY_ID, habit.categoriaId)
                },
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        configurarRecyclerView()
        configurarNavegacion()
        observarEstado()
    }

    // Configuración de vistas

    private fun configurarRecyclerView() {
        binding.rvHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHabits.adapter = habitAdapter
        configurarSwipe()
    }

    private fun configurarNavegacion() {
        binding.fab.setOnClickListener {
            startActivity(Intent(requireContext(), CreateHabitActivity::class.java))
        }
        binding.tvVerArchivados.setOnClickListener {
            startActivity(Intent(requireContext(), ArchivedHabitsActivity::class.java))
        }
    }

    // Observación de estado

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado -> renderEstado(estado) }
            }
        }
    }

    private fun renderEstado(estado: HabitsUiState) {
        when (estado) {
            is HabitsUiState.Loading -> {
                mostrarCarga(true)
                mostrarEstadoVacio(false)
            }
            is HabitsUiState.Empty -> {
                mostrarCarga(false)
                mostrarEstadoVacio(true)
            }
            is HabitsUiState.Success -> {
                mostrarCarga(false)
                mostrarEstadoVacio(false)
                val habitsConDias =
                    estado.habitos.map { habit ->
                        habit.copy(weekDays = construirDiasParaLista(habit.estaCompletadoHoy))
                    }
                habitAdapter.submitList(habitsConDias)
            }
            is HabitsUiState.Error -> {
                mostrarCarga(false)
                mostrarEstadoVacio(true)
            }
        }
    }

    // Swipe to archive

    private fun configurarSwipe() {
        val itemTouchHelper =
            ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                    0,
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                ) {
                    override fun onMove(
                        recyclerView: androidx.recyclerview.widget.RecyclerView,
                        viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                        target: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    ) = false

                    override fun onSwiped(
                        viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                        direction: Int,
                    ) {
                        val posicion = viewHolder.adapterPosition
                        val habito = habitAdapter.currentList[posicion]
                        archivarConUndo(habito)
                    }
                },
            )
        itemTouchHelper.attachToRecyclerView(binding.rvHabits)
    }

    private fun archivarConUndo(habito: Habit) {
        viewModel.archivarHabito(habito) {
            if (_binding == null) return@archivarHabito
            Snackbar.make(binding.root, getString(R.string.habit_archived), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.habit_undo)) {
                    viewModel.restaurarHabito(habito.id)
                }
                .show()
        }
    }

    // Construcción de días para la vista de lista
    private fun construirDiasParaLista(completadoHoy: Boolean): List<DayStatus> {
        val dias = mutableListOf<DayStatus>()
        val etiquetas = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val etiqueta = etiquetas[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val numeroDia = cal.get(Calendar.DAY_OF_MONTH)

            val estado =
                when {
                    i == 0 && completadoHoy -> DayState.COMPLETED
                    i == 0 -> DayState.TODAY
                    else -> DayState.NOT_APPLICABLE
                }

            dias.add(DayStatus(etiqueta, numeroDia, estado))
        }

        return dias
    }

    // Visibilidad de vistas

    private fun mostrarCarga(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.rvHabits.visibility = if (mostrar) View.GONE else View.VISIBLE
    }

    private fun mostrarEstadoVacio(mostrar: Boolean) {
        binding.tvEmptyState.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.rvHabits.visibility = if (mostrar) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
