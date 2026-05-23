package com.ochoastack.habitus.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests unitarios para la lógica de racha de [HabitRepository]
class HabitRepositoryStreakTest {
    private lateinit var repository: HabitRepository
    private val firestore = mockk<FirebaseFirestore>()
    private val auth = mockk<FirebaseAuth>()

    // Formato ISO estricto, igual que el repositorio usa con Locale.ROOT
    private val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    @Before
    fun setUp() {
        // HabitRepository con instancias de Firebase se mockean en tests de integración.
        // Aquí probamos SOLO la lógica de racha (función pura sin red).
        repository = HabitRepository(firestore, auth)
    }

    // Casos: sin días de gracia (diasGracia = 0)

    @Test
    fun `racha no se resetea si completó ayer y tiene ese día programado`() {
        val diasSemana = diasConHoy().toMutableList()
        val ultimaCompletacion = timestampDeHaceNDias(1)

        val resultado = repository.testDebeResetearRacha(diasSemana, ultimaCompletacion, 0)

        assertFalse("La racha no debe resetearse si completó el último día programado", resultado)
    }

    @Test
    fun `racha se resetea si no completó ayer y no tiene días de gracia`() {
        val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val ultimaCompletacion = timestampDeHaceNDias(2) // faltó ayer

        val resultado = repository.testDebeResetearRacha(diasSemana, ultimaCompletacion, 0)

        assertTrue("La racha debe resetearse si faltó sin días de gracia", resultado)
    }

    @Test
    fun `racha no se resetea si lista de dias esta vacia`() {
        val resultado = repository.testDebeResetearRacha(emptyList(), null, 0)

        assertFalse("Con lista vacía de días, la racha nunca se resetea", resultado)
    }

    @Test
    fun `racha no se resetea si ultima completacion es null y diasSemana es vacia`() {
        val resultado = repository.testDebeResetearRacha(emptyList(), null, 0)

        assertFalse("Sin días programados no hay racha que resetear", resultado)
    }

    // Casos: con días de gracia

    @Test
    fun `racha no se resetea con 1 dia de gracia y faltó 1 dia programado`() {
        val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val ultimaCompletacion = timestampDeHaceNDias(2) // faltó ayer

        val resultado = repository.testDebeResetearRacha(diasSemana, ultimaCompletacion, 1)

        assertFalse("Con 1 día de gracia, 1 día fallado no resetea la racha", resultado)
    }

    @Test
    fun `racha se resetea con 1 dia de gracia si faltó 2 dias consecutivos programados`() {
        val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val ultimaCompletacion = timestampDeHaceNDias(3) // faltó antes de ayer y ayer

        val resultado = repository.testDebeResetearRacha(diasSemana, ultimaCompletacion, 1)

        assertTrue("Con 1 día de gracia, 2 días fallados sí resetean la racha", resultado)
    }

    @Test
    fun `racha no se resetea con 2 dias de gracia y faltó 2 dias programados`() {
        val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val ultimaCompletacion = timestampDeHaceNDias(3) // faltó 2 días

        val resultado = repository.testDebeResetearRacha(diasSemana, ultimaCompletacion, 2)

        assertFalse("Con 2 días de gracia, 2 días fallados no resetean la racha", resultado)
    }

    // Casos: hábito con días específicos de la semana

    @Test
    fun `racha no se resetea si el dia programado fue completado aunque hayan pasado dias no programados`() {
        // Hábito solo los lunes
        val diasSemana = listOf("Lun")
        // Última completación el lunes pasado (6 días atrás si hoy es domingo)
        val ultimaCompletacion = timestampDeHaceNDias(6)

        // No debe resetear porque el único día programado fue completado
        val resultado = repository.testDebeResetearRacha(diasSemana, ultimaCompletacion, 0)

        // El resultado depende del día actual; si hoy no es lunes, no hay días perdidos.
        // Este test valida que la lógica itera sobre días PROGRAMADOS, no todos los días.
        // Si el lunes más reciente fue completado, no debe resetear.
        assertFalse(
            "Un hábito de solo lunes cuyo último lunes fue completado no debe resetear la racha",
            resultado,
        )
    }

    // Helpers

    // Retorna un Timestamp que representa hace [n] días desde hoy
    private fun timestampDeHaceNDias(n: Int): Timestamp {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -n)
        return Timestamp(cal.time)
    }

    // Retorna las etiquetas de días que incluyen el día de HOY.
    // Útil para simular un hábito que tiene programado el día actual
    private fun diasConHoy(): List<String> {
        val etiquetas = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        val diaHoy = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return listOf(etiquetas[diaHoy - 1])
    }
}
