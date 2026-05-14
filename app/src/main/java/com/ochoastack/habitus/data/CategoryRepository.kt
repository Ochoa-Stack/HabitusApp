package com.ochoastack.habitus.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Creamos 'HabitCategory' para representar una categoría de hábitos (modelo)
data class HabitCategory(
    val id: String       = "",
    val nombre: String   = "",
    val color: String    = "#C8614A",
    val esDefault: Boolean = false,
    val uid: String      = "",
    val totalHabitos: Int = 0    // Hacemos el cálculo localmente
)

class CategoryRepository {    // Interactuamos con Firestore
    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private fun obtenerUid(): String =
        auth.currentUser?.uid ?: throw Exception("No hay sesión activa")
    // Obtenemos las categorías del usuario junto con el conteo de hábitos en cada una
    suspend fun obtenerCategorias(): Result<List<HabitCategory>> {
        return try {
            val uid = obtenerUid()
            // Obtenemos las categorías del usuario
            val snapshotCategorias = firestore.collection("categorias")
                .whereEqualTo("uid", uid)
                .get()
                .await()
            // Obtenemos todos los hábitos del usuario para contar por categoría
            val snapshotHabitos = firestore.collection("habitos")
                .whereEqualTo("uid", uid)
                .get()
                .await()
            // Construimos un mapa de 'categoriaId' -> cantidad de hábitos
            val conteoHabitos = snapshotHabitos.documents
                .groupingBy { it.getString("categoriaId") ?: "" }
                .eachCount()
            // Construimos la lista de categorías con el conteo de hábitos
            val categorias = snapshotCategorias.documents.map { doc ->
                HabitCategory(
                    id          = doc.id,
                    nombre      = doc.getString("nombre")   ?: "",
                    color       = doc.getString("color")    ?: "#C8614A",
                    esDefault   = doc.getBoolean("esDefault") ?: false,
                    uid         = doc.getString("uid")      ?: "",
                    totalHabitos = conteoHabitos[doc.id]    ?: 0
                )
            }
            Result.success(categorias)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Creamos una nueva categoría personalizada
    suspend fun crearCategoria(nombre: String, color: String): Result<String> {
        return try {
            val uid  = obtenerUid()
            val dato = mapOf(
                "uid"         to uid,
                "nombre"      to nombre,
                "color"       to color,
                "esDefault"   to false,
                "fechaCreacion" to com.google.firebase.Timestamp.now()
            )
            val ref = firestore.collection("categorias").add(dato).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Inicializamos las categorías por defecto en Firestore (una sola vez por Usuario)
    suspend fun inicializarCategoriasPorDefecto(): Result<Unit> {
        return try {
            val uid = obtenerUid()
            // Verificamos si ya existen categorías por defecto
            val defaultsExistentes = firestore.collection("categorias")
                .whereEqualTo("uid", uid)
                .whereEqualTo("esDefault", true)
                .get()
                .await()
            // Si no existen, las creamos
            if (defaultsExistentes.isEmpty) {
                val categoriasPorDefecto = listOf(
                    mapOf("nombre" to "Salud",      "color" to "#C8614A", "esDefault" to true),
                    mapOf("nombre" to "Estudio",    "color" to "#673AB7", "esDefault" to true),
                    mapOf("nombre" to "Meditación", "color" to "#9B59B6", "esDefault" to true),
                    mapOf("nombre" to "Arte",       "color" to "#E91E8C", "esDefault" to true),
                    mapOf("nombre" to "Deporte",    "color" to "#E74C3C", "esDefault" to true)
                )
                // Realizamos la escritura en lote
                val batch = firestore.batch()
                categoriasPorDefecto.forEach { categoria ->
                    val ref = firestore.collection("categorias").document()
                    batch.set(ref, categoria + mapOf(
                        "uid"           to uid,
                        "fechaCreacion" to com.google.firebase.Timestamp.now()
                    ))
                }
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Eliminamos una categoría personalizada
    suspend fun eliminarCategoria(categoriaId: String): Result<Unit> {
        return try {
            firestore.collection("categorias").document(categoriaId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
