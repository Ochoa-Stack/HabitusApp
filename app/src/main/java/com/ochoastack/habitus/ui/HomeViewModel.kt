package com.ochoastack.habitus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ochoastack.habitus.data.EstadisticasUsuario
import com.ochoastack.habitus.data.HabitRepository
import com.ochoastack.habitus.data.TipoCognitivo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/* ViewModel de la pantalla de inicio */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    /* Carga estadísticas, porcentaje semanal y balance cognitivo en paralelo */
    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val (statsResult, semanaResult, balanceResult) = cargarEnParalelo()

                val stats = statsResult.getOrThrow()
                val pct = semanaResult.getOrElse { 0 }
                val balance = balanceResult.getOrElse { emptyMap() }

                _uiState.value = HomeUiState.Success(
                    estadisticas = stats,
                    porcentajeSemana = pct,
                    balanceCognitivo = balance,
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private suspend fun cargarEnParalelo() = coroutineScope {
        val statsDeferred = async { habitRepository.obtenerEstadisticas() }
        val semanaDeferred = async { habitRepository.obtenerPorcentajeSemana() }
        val balanceDeferred = async { habitRepository.obtenerBalanceCognitivo() }
        Triple(statsDeferred.await(), semanaDeferred.await(), balanceDeferred.await())
    }
}

// Estados de UI

/* Representación sellada del estado de la pantalla de inicio */
sealed class HomeUiState {
    data object Loading : HomeUiState()

    data class Success(
        val estadisticas: EstadisticasUsuario,
        val porcentajeSemana: Int,
        val balanceCognitivo: Map<String, Int>,
    ) : HomeUiState() {
        /* Mensaje motivacional calculado a partir de los datos de la semana */
        val tipoMensaje: TipoMensaje = when {
            porcentajeSemana == 100 -> TipoMensaje.PERFECTO
            porcentajeSemana >= 80 -> TipoMensaje.EXCELENTE
            porcentajeSemana >= 50 -> TipoMensaje.BIEN
            estadisticas.rachaMaxima > 0 -> TipoMensaje.RACHA
            else -> TipoMensaje.COMENZAR
        }
        val tieneBalanceCognitivo: Boolean = balanceCognitivo.values.sum() > 0
        val tiposActivos: List<Pair<String, Int>> =
            TipoCognitivo.todos
                .mapNotNull { tipo ->
                    val cantidad = balanceCognitivo[tipo] ?: 0
                    if (cantidad > 0) Pair(tipo, cantidad) else null
                }
    }

    data class Error(val mensaje: String) : HomeUiState()
}

enum class TipoMensaje { PERFECTO, EXCELENTE, BIEN, RACHA, COMENZAR }
