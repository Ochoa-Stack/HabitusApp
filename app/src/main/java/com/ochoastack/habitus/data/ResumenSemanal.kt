package com.ochoastack.habitus.data

// Declaramos el modelo de datos para el resumen semanal
data class ResumenSemanal(
    val porcentajeSemana: Int,
    val totalHabitos: Int,
    val completacionesSemana: Int,
    val totalProgramadasSemana: Int,
    val habitoMejorRacha: String,
    val rachaMaxima: Int,
    val habitoMasDescuidado: String?,
)
