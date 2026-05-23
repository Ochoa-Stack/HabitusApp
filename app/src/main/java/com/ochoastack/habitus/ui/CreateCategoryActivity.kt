package com.ochoastack.habitus.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.CategoryRepository
import com.ochoastack.habitus.databinding.ActivityCreateCategoryBinding
import kotlinx.coroutines.launch

class CreateCategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateCategoryBinding

    // Declaramos el repositorio de categorías
    private val categoryRepository = CategoryRepository()

    // Declaramos el color seleccionado por defecto
    private var colorSeleccionado = "#C8614A"

    // Declaramos el mapa de vistas de color a su valor hex
    private lateinit var mapaColores: Map<View, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Inicializamos el mapa de colores
        mapaColores =
            mapOf(
                binding.colorTerracota to "#C8614A",
                binding.colorMorado to "#673AB7",
                binding.colorVerde to "#7DAF8F",
                binding.colorAzul to "#2196F3",
                binding.colorRosa to "#E91E8C",
                binding.colorNaranja to "#FF9800",
            )
        // Marcamos el primer color como seleccionado por defecto
        actualizarSeleccionColor(binding.colorTerracota)
        // Asignamos el listener a cada color
        mapaColores.forEach { (vista, color) ->
            vista.setOnClickListener {
                colorSeleccionado = color
                actualizarSeleccionColor(vista)
            }
        }
        // Asignamos el evento al botón guardar
        binding.btnSave.setOnClickListener {
            if (validarFormulario()) {
                guardarCategoria()
            }
        }
        // Limpiamos error mientras el usuario escribe
        binding.edtName.setOnFocusChangeListener { _, _ ->
            binding.tilName.error = null
        }
        binding.btnBack.setOnClickListener { finish() }
    }

    // Actualizamos el borde de selección del color activo
    private fun actualizarSeleccionColor(vistaSeleccionada: View) {
        mapaColores.forEach { (vista, _) ->
            vista.scaleX = if (vista == vistaSeleccionada) 1.2f else 1.0f
            vista.scaleY = if (vista == vistaSeleccionada) 1.2f else 1.0f
        }
    }

    // Validamos que el nombre no esté vacío
    private fun validarFormulario(): Boolean {
        val nombre = binding.edtName.text.toString().trim()
        return if (nombre.isEmpty()) {
            binding.tilName.error = getString(R.string.error_category_name_empty)
            false
        } else {
            binding.tilName.error = null
            true
        }
    }

    // Guardamos la categoría en Firestore
    private fun guardarCategoria() {
        val nombre = binding.edtName.text.toString().trim()

        binding.btnSave.isEnabled = false
        binding.btnSave.text = getString(R.string.btn_save_loading)

        lifecycleScope.launch {
            val resultado = categoryRepository.crearCategoria(nombre, colorSeleccionado)

            resultado.fold(
                onSuccess = { finish() },
                onFailure = {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.btn_save_category)
                },
            )
        }
    }
}
