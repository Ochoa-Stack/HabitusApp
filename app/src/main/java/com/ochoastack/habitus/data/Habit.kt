package com.ochoastack.habitus.data

// Declaramos el modelo de datos para los hábitos
data class Habit(
    val id: String = "",
    val nombre: String = "",
    val frecuencia: String = "",
    val diasSemana: List<String> = emptyList(),
    val racha: Int = 0,
    val porcentaje: Int = 0,
    val totalCompletaciones: Int = 0,
    val diasGracia: Int = 0,
    val archivado: Boolean = false,
    val tipoCognitivo: String = "Físico",
    val uid: String = "",
    val categoriaId: String = "",
    val categoriaNombre: String = "",
    val categoriaColor: String = "",
    // Campos calculados en cliente, no persisten en Firestore
    val weekDays: List<DayStatus> = emptyList(),
    val iconRes: Int = 0,
    val estaCompletadoHoy: Boolean = false,
)

// Declaramos el modelo de datos para el estado de los días
data class DayStatus(
    val label: String,
    val dayNumber: Int,
    val status: DayState,
)

enum class DayState {
    // Declaramos el modelo de datos para los estados de los días
    COMPLETED,
    TODAY,
    MISSED,
    NOT_APPLICABLE,
}
