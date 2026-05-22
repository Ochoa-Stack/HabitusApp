package com.ochoastack.habitus.data

// Creamos 'EstadisticasUsuario' como modelo de datos para las estadísticas del usuario
data class EstadisticasUsuario(
    val totalHabitos: Int,
    val completadosHoy: Int,
    val rachaMaxima: Int,
    val totalCompletaciones: Int
)
