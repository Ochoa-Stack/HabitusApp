package com.example.habittrackerapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.habittrackerapp.R
import com.example.habittrackerapp.data.DayState
import com.example.habittrackerapp.data.DayStatus
import com.example.habittrackerapp.data.Habit
import com.example.habittrackerapp.data.HabitRepository
import com.example.habittrackerapp.databinding.FragmentHabitsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar

class HabitsFragment : Fragment() {    // Declaramos el fragmento de hábitos
    private var _binding: FragmentHabitsBinding? = null    // Declaramos el binding
    private val binding get() = _binding!!    // Declaramos el binding get()
    // Declaramos el repositorio de hábitos
    private val habitRepository = HabitRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }
    // Configuramos la UI con los hábitos
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.fab.setOnClickListener {
            startActivity(Intent(requireContext(), CreateHabitActivity::class.java))
        }
        binding.tvVerArchivados.setOnClickListener {
            startActivity(Intent(requireContext(), ArchivedHabitsActivity::class.java))
        }
        cargarHabitos()
    }
    override fun onResume() {    // Actualizamos la UI con los hábitos
        super.onResume()
        cargarHabitos()
    }
    private fun cargarHabitos() {    // Cargamos los hábitos
        mostrarCarga(true)

        viewLifecycleOwner.lifecycleScope.launch {
            if (_binding == null) return@launch
            val resultado = habitRepository.obtenerHabitos()    // Obtenemos los hábitos

            resultado.fold(
                onSuccess = { habitos ->
                    mostrarCarga(false)

                    if (habitos.isEmpty()) {
                        mostrarEstadoVacio(true)
                    } else {
                        mostrarEstadoVacio(false)
                        /* Construimos la representación visual de la semana para cada hábito
                        en la vista de lista mostramos el estado -> hoy completado o no,
                        y 'NOT_APPLICABLE' para días pasados (el historial detallado está en el) */
                        val habitsConDias = habitos.map { habit ->
                            habit.copy(weekDays = construirDiasParaLista(habit.estaCompletadoHoy))
                        }

                        val adapter = HabitAdapter { habit ->
                            val intent = Intent(requireContext(), HabitDetailActivity::class.java).apply {
                                putExtra(HabitDetailActivity.EXTRA_HABIT_ID,          habit.id)
                                putExtra(HabitDetailActivity.EXTRA_HABIT_NAME,        habit.nombre)
                                putExtra(HabitDetailActivity.EXTRA_HABIT_FREQUENCY,   habit.frecuencia)
                                putExtra(HabitDetailActivity.EXTRA_HABIT_STREAK,      habit.racha)
                                putExtra(HabitDetailActivity.EXTRA_HABIT_PERCENT,     habit.porcentaje)
                                putExtra(HabitDetailActivity.EXTRA_HABIT_CATEGORY_ID, habit.categoriaId)
                            }
                            startActivity(intent)
                        }
                        // Configuramos el swipe para archivar
                        binding.rvHabits.adapter = adapter
                        adapter.submitList(habitsConDias)
                        configurarSwipe(adapter)
                    }
                },
                onFailure = {
                    mostrarCarga(false)
                    mostrarEstadoVacio(true)
                }
            )
        }
    }
    /* Construye los 7 días de la semana para la vista de lista, el estado de hoy
     se determina con el campo calculado del repositorio. Los días pasados se
     muestran como 'NOT_APPLICABLE' -> en la lista no tenemos el historial completo;
     ese detalle pertenece a la pantalla de detalle del hábito */
    private fun construirDiasParaLista(completadoHoy: Boolean): List<DayStatus> {
        val dias      = mutableListOf<DayStatus>()
        val etiquetas = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

        for (i in 6 downTo 0) {
            val cal       = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val etiqueta  = etiquetas[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val numeroDia = cal.get(Calendar.DAY_OF_MONTH)

            val estado = when {
                i == 0 && completadoHoy -> DayState.COMPLETED
                i == 0                  -> DayState.TODAY
                else                    -> DayState.NOT_APPLICABLE
            }

            dias.add(DayStatus(etiqueta, numeroDia, estado))
        }

        return dias
    }
    // Configuramos el swipe para archivar (en lugar de eliminar permanentemente)
    private fun configurarSwipe(adapter: HabitAdapter) {
        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ) = false

                override fun onSwiped(
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    val posicion = viewHolder.adapterPosition
                    val habito   = adapter.currentList[posicion]

                    viewLifecycleOwner.lifecycleScope.launch {
                        if (_binding == null) return@launch
                        val resultado = habitRepository.archivarHabito(habito.id)
                        resultado.fold(
                            onSuccess = {
                                cargarHabitos()
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.habit_archived),
                                    Snackbar.LENGTH_LONG
                                ).setAction(getString(R.string.habit_undo)) {
                                    // Deshacer: restaurar el hábito inmediatamente
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        if (_binding == null) return@launch
                                        habitRepository.restaurarHabito(habito.id)
                                        cargarHabitos()
                                    }
                                }.show()
                            },
                            onFailure = {
                                adapter.notifyItemChanged(posicion)
                            }
                        )
                    }
                }
            }
        )
        itemTouchHelper.attachToRecyclerView(binding.rvHabits)
    }
    // Mostramos la carga
    private fun mostrarCarga(mostrar: Boolean) {
        binding.progressBar.visibility =
            if (mostrar) View.VISIBLE else View.GONE
        binding.rvHabits.visibility =
            if (mostrar) View.GONE else View.VISIBLE
    }
    // Mostramos el estado vacío
    private fun mostrarEstadoVacio(mostrar: Boolean) {
        binding.tvEmptyState.visibility =
            if (mostrar) View.VISIBLE else View.GONE
        binding.rvHabits.visibility =
            if (mostrar) View.GONE else View.VISIBLE
    }
    // Limpiamos el binding
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
