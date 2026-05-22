package com.ochoastack.habitus.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class HabitRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun coleccionHabitos() = firestore.collection("habitos")

    private fun obtenerUid(): String =
            auth.currentUser?.uid ?: throw Exception("No hay sesión activa")
    // Obtenemos la fecha actual en formato yyyy-MM-dd
    // IMPORTANTE: Locale.ROOT garantiza formato ISO consistente independiente del locale del dispositivo.
    // Las fechas se usan como IDs de documentos en Firestore — Locale.getDefault() causaría
    // inconsistencias silenciosas en dispositivos con configuraciones regionales no estándar.
    private fun obtenerFechaHoy(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    // Comprueba si la fecha de completación es hoy
    private fun esHoy(fechaTimestamp: com.google.firebase.Timestamp?): Boolean {
        if (fechaTimestamp == null) return false
        val hoy = Calendar.getInstance()
        val fechaDoc = Calendar.getInstance().apply { time = fechaTimestamp.toDate() }
        return hoy.get(Calendar.DAY_OF_YEAR) == fechaDoc.get(Calendar.DAY_OF_YEAR) &&
                hoy.get(Calendar.YEAR) == fechaDoc.get(Calendar.YEAR)
    }
    // Declaramos el mapa de constante Calendar -> etiqueta de día usada en diasSemana
    private val mapaCalendario =
            mapOf(
                    Calendar.MONDAY to "Lun",
                    Calendar.TUESDAY to "Mar",
                    Calendar.WEDNESDAY to "Mié",
                    Calendar.THURSDAY to "Jue",
                    Calendar.FRIDAY to "Vie",
                    Calendar.SATURDAY to "Sáb",
                    Calendar.SUNDAY to "Dom"
            )

    // `internal` permite tests unitarios sin reflexión
    internal fun debeResetearRacha(
            diasSemana: List<String>,
            ultimaCompletacion: com.google.firebase.Timestamp?,
            diasGracia: Int = 0
    ): Boolean {
        if (diasSemana.isEmpty()) return false

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val fechaUltimaStr =
                if (ultimaCompletacion != null) formato.format(ultimaCompletacion.toDate()) else ""

        var diasMissed = 0
        // Iteramos hacia atrás hasta 14 días contando días programados consecutivos sin completar
        for (i in 1..14) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val etiqueta = mapaCalendario[cal.get(Calendar.DAY_OF_WEEK)] ?: continue

            if (!diasSemana.contains(etiqueta)) continue

            val fechaDia = formato.format(cal.time)
            // Si no tiene completación, la racha se rompió
            if (fechaDia <= fechaUltimaStr) break

            diasMissed++

            if (diasMissed > diasGracia) return true
        }

        return false
    }

    /* Alias de test para [debeResetearRacha]. Solo para uso en tests unitarios */
    internal fun testDebeResetearRacha(
        diasSemana: List<String>,
        ultimaCompletacion: com.google.firebase.Timestamp?,
        diasGracia: Int = 0,
    ) = debeResetearRacha(diasSemana, ultimaCompletacion, diasGracia)
    // Recalculamos el porcentaje de cumplimiento real sobre los últimos 30 días
    private suspend fun calcularPorcentaje30Dias(habitoId: String, diasSemana: List<String>): Int {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

        val fechasEsperadas = mutableListOf<String>()
        for (i in 0..29) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val etiqueta = mapaCalendario[cal.get(Calendar.DAY_OF_WEEK)]
            if (diasSemana.contains(etiqueta)) {
                fechasEsperadas.add(formato.format(cal.time))
            }
        }

        if (fechasEsperadas.isEmpty()) return 0

        val completaciones =
                coleccionHabitos().document(habitoId).collection("completaciones").get().await()

        val fechasCompletadas = completaciones.documents.map { it.id }.toSet()
        val cumplidas = fechasEsperadas.count { fechasCompletadas.contains(it) }

        return ((cumplidas.toFloat() / fechasEsperadas.size.toFloat()) * 100).toInt()
    }
    // Guardamos un hábito en Firestore
    suspend fun guardarHabito(
            nombre: String,
            frecuencia: String,
            diasSemana: List<String>,
            categoriaId: String = "",
            diasGracia: Int = 0,
            tipoCognitivo: String = TipoCognitivo.FISICO
    ): Result<String> {
        return try {
            val uid = obtenerUid()
            val dato =
                    mapOf(
                            "uid" to uid,
                            "nombre" to nombre,
                            "frecuencia" to frecuencia,
                            "diasSemana" to diasSemana,
                            "categoriaId" to categoriaId,
                            "diasGracia" to diasGracia,
                            "tipoCognitivo" to tipoCognitivo,
                            "racha" to 0,
                            "porcentaje" to 0,
                            "totalCompletaciones" to 0,
                            "archivado" to false,
                            "fechaCreacion" to com.google.firebase.Timestamp.now()
                    )
            val referencia = coleccionHabitos().add(dato).await()
            Result.success(referencia.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun obtenerHabitos(): Result<List<Habit>> {
        return try {
            val uid = obtenerUid()
            val snapshot =
                    coleccionHabitos()
                            .whereEqualTo("uid", uid)
                            .whereEqualTo("archivado", false)
                            .get()
                            .await()

            val snapshotCategorias =
                    firestore.collection("categorias").whereEqualTo("uid", uid).get().await()

            val mapaCategorias =
                    snapshotCategorias.documents.associate { doc ->
                        doc.id to
                                Pair(
                                        doc.getString("nombre") ?: "",
                                        doc.getString("color") ?: "#C8614A"
                                )
                    }
            // Detectamos hábitos con racha rota y los reseteamos en lote
            val batch = firestore.batch()
            var hayResets = false

            snapshot.documents.forEach { doc ->
                val rachaActual = (doc.getLong("racha") ?: 0L).toInt()
                if (rachaActual > 0) {
                    val diasSemana =
                            (doc.get("diasSemana") as? List<Any?>)?.filterIsInstance<String>()
                                    ?: emptyList()
                    val ultimaCompletacion = doc.getTimestamp("ultimaCompletacion")
                    val diasGraciaDoc = (doc.getLong("diasGracia") ?: 0L).toInt()

                    if (debeResetearRacha(diasSemana, ultimaCompletacion, diasGraciaDoc)) {
                        batch.update(coleccionHabitos().document(doc.id), mapOf("racha" to 0))
                        hayResets = true
                    }
                }
            }
            // Solo escribimos a Firestore si hay alguna racha que resetear
            if (hayResets) batch.commit().await()
            // Construimos los modelos con los datos definitivos (ya corregidos en Firestore)
            val habitosActualizados =
                    if (hayResets) {
                        coleccionHabitos().whereEqualTo("uid", uid).get().await().documents
                    } else {
                        snapshot.documents
                    }

            val habitos =
                    habitosActualizados.mapNotNull { doc ->
                        val categoriaId = doc.getString("categoriaId") ?: ""
                        val (categoriaNombre, categoriaColor) =
                                mapaCategorias[categoriaId] ?: Pair("", "#C8614A")

                        Habit(
                                id = doc.id,
                                nombre = doc.getString("nombre") ?: "",
                                frecuencia = doc.getString("frecuencia") ?: "",
                                diasSemana =
                                        (doc.get("diasSemana") as? List<Any?>)?.filterIsInstance<
                                                String>()
                                                ?: emptyList(),
                                racha = (doc.getLong("racha") ?: 0L).toInt(),
                                porcentaje = (doc.getLong("porcentaje") ?: 0L).toInt(),
                                totalCompletaciones =
                                        (doc.getLong("totalCompletaciones") ?: 0L).toInt(),
                                diasGracia = (doc.getLong("diasGracia") ?: 0L).toInt(),
                                archivado = doc.getBoolean("archivado") ?: false,
                                tipoCognitivo = doc.getString("tipoCognitivo")
                                                ?: TipoCognitivo.FISICO,
                                uid = doc.getString("uid") ?: "",
                                categoriaId = categoriaId,
                                categoriaNombre = categoriaNombre,
                                categoriaColor = categoriaColor,
                                estaCompletadoHoy = esHoy(doc.getTimestamp("ultimaCompletacion"))
                        )
                    }

            val hoy = obtenerFechaHoy()
            val habitosCompletos = coroutineScope {
                habitos
                        .map { habit ->
                            async {
                                val completadoHoy =
                                        coleccionHabitos()
                                                .document(habit.id)
                                                .collection("completaciones")
                                                .document(hoy)
                                                .get()
                                                .await()
                                                .exists()
                                habit.copy(estaCompletadoHoy = completadoHoy)
                            }
                        }
                        .awaitAll()
            }

            val habitosOrdenados =
                    habitosCompletos.sortedByDescending { habit ->
                        habitosActualizados
                                .find { it.id == habit.id }
                                ?.getTimestamp("fechaCreacion")
                                ?.seconds
                                ?: 0L
                    }

            Result.success(habitosOrdenados)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Eliminamos el hábito y su subcolección de completaciones en lote
    suspend fun eliminarHabito(habitoId: String): Result<Unit> {
        return try {
            val completaciones =
                    coleccionHabitos().document(habitoId).collection("completaciones").get().await()

            val batch = firestore.batch()
            completaciones.documents.forEach { batch.delete(it.reference) }
            batch.delete(coleccionHabitos().document(habitoId))
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completarHabito(habitoId: String): Result<Boolean> {
        return try {
            val uid = obtenerUid()
            val fecha = obtenerFechaHoy()
            val docRef = coleccionHabitos().document(habitoId)
            val completacionRef = docRef.collection("completaciones").document(fecha)

            // Guard anti-doble-completación FUERA de la transacción
            val yaExiste = completacionRef.get().await()
            if (yaExiste.exists()) return Result.success(false)

            // Variables para capturar datos de la transacción
            var nuevaRacha = 0
            var nuevoTotal = 0
            var diasSemanaCapturados = emptyList<String>()

            // Transacción atómica: solo operaciones síncronas
            firestore
                    .runTransaction { transaction ->
                        val snapshot = transaction.get(docRef)

                        val rachaActual = (snapshot.getLong("racha") ?: 0L).toInt()
                        nuevoTotal = (snapshot.getLong("totalCompletaciones") ?: 0L).toInt() + 1
                        diasSemanaCapturados =
                                (snapshot.get("diasSemana") as? List<Any?>)?.filterIsInstance<String>()
                                        ?: emptyList()
                        val ultimaCompletacion = snapshot.getTimestamp("ultimaCompletacion")
                        val diasGracia = (snapshot.getLong("diasGracia") ?: 0L).toInt()

                        nuevaRacha =
                                if (!debeResetearRacha(
                                                diasSemanaCapturados,
                                                ultimaCompletacion,
                                                diasGracia
                                        )
                                )
                                        rachaActual + 1
                                else 1

                        // Registrar completación (síncrono dentro de la tx)
                        transaction.set(
                                completacionRef,
                                mapOf(
                                        "fecha" to fecha,
                                        "timestamp" to com.google.firebase.Timestamp.now()
                                )
                        )

                        // Actualizar hábito (síncrono dentro de la tx)
                        transaction.update(
                                docRef,
                                mapOf(
                                        "racha" to nuevaRacha,
                                        "totalCompletaciones" to nuevoTotal,
                                        "ultimaCompletacion" to com.google.firebase.Timestamp.now()
                                )
                        )
                    }
                    .await()

            // Actualizar porcentaje FUERA de la transacción usando los datos capturados arriba
            val porcentaje = calcularPorcentajeReal(habitoId, diasSemanaCapturados, nuevoTotal)
            docRef.update("porcentaje", porcentaje).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun calcularPorcentajeReal(
            habitoId: String,
            diasSemana: List<String>,
            totalCompletaciones: Int
    ): Int {
        val completacionesDocs =
                coleccionHabitos().document(habitoId).collection("completaciones").get().await()

        val fechasCompletadas = completacionesDocs.documents.map { it.id }.toSet()
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val fechasEsperadas = mutableListOf<String>()
        for (i in 0..29) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val etiqueta = mapaCalendario[cal.get(Calendar.DAY_OF_WEEK)]
            if (diasSemana.contains(etiqueta)) {
                fechasEsperadas.add(formato.format(cal.time))
            }
        }
        return if (fechasEsperadas.isNotEmpty()) {
            val cumplidas = fechasEsperadas.count { fechasCompletadas.contains(it) }
            ((cumplidas.toFloat() / fechasEsperadas.size.toFloat()) * 100).toInt()
        } else 0
    }
    // Eliminamos la completación de hoy, decrementa racha, totalCompletaciones y recalcula el porcentaje real
    suspend fun descompletarHabito(habitoId: String): Result<Unit> {
        return try {
            val hoy = obtenerFechaHoy()
            val refCompletacion =
                    coleccionHabitos().document(habitoId).collection("completaciones").document(hoy)

            val existeHoy = refCompletacion.get().await()
            if (!existeHoy.exists()) return Result.success(Unit)

            refCompletacion.delete().await()

            val refHabito = coleccionHabitos().document(habitoId)
            val snapshot = refHabito.get().await()

            val rachaActual = (snapshot.getLong("racha") ?: 0L).toInt()
            val totalActual = (snapshot.getLong("totalCompletaciones") ?: 0L).toInt()
            val diasSemana =
                    (snapshot.get("diasSemana") as? List<Any?>)?.filterIsInstance<String>()
                            ?: emptyList()

            val nuevoPorcentaje = calcularPorcentaje30Dias(habitoId, diasSemana)

            refHabito
                    .update(
                            mapOf(
                                    "racha" to maxOf(rachaActual - 1, 0),
                                    "porcentaje" to nuevoPorcentaje,
                                    "totalCompletaciones" to maxOf(totalActual - 1, 0)
                            )
                    )
                    .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Comprobamos si el hábito se completó hoy
    suspend fun estaCompletadoHoy(habitoId: String): Result<Boolean> {
        return try {
            val hoy = obtenerFechaHoy()
            val doc =
                    coleccionHabitos()
                            .document(habitoId)
                            .collection("completaciones")
                            .document(hoy)
                            .get()
                            .await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Obtenemos el historial de los últimos 7 días
    suspend fun obtenerHistorial7Dias(habitoId: String): Result<List<DayStatus>> {
        return try {
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val etiquetas = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
            val hoyStr = obtenerFechaHoy()

            val completaciones =
                    coleccionHabitos().document(habitoId).collection("completaciones").get().await()

            val fechasCompletadas = completaciones.documents.map { it.id }.toSet()

            val dias = mutableListOf<DayStatus>()
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val fechaStr = formato.format(cal.time)
                val etiqueta = etiquetas[cal.get(Calendar.DAY_OF_WEEK) - 1]
                val numeroDia = cal.get(Calendar.DAY_OF_MONTH)

                val estado =
                        when {
                            fechasCompletadas.contains(fechaStr) -> DayState.COMPLETED
                            fechaStr == hoyStr -> DayState.TODAY
                            else -> DayState.MISSED
                        }
                dias.add(DayStatus(etiqueta, numeroDia, estado))
            }

            Result.success(dias)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Obtenemos el hábito por su ID
    suspend fun obtenerHabito(habitoId: String): Result<Habit> {
        return try {
            val doc = coleccionHabitos().document(habitoId).get().await()
            val habito =
                    Habit(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "",
                            frecuencia = doc.getString("frecuencia") ?: "",
                            diasSemana =
                                    (doc.get("diasSemana") as? List<Any?>)?.filterIsInstance<String>()
                                            ?: emptyList(),
                            racha = (doc.getLong("racha") ?: 0L).toInt(),
                            porcentaje = (doc.getLong("porcentaje") ?: 0L).toInt(),
                            totalCompletaciones =
                                    (doc.getLong("totalCompletaciones") ?: 0L).toInt(),
                            diasGracia = (doc.getLong("diasGracia") ?: 0L).toInt(),
                            tipoCognitivo = doc.getString("tipoCognitivo") ?: TipoCognitivo.FISICO,
                            uid = doc.getString("uid") ?: "",
                            categoriaId = doc.getString("categoriaId") ?: ""
                    )
            Result.success(habito)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Actualizamos el hábito
    suspend fun actualizarHabito(
            habitoId: String,
            nombre: String,
            frecuencia: String,
            diasSemana: List<String>,
            categoriaId: String,
            diasGracia: Int = 0,
            tipoCognitivo: String = TipoCognitivo.FISICO
    ): Result<Unit> {
        return try {
            coleccionHabitos()
                    .document(habitoId)
                    .update(
                            mapOf(
                                    "nombre" to nombre,
                                    "frecuencia" to frecuencia,
                                    "diasSemana" to diasSemana,
                                    "categoriaId" to categoriaId,
                                    "diasGracia" to diasGracia,
                                    "tipoCognitivo" to tipoCognitivo
                            )
                    )
                    .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun obtenerEstadisticas(): Result<EstadisticasUsuario> {
        return try {
            val uid = obtenerUid()
            val snapshot = coleccionHabitos().whereEqualTo("uid", uid).get().await()

            val totalHabitos = snapshot.size()

            val rachaMaxima =
                    snapshot.documents.maxOfOrNull { (it.getLong("racha") ?: 0L).toInt() } ?: 0

            val completadosHoy =
                    snapshot.documents.count { doc ->
                        esHoy(doc.getTimestamp("ultimaCompletacion"))
                    }

            val totalCompletaciones =
                    snapshot.documents.sumOf { doc ->
                        (doc.getLong("totalCompletaciones") ?: 0L).toInt()
                    }

            Result.success(
                    EstadisticasUsuario(
                            totalHabitos = totalHabitos,
                            completadosHoy = completadosHoy,
                            rachaMaxima = rachaMaxima,
                            totalCompletaciones = totalCompletaciones
                    )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Obtenemos el estado de cada día del mes indicado para el calendario
    suspend fun obtenerHistorialMes(
            habitoId: String,
            año: Int,
            mes: Int
    ): Result<Map<String, DayState>> {
        return try {
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val hoyStr = obtenerFechaHoy()

            val completaciones =
                    coleccionHabitos().document(habitoId).collection("completaciones").get().await()

            val fechasCompletadas = completaciones.documents.map { it.id }.toSet()

            val habitoDoc = coleccionHabitos().document(habitoId).get().await()
            val diasSemana =
                    (habitoDoc.get("diasSemana") as? List<Any?>)?.filterIsInstance<String>()
                            ?: emptyList()

            val resultado = mutableMapOf<String, DayState>()

            val cal = Calendar.getInstance()
            cal.set(año, mes - 1, 1)
            val diasEnMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (dia in 1..diasEnMes) {
                cal.set(año, mes - 1, dia)
                val fechaStr = formato.format(cal.time)
                val etiqueta = mapaCalendario[cal.get(Calendar.DAY_OF_WEEK)]

                val estado =
                        when {
                            fechasCompletadas.contains(fechaStr) -> DayState.COMPLETED
                            fechaStr == hoyStr -> DayState.TODAY
                            fechaStr > hoyStr -> DayState.NOT_APPLICABLE
                            !diasSemana.contains(etiqueta) -> DayState.NOT_APPLICABLE
                            else -> DayState.MISSED
                        }
                resultado[fechaStr] = estado
            }

            Result.success(resultado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Archivamos un hábito -> no se elimina, solo lo oculta de la lista activa
    suspend fun archivarHabito(habitoId: String): Result<Unit> {
        return try {
            coleccionHabitos().document(habitoId).update(mapOf("archivado" to true)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Restauramos un hábito archivado a la lista activa
    suspend fun restaurarHabito(habitoId: String): Result<Unit> {
        return try {
            coleccionHabitos().document(habitoId).update(mapOf("archivado" to false)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Obtienemos los hábitos archivados del usuario
    suspend fun obtenerHabitosArchivados(): Result<List<Habit>> {
        return try {
            val uid = obtenerUid()
            val snapshot =
                    coleccionHabitos()
                            .whereEqualTo("uid", uid)
                            .whereEqualTo("archivado", true)
                            .get()
                            .await()

            val snapshotCategorias =
                    firestore.collection("categorias").whereEqualTo("uid", uid).get().await()

            val mapaCategorias =
                    snapshotCategorias.documents.associate { doc ->
                        doc.id to
                                Pair(
                                        doc.getString("nombre") ?: "",
                                        doc.getString("color") ?: "#C8614A"
                                )
                    }

            val habitos =
                    snapshot.documents.mapNotNull { doc ->
                        val categoriaId = doc.getString("categoriaId") ?: ""
                        val (categoriaNombre, categoriaColor) =
                                mapaCategorias[categoriaId] ?: Pair("", "#C8614A")

                        Habit(
                                id = doc.id,
                                nombre = doc.getString("nombre") ?: "",
                                frecuencia = doc.getString("frecuencia") ?: "",
                                diasSemana =
                                        (doc.get("diasSemana") as? List<Any?>)?.filterIsInstance<
                                                String>()
                                                ?: emptyList(),
                                racha = (doc.getLong("racha") ?: 0L).toInt(),
                                porcentaje = (doc.getLong("porcentaje") ?: 0L).toInt(),
                                totalCompletaciones =
                                        (doc.getLong("totalCompletaciones") ?: 0L).toInt(),
                                archivado = true,
                                tipoCognitivo = doc.getString("tipoCognitivo")
                                                ?: TipoCognitivo.FISICO,
                                uid = doc.getString("uid") ?: "",
                                categoriaId = categoriaId,
                                categoriaNombre = categoriaNombre,
                                categoriaColor = categoriaColor
                        )
                    }

            Result.success(
                    habitos.sortedByDescending { habit ->
                        snapshot.documents
                                .find { it.id == habit.id }
                                ?.getTimestamp("fechaCreacion")
                                ?.seconds
                                ?: 0L
                    }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Calculamos el % de cumplimiento de la semana actual -> lunes a hoy
    suspend fun obtenerPorcentajeSemana(): Result<Int> {
        return try {
            val uid = obtenerUid()
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

            val calLunes = Calendar.getInstance()
            val diasDesdeElLunes = (calLunes.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            calLunes.add(Calendar.DAY_OF_YEAR, -diasDesdeElLunes)
            val inicioSemana = formato.format(calLunes.time)
            val finSemana = obtenerFechaHoy()

            val snapshot =
                    coleccionHabitos()
                            .whereEqualTo("uid", uid)
                            .whereEqualTo("archivado", false)
                            .get()
                            .await()

            var totalProgramadas = 0
            var totalCompletadas = 0

            val resultados = coroutineScope {
                snapshot.documents
                        .map { doc ->
                            async {
                                val completaciones =
                                        coleccionHabitos()
                                                .document(doc.id)
                                                .collection("completaciones")
                                                .get()
                                                .await()
                                Pair(doc, completaciones)
                            }
                        }
                        .awaitAll()
            }

            for ((doc, completaciones) in resultados) {
                val diasSemana =
                        (doc.get("diasSemana") as? List<Any?>)?.filterIsInstance<String>()
                                ?: emptyList()

                val calTemp = calLunes.clone() as Calendar
                while (formato.format(calTemp.time) <= finSemana) {
                    val etiqueta = mapaCalendario[calTemp.get(Calendar.DAY_OF_WEEK)]
                    if (etiqueta != null && diasSemana.contains(etiqueta)) {
                        totalProgramadas++
                    }
                    calTemp.add(Calendar.DAY_OF_YEAR, 1)
                }

                totalCompletadas +=
                        completaciones.documents.count { d ->
                            val fecha = d.getString("fecha") ?: d.id
                            fecha >= inicioSemana && fecha <= finSemana
                        }
            }

            Result.success(
                    if (totalProgramadas > 0) (totalCompletadas * 100) / totalProgramadas else 0
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun obtenerResumenSemanal(): Result<ResumenSemanal> {
        return try {
            val uid = obtenerUid()
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

            val calLunes = Calendar.getInstance()
            val diasDesdeElLunes = (calLunes.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            calLunes.add(Calendar.DAY_OF_YEAR, -diasDesdeElLunes)
            val inicioSemana = formato.format(calLunes.time)
            val finSemana = obtenerFechaHoy()

            val snapshot =
                    coleccionHabitos()
                            .whereEqualTo("uid", uid)
                            .whereEqualTo("archivado", false)
                            .get()
                            .await()

            var totalProgramadas = 0
            var totalCompletadas = 0
            var rachaMaxima = 0
            var habitoMejorRacha = ""
            var peorPorcentaje = 101
            var habitoMasDescuidado: String? = null

            val resultados = coroutineScope {
                snapshot.documents
                        .map { doc ->
                            async {
                                val completaciones =
                                        coleccionHabitos()
                                                .document(doc.id)
                                                .collection("completaciones")
                                                .get()
                                                .await()
                                Pair(doc, completaciones)
                            }
                        }
                        .awaitAll()
            }

            for ((doc, completaciones) in resultados) {
                val nombre = doc.getString("nombre") ?: ""
                val diasSemana =
                        (doc.get("diasSemana") as? List<Any?>)?.filterIsInstance<String>()
                                ?: emptyList()
                val racha = (doc.getLong("racha") ?: 0L).toInt()

                if (racha > rachaMaxima) {
                    rachaMaxima = racha
                    habitoMejorRacha = nombre
                }

                val calTemp = calLunes.clone() as Calendar
                var programadasEsteHabito = 0
                while (formato.format(calTemp.time) <= finSemana) {
                    val etiqueta = mapaCalendario[calTemp.get(Calendar.DAY_OF_WEEK)]
                    if (etiqueta != null && diasSemana.contains(etiqueta)) {
                        programadasEsteHabito++
                    }
                    calTemp.add(Calendar.DAY_OF_YEAR, 1)
                }

                if (programadasEsteHabito > 0) {
                    totalProgramadas += programadasEsteHabito

                    val completadasEsteHabito =
                            completaciones.documents.count { d ->
                                val fecha = d.getString("fecha") ?: d.id
                                fecha >= inicioSemana && fecha <= finSemana
                            }
                    totalCompletadas += completadasEsteHabito

                    val pct = (completadasEsteHabito * 100) / programadasEsteHabito
                    if (pct < peorPorcentaje) {
                        peorPorcentaje = pct
                        habitoMasDescuidado = if (pct < 50) nombre else null
                    }
                }
            }

            val porcentajeSemana =
                    if (totalProgramadas > 0) (totalCompletadas * 100) / totalProgramadas else 0

            Result.success(
                    ResumenSemanal(
                            porcentajeSemana = porcentajeSemana,
                            totalHabitos = snapshot.size(),
                            completacionesSemana = totalCompletadas,
                            totalProgramadasSemana = totalProgramadas,
                            habitoMejorRacha = habitoMejorRacha,
                            rachaMaxima = rachaMaxima,
                            habitoMasDescuidado = habitoMasDescuidado
                    )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Retorna un mapa tipo -> cantidad de hábitos activos de ese tipo
    suspend fun obtenerBalanceCognitivo(): Result<Map<String, Int>> {
        return try {
            val uid = obtenerUid()
            val snapshot =
                    coleccionHabitos()
                            .whereEqualTo("uid", uid)
                            .whereEqualTo("archivado", false)
                            .get()
                            .await()

            val balance = mutableMapOf<String, Int>()
            TipoCognitivo.todos.forEach { balance[it] = 0 }

            snapshot.documents.forEach { doc ->
                val tipo = doc.getString("tipoCognitivo") ?: TipoCognitivo.FISICO
                balance[tipo] = (balance[tipo] ?: 0) + 1
            }

            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Guarda o actualiza la reflexión del día para un hábito
    suspend fun guardarReflexion(habitoId: String, texto: String): Result<Unit> {
        return try {
            val fecha = obtenerFechaHoy()
            coleccionHabitos()
                    .document(habitoId)
                    .collection("reflexiones")
                    .document(fecha)
                    .set(
                            mapOf(
                                    "fecha" to fecha,
                                    "texto" to texto,
                                    "timestamp" to com.google.firebase.Timestamp.now()
                            )
                    )
                    .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtiene las últimas 5 reflexiones de un hábito, ordenadas por fecha desc
    suspend fun obtenerReflexiones(habitoId: String): Result<List<Reflexion>> {
        return try {
            val snapshot =
                    coleccionHabitos()
                            .document(habitoId)
                            .collection("reflexiones")
                            .orderBy(
                                    "timestamp",
                                    com.google.firebase.firestore.Query.Direction.DESCENDING
                            )
                            .limit(5)
                            .get()
                            .await()

            val reflexiones =
                    snapshot.documents.mapNotNull { doc ->
                        Reflexion(
                                fecha = doc.getString("fecha") ?: "",
                                texto = doc.getString("texto") ?: "",
                                timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0L
                        )
                    }
            Result.success(reflexiones)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
