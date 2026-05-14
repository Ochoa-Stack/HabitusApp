package com.ochoastack.habitus.data

data class ResumenSemanal(    // Declaramos el modelo de datos para el resumen semanal
    val porcentajeSemana: Int,
    val totalHabitos: Int,
    val completacionesSemana: Int,
    val totalProgramadasSemana: Int,
    val habitoMejorRacha: String,
    val rachaMaxima: Int,
    val habitoMasDescuidado: String?
)
