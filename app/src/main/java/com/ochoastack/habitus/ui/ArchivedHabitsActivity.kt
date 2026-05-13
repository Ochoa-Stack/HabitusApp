package com.ochoastack.habitus.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.Habit
import com.ochoastack.habitus.data.HabitRepository
import com.ochoastack.habitus.databinding.ActivityArchivedHabitsBinding
import com.ochoastack.habitus.databinding.ItemArchivedHabitBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ArchivedHabitsActivity : AppCompatActivity() {
    // Declaramos las variables necesarias
    private lateinit var binding: ActivityArchivedHabitsBinding
    private val habitRepository = HabitRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchivedHabitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvArchivados.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }

        cargarArchivados()
    }
    // Cargamos los hábitos archivados
    private fun cargarArchivados() {
        lifecycleScope.launch {
            val resultado = habitRepository.obtenerHabitosArchivados()
            resultado.fold(
                onSuccess = { habitos ->
                    if (habitos.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvArchivados.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvArchivados.visibility = View.VISIBLE
                        binding.rvArchivados.adapter    = ArchivedHabitAdapter(
                            habitos,
                            onRestore = { habito -> restaurar(habito) },
                            onDelete  = { habito -> eliminar(habito)  }
                        )
                    }
                },
                onFailure = {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvArchivados.visibility = View.GONE
                }
            )
        }
    }
    // Restauramos un hábito archivado
    private fun restaurar(habito: Habit) {
        lifecycleScope.launch {
            val resultado = habitRepository.restaurarHabito(habito.id)
            resultado.fold(
                onSuccess = {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.habit_restored),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    cargarArchivados()
                },
                onFailure = {
                    android.widget.Toast.makeText(
                        this@ArchivedHabitsActivity,
                        getString(R.string.error_cargar_datos),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
    // Eliminamos un hábito archivado
    private fun eliminar(habito: Habit) {
        lifecycleScope.launch {
            val resultado = habitRepository.eliminarHabito(habito.id)
            resultado.fold(
                onSuccess = {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.habit_deleted),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    cargarArchivados()
                },
                onFailure = {
                    android.widget.Toast.makeText(
                        this@ArchivedHabitsActivity,
                        getString(R.string.error_cargar_datos),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}
// Creamos un 'Adaptador' para los hábitos archivados
class ArchivedHabitAdapter(
    private val habitos: List<Habit>,
    private val onRestore: (Habit) -> Unit,
    private val onDelete:  (Habit) -> Unit
) : RecyclerView.Adapter<ArchivedHabitAdapter.VH>() {

    inner class VH(val binding: ItemArchivedHabitBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemArchivedHabitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))

    override fun getItemCount() = habitos.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val habito = habitos[position]
        with(holder.binding) {
            tvNombreArchivado.text    = habito.nombre
            tvFrecuenciaArchivado.text = habito.frecuencia
            tvRachaArchivado.text     = "🔗 ${habito.racha}"
            btnRestaurar.setOnClickListener { onRestore(habito) }
            btnEliminar.setOnClickListener  { onDelete(habito)  }
        }
    }
}
