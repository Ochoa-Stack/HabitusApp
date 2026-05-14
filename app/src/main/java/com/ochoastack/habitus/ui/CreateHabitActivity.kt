package com.ochoastack.habitus.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.ochoastack.habitus.data.CategoryRepository
import com.ochoastack.habitus.data.HabitCategory
import com.ochoastack.habitus.data.HabitRepository
import kotlinx.coroutines.launch
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ochoastack.habitus.R
import com.ochoastack.habitus.databinding.ActivityCreateHabitBinding
import com.google.android.material.button.MaterialButton

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateHabitBinding
    private lateinit var formManager: HabitFormManager

    private val habitRepository    = HabitRepository()
    private val categoryRepository = CategoryRepository()
    private var categorias         = listOf<HabitCategory>()
    private var categoriaSeleccionada: HabitCategory? = null
    private var diasGraciaSeleccionados = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateHabitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        formManager = HabitFormManager(
            context       = this,
            chipData      = listOf(
                binding.chipLun to "Lun",
                binding.chipMar to "Mar",
                binding.chipMie to "Mié",
                binding.chipJue to "Jue",
                binding.chipVie to "Vie",
                binding.chipSab to "Sáb",
                binding.chipDom to "Dom"
            ),
            btnFreqDaily  = binding.btnFreqDaily,
            btnFreqCustom = binding.btnFreqCustom,
            layoutDays    = binding.layoutDays,
            tvDaysError   = binding.tvDaysError
        )

        formManager.inicializarFrecuencia(diario = true)
        formManager.configurarSelectorTipo(binding.chipGroupTipo)
        seleccionarGracia(0)
        cargarCategorias()

        binding.btnGrace0.setOnClickListener { seleccionarGracia(0) }
        binding.btnGrace1.setOnClickListener { seleccionarGracia(1) }
        binding.btnGrace2.setOnClickListener { seleccionarGracia(2) }

        binding.btnSave.setOnClickListener {
            if (validarFormulario()) guardarHabito()
        }

        binding.edtHabitName.setOnFocusChangeListener { _, _ ->
            binding.tilHabitName.error = null
        }
    }

    private fun seleccionarGracia(dias: Int) {
        diasGraciaSeleccionados = dias
        listOf(binding.btnGrace0, binding.btnGrace1, binding.btnGrace2)
            .forEachIndexed { index, btn ->
                if (index == dias) aplicarEstiloGraciaActivo(btn)
                else               aplicarEstiloGraciaInactivo(btn)
            }
    }

    private fun aplicarEstiloGraciaActivo(boton: MaterialButton) {
        boton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent))
        boton.strokeColor        = ColorStateList.valueOf(getColor(R.color.accent))
        boton.setTextColor(getColor(R.color.on_accent))
    }

    private fun aplicarEstiloGraciaInactivo(boton: MaterialButton) {
        boton.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        boton.strokeColor        = ColorStateList.valueOf(getColor(R.color.divider_color))
        boton.setTextColor(getColor(R.color.text_secondary))
    }

    private fun cargarCategorias() {
        lifecycleScope.launch {
            val resultado = categoryRepository.obtenerCategorias()
            resultado.fold(
                onSuccess = { lista ->
                    categorias = lista
                    val adapter = ArrayAdapter(
                        this@CreateHabitActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        lista.map { it.nombre }
                    )
                    binding.actvCategory.setAdapter(adapter)
                    binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
                        categoriaSeleccionada = lista[position]
                    }
                },
                onFailure = { }
            )
        }
    }

    private fun validarFormulario(): Boolean {
        val nombre   = binding.edtHabitName.text.toString().trim()
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

    private fun guardarHabito() {
        val nombre = binding.edtHabitName.text.toString().trim()

        binding.btnSave.isEnabled = false
        binding.btnSave.text      = getString(R.string.btn_save_loading)

        lifecycleScope.launch {
            val resultado = habitRepository.guardarHabito(
                nombre      = nombre,
                frecuencia  = formManager.obtenerFrecuenciaString(),
                diasSemana  = formManager.obtenerDiasLista(),
                categoriaId = categoriaSeleccionada?.id ?: "",
                diasGracia  = diasGraciaSeleccionados,
                tipoCognitivo = formManager.obtenerTipoSeleccionado()
            )
            resultado.fold(
                onSuccess = { finish() },
                onFailure = {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text      = getString(R.string.btn_save_habit)
                }
            )
        }
    }
}
