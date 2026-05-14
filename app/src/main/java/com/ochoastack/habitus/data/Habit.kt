package com.ochoastack.habitus.data

// Declaramos el modelo de datos para los hábitos

/* Modelo de dominio que representa un hábito del usuario.
    - @property weekDays, [iconRes] y [estaCompletadoHoy] son campos de UI calculados en
    [HabitRepository.obtenerHabitos]; no se persisten en Firestore.
    - @property diasGracia Días programados fallados consecutivos permitidos antes de resetear la
racha. Rango: 0–2.
    - @property tipoCognitivo Clasificación de la actividad. Usar constantes de [TipoCognitivo].
    - @property archivado Si true, excluido de la lista activa. Los datos históricos se conservan en
Firestore.
    - @property estaCompletadoHoy Campo de UI. Poblado por el repositorio consultando
completaciones/{fecha} en carga. */

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
        val estaCompletadoHoy: Boolean = false
)

data class DayStatus( // Declaramos el modelo de datos para el estado de los días
val label: String, val dayNumber: Int, val status: DayState)

enum class DayState { // Declaramos el modelo de datos para los estados de los días
    COMPLETED,
    TODAY,
    MISSED,
    NOT_APPLICABLE
}
