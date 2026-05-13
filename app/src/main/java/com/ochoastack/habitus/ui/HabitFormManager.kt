package com.ochoastack.habitus.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ochoastack.habitus.R
import com.google.android.material.button.MaterialButton

// Centraliza toda la lógica del formulario de hábitos para evitar duplicación entre 'CreateHabitActivity' y 'EditHabitActivity'
class HabitFormManager(
    private val context: Context,
    chipData: List<Pair<TextView, String>>,
    private val btnFreqDaily: MaterialButton,
    private val btnFreqCustom: MaterialButton,
    private val layoutDays: ViewGroup,
    private val tvDaysError: TextView
) {
    var esDiario: Boolean = true    // Indica si el hábito es diario o personalizado
        private set
    // Mapeamos los chips a su estado actual
    private val estadoChips: MutableMap<TextView, Boolean> = mutableMapOf()
    private val etiquetaChips: Map<TextView, String>
    
    private var tipoSeleccionado: String = com.ochoastack.habitus.data.TipoCognitivo.FISICO

    fun configurarSelectorTipo(
        chipGroup: com.google.android.material.chip.ChipGroup,
        tipoInicial: String = com.ochoastack.habitus.data.TipoCognitivo.FISICO
    ) {
        tipoSeleccionado = tipoInicial
        // Seleccionar el chip correspondiente al tipo inicial
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? com.google.android.material.chip.Chip
            if (chip?.text.toString() == "${com.ochoastack.habitus.data.TipoCognitivo.emoji(tipoInicial)} $tipoInicial") {
                chip?.isChecked = true
            }
        }
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = group.findViewById<com.google.android.material.chip.Chip>(
                checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            )
            // Extrae solo el texto sin emoji: "🏃 Físico" -> "Físico"
            tipoSeleccionado = chip?.text?.toString()?.substringAfter(" ") ?: com.ochoastack.habitus.data.TipoCognitivo.FISICO
        }
    }

    fun obtenerTipoSeleccionado(): String = tipoSeleccionado


    init {    // Inicializamos el estado de los chips
        etiquetaChips = chipData.associate { (chip, etiqueta) -> chip to etiqueta }

        chipData.forEach { (chip, _) ->
            estadoChips[chip] = false
            chip.setOnClickListener { toggleChip(chip) }
        }
        // Configuramos los botones de frecuencia
        btnFreqDaily.setOnClickListener  { seleccionarFrecuencia(true)  }
        btnFreqCustom.setOnClickListener { seleccionarFrecuencia(false) }
    }
    // Establecemos el modo de frecuencia inicial y actualiza el estado visual
    fun inicializarFrecuencia(diario: Boolean) {
        seleccionarFrecuencia(diario)
    }
    // Marcamos como activos los chips cuyos labels coincidan con la lista recibida
    fun preseleccionarDias(diasActivos: List<String>) {
        etiquetaChips.forEach { (chip, etiqueta) ->
            if (diasActivos.contains(etiqueta) && estadoChips[chip] == false) {
                toggleChip(chip)
            }
        }
    }
    // Comprobamos si hay al menos un chip seleccionado
    fun hayDiasSeleccionados(): Boolean = estadoChips.values.any { it }
    // Mostramos el mensaje de error de días
    fun mostrarErrorDias() {
        tvDaysError.visibility = View.VISIBLE
    }
    // Construimos el string de frecuencia que se persiste en Firestore
    fun obtenerFrecuenciaString(): String {
        return if (esDiario) {
            context.getString(R.string.frequency_daily)
        } else {
            estadoChips
                .filter { it.value }
                .keys
                .mapNotNull { etiquetaChips[it] }
                .joinToString(" - ")
        }
    }
    // Construimos la lista de días seleccionados que se persiste en Firestore
    fun obtenerDiasLista(): List<String> {
        return if (esDiario) {
            listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        } else {
            estadoChips
                .filter { it.value }
                .keys
                .mapNotNull { etiquetaChips[it] }
        }
    }
    // Configuramos el modo de frecuencia
    private fun seleccionarFrecuencia(diario: Boolean) {
        esDiario = diario
        if (diario) {
            aplicarEstiloActivo(btnFreqDaily)
            aplicarEstiloInactivo(btnFreqCustom)
            layoutDays.visibility = View.GONE
        } else {
            aplicarEstiloActivo(btnFreqCustom)
            aplicarEstiloInactivo(btnFreqDaily)
            layoutDays.visibility = View.VISIBLE
        }
    }
    // Alternamos el estado de un chip
    private fun toggleChip(chip: TextView) {
        val seleccionado = estadoChips[chip] ?: false
        estadoChips[chip] = !seleccionado

        if (!seleccionado) {
            chip.background = context.getDrawable(R.drawable.bg_chip_day_selected)
            chip.setTextColor(context.getColor(R.color.on_accent))
        } else {
            chip.background = context.getDrawable(R.drawable.bg_chip_day_unselected)
            chip.setTextColor(context.getColor(R.color.text_secondary))
        }
        tvDaysError.visibility = View.GONE
    }
    // Aplicamos el estilo a un botón
    private fun aplicarEstiloActivo(boton: MaterialButton) {
        boton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(context.getColor(R.color.accent))
        boton.strokeColor =
            android.content.res.ColorStateList.valueOf(context.getColor(R.color.accent))
        boton.setTextColor(context.getColor(R.color.on_accent))
    }
    // Aplicamos el estilo a un botón
    private fun aplicarEstiloInactivo(boton: MaterialButton) {
        boton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        boton.strokeColor =
            android.content.res.ColorStateList.valueOf(context.getColor(R.color.divider_color))
        boton.setTextColor(context.getColor(R.color.text_secondary))
    }
}
