package com.ochoastack.habitus.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.CategoryRepository
import com.ochoastack.habitus.data.HabitCategory
import com.ochoastack.habitus.data.HabitRepository
import com.ochoastack.habitus.databinding.ActivityEditHabitBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditHabitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditHabitBinding
    private lateinit var formManager: HabitFormManager

    // Configuramos la inyección para recibir el repositorio sin crearlo manualmente
    @Inject
    lateinit var habitRepository: HabitRepository
    private val categoryRepository = CategoryRepository()

    private var habitoId = ""
    private var categorias = listOf<HabitCategory>()
    private var categoriaSeleccionada: HabitCategory? = null
    private var diasGraciaSeleccionados = 0

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_HABIT_FREQUENCY = "habit_frequency"
        const val EXTRA_HABIT_CATEGORY_ID = "habit_category_id"
        const val EXTRA_HABIT_GRACE_DAYS = "habit_grace_days"
        const val EXTRA_HABIT_TIPO_COGNITIVO = "habit_tipo_cognitivo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHabitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        habitoId = intent.getStringExtra(EXTRA_HABIT_ID) ?: ""
        val nombreActual = intent.getStringExtra(EXTRA_HABIT_NAME) ?: ""
        val frecuenciaActual = intent.getStringExtra(EXTRA_HABIT_FREQUENCY) ?: ""
        val categoriaIdActual = intent.getStringExtra(EXTRA_HABIT_CATEGORY_ID) ?: ""
        diasGraciaSeleccionados = intent.getIntExtra(EXTRA_HABIT_GRACE_DAYS, 0)

        binding.edtHabitName.setText(nombreActual)

        formManager =
            HabitFormManager(
                context = this,
                chipData =
                    listOf(
                        binding.chipLun to "Lun",
                        binding.chipMar to "Mar",
                        binding.chipMie to "Mié",
                        binding.chipJue to "Jue",
                        binding.chipVie to "Vie",
                        binding.chipSab to "Sáb",
                        binding.chipDom to "Dom",
                    ),
                btnFreqDaily = binding.btnFreqDaily,
                btnFreqCustom = binding.btnFreqCustom,
                layoutDays = binding.layoutDays,
                tvDaysError = binding.tvDaysError,
            )

        val esDiarioActual = frecuenciaActual == getString(R.string.frequency_daily)
        formManager.inicializarFrecuencia(esDiarioActual)

        if (!esDiarioActual) {
            formManager.preseleccionarDias(frecuenciaActual.split(" - "))
        }

        val tipoInicial =
            intent.getStringExtra(EXTRA_HABIT_TIPO_COGNITIVO)
                ?: com.ochoastack.habitus.data.TipoCognitivo.FISICO
        formManager.configurarSelectorTipo(binding.chipGroupTipo, tipoInicial)

        seleccionarGracia(diasGraciaSeleccionados)
        cargarCategorias(categoriaIdActual)

        binding.btnGrace0.setOnClickListener { seleccionarGracia(0) }
        binding.btnGrace1.setOnClickListener { seleccionarGracia(1) }
        binding.btnGrace2.setOnClickListener { seleccionarGracia(2) }

        binding.btnSave.setOnClickListener {
            if (validarFormulario()) guardarCambios()
        }

        binding.edtHabitName.setOnFocusChangeListener { _, _ ->
            binding.tilHabitName.error = null
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun seleccionarGracia(dias: Int) {
        diasGraciaSeleccionados = dias
        listOf(binding.btnGrace0, binding.btnGrace1, binding.btnGrace2)
            .forEachIndexed { index, btn ->
                if (index == dias) {
                    aplicarEstiloGraciaActivo(btn)
                } else {
                    aplicarEstiloGraciaInactivo(btn)
                }
            }
    }

    private fun aplicarEstiloGraciaActivo(boton: MaterialButton) {
        boton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent))
        boton.strokeColor = ColorStateList.valueOf(getColor(R.color.accent))
        boton.setTextColor(getColor(R.color.on_accent))
    }

    private fun aplicarEstiloGraciaInactivo(boton: MaterialButton) {
        boton.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        boton.strokeColor = ColorStateList.valueOf(getColor(R.color.divider_color))
        boton.setTextColor(getColor(R.color.text_secondary))
    }

    private fun cargarCategorias(categoriaIdActual: String) {
        lifecycleScope.launch {
            val resultado = categoryRepository.obtenerCategorias()
            resultado.fold(
                onSuccess = { lista ->
                    categorias = lista
                    val adapter =
                        ArrayAdapter(
                            this@EditHabitActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            lista.map { it.nombre },
                        )
                    binding.actvCategory.setAdapter(adapter)

                    val categoriaActual = lista.find { it.id == categoriaIdActual }
                    if (categoriaActual != null) {
                        binding.actvCategory.setText(categoriaActual.nombre, false)
                        categoriaSeleccionada = categoriaActual
                    }

                    binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
                        categoriaSeleccionada = lista[position]
                    }
                },
                onFailure = { },
            )
        }
    }

    private fun validarFormulario(): Boolean {
        val nombre = binding.edtHabitName.text.toString().trim()
        var esValido = true

        if (nombre.isEmpty()) {
            binding.tilHabitName.error = getString(R.string.error_habit_name_empty)
            esValido = false
        } else {
            binding.tilHabitName.error = null
        }

        if (!formManager.esDiario && !formManager.hayDiasSeleccionados()) {
            formManager.mostrarErrorDias()
            esValido = false
        }

        return esValido
    }

    private fun guardarCambios() {
        val nombre = binding.edtHabitName.text.toString().trim()

        binding.btnSave.isEnabled = false
        binding.btnSave.text = getString(R.string.btn_update_loading)

        lifecycleScope.launch {
            val resultado =
                habitRepository.actualizarHabito(
                    habitoId = habitoId,
                    nombre = nombre,
                    frecuencia = formManager.obtenerFrecuenciaString(),
                    diasSemana = formManager.obtenerDiasLista(),
                    categoriaId = categoriaSeleccionada?.id ?: "",
                    diasGracia = diasGraciaSeleccionados,
                    tipoCognitivo = formManager.obtenerTipoSeleccionado(),
                )
            resultado.fold(
                onSuccess = { finish() },
                onFailure = {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.btn_update_habit)
                },
            )
        }
    }
}
