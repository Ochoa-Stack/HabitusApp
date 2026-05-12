package com.example.habittrackerapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider

class FirebaseAuthRepository {    // Declaramos el repositorio de autenticación de Firebase
    private val auth      = FirebaseAuth.getInstance()         // Instanciamos la autentificación de Firebase
    private val firestore = FirebaseFirestore.getInstance()    // Instanciamos Firestore
    
    companion object {
        private const val TAG = "FirebaseAuthRepository"
    }

    fun obtenerUsuarioActual(): FirebaseUser? = auth.currentUser    // Obtenemos el usuario actual
    
    // Obtenemos el nombre del usuario desde Firestore usando el repositorio, sin acceso directo desde la UI
    suspend fun obtenerNombreUsuario(): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No hay sesión activa")
            val doc = firestore.collection("usuarios").document(uid).get().await()
            val nombre = doc.getString("nombre") ?: "Usuario"
            Result.success(nombre)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener nombre de usuario", e)
            Result.failure(e)
        }
    }
    
    // Iniciamos sesión con correo y contraseña (Usuario)
    suspend fun iniciarSesion(correo: String, contraseña: String): Result<FirebaseUser> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(correo, contraseña).await()
            val usuario   = resultado.user ?: throw Exception("Usuario no encontrado")
            
            // Aseguramos que las categorías por defecto existan
            try {
                CategoryRepository().inicializarCategoriasPorDefecto()
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar categorías en login", e)
            }
            
            Result.success(usuario)
        } catch (e: Exception) {
            Log.e(TAG, "Error en iniciarSesion", e)
            Result.failure(e)
        }
    }
    
    // Registramos un nuevo usuario con correo y contraseña
    suspend fun registrarUsuario(
        nombre: String,
        correo: String,
        contraseña: String
    ): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Registrando usuario: $correo")
            val resultado = auth.createUserWithEmailAndPassword(correo, contraseña).await()
            val usuario   = resultado.user ?: throw Exception("Error al crear usuario en Firebase Auth")
            
            val datosUsuario = mapOf(
                "nombre"        to nombre,
                "correo"        to correo,
                "fechaRegistro" to com.google.firebase.Timestamp.now()
            )
            
            Log.d(TAG, "Guardando datos de usuario en Firestore para UID: ${usuario.uid}")
            firestore.collection("usuarios")
                .document(usuario.uid)
                .set(datosUsuario)
                .await()
            
            Log.d(TAG, "Inicializando categorías por defecto")
            CategoryRepository().inicializarCategoriasPorDefecto()
            
            Result.success(usuario)
        } catch (e: Exception) {
            Log.e(TAG, "Error en registrarUsuario", e)
            Result.failure(e)
        }
    }
    
    // Recuperamos contraseña (Usuario)
    suspend fun enviarCorreoRestablecimiento(correo: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(correo).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar correo de restablecimiento", e)
            Result.failure(e)
        }
    }
    
    fun cerrarSesion() = auth.signOut()    // Cerramos sesión
    
    // Iniciamos sesión con Google (Usuario)
    suspend fun iniciarSesionConGoogle(
        context: Context,
        webClientId: String
    ): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Iniciando sesión con Google...")
            val opcion = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val solicitud = GetCredentialRequest.Builder()
                .addCredentialOption(opcion)
                .build()

            val credentialManager = CredentialManager.create(context)
            val resultado = credentialManager.getCredential(context, solicitud)
            val credencial = resultado.credential

            val tokenGoogle = GoogleIdTokenCredential
                .createFrom(credencial.data)
                .idToken

            val credencialFirebase = GoogleAuthProvider.getCredential(tokenGoogle, null)
            val resultadoFirebase  = auth.signInWithCredential(credencialFirebase).await()
            val usuario = resultadoFirebase.user
                ?: throw Exception("No se pudo autenticar con Google en Firebase")
            
            // Si es un usuario nuevo (o no tiene documento), creamos su perfil
            asegurarPerfilUsuario(usuario)
            
            // Inicializamos categorías por defecto
            try {
                CategoryRepository().inicializarCategoriasPorDefecto()
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar categorías en login Google", e)
            }
            
            Result.success(usuario)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Error de CredentialManager", e)
            Result.failure(Exception("Inicio con Google cancelado o no disponible: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error en iniciarSesionConGoogle", e)
            Result.failure(e)
        }
    }

    private suspend fun asegurarPerfilUsuario(usuario: FirebaseUser) {
        try {
            val doc = firestore.collection("usuarios").document(usuario.uid).get().await()
            if (!doc.exists()) {
                Log.d(TAG, "Creando perfil de usuario para login con Google")
                val datosUsuario = mapOf(
                    "nombre"        to (usuario.displayName ?: "Usuario Habitus"),
                    "correo"        to (usuario.email ?: ""),
                    "fechaRegistro" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("usuarios")
                    .document(usuario.uid)
                    .set(datosUsuario)
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al asegurar perfil de usuario", e)
        }
    }
}
