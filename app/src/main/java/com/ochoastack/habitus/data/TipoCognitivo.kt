package com.ochoastack.habitus.data

import com.ochoastack.habitus.R

object TipoCognitivo {
    const val FISICO = "Físico"
    const val MENTAL = "Mental"
    const val SOCIAL = "Social"
    const val CREATIVO = "Creativo"
    const val DESCANSO = "Descanso"

    val todos = listOf(FISICO, MENTAL, SOCIAL, CREATIVO, DESCANSO)

    // Colores representativos para cada tipo (hex strings)
    fun color(tipo: String): String =
        when (tipo) {
            FISICO -> "#E07B5A" // terracota cálido
            MENTAL -> "#7B9FD4" // azul serenidad
            SOCIAL -> "#F0B86E" // ámbar social
            CREATIVO -> "#A78BCA" // violeta creativo
            DESCANSO -> "#78B89A" // verde salvia
            else -> "#C8614A" // color por defecto
        }

    /* Retorna el resource ID del color asociado al tipo.
    - Preferir este método sobre [color] en código de UI para garantizar compatibilidad con modo oscuro.
    - @param tipo Una de las constantes de este objeto.
    - @return Resource ID de [R.color.tipo_fisico] y equivalentes. */

    fun colorRes(tipo: String): Int =
        when (tipo) {
            FISICO -> R.color.tipo_fisico
            MENTAL -> R.color.tipo_mental
            SOCIAL -> R.color.tipo_social
            CREATIVO -> R.color.tipo_creativo
            DESCANSO -> R.color.tipo_descanso
            else -> R.color.accent
        }

    // Emoji para cada tipo
    fun emoji(tipo: String): String =
        when (tipo) {
            FISICO -> " "
            MENTAL -> " "
            SOCIAL -> " "
            CREATIVO -> " "
            DESCANSO -> " "
            else -> " "
        }
}
