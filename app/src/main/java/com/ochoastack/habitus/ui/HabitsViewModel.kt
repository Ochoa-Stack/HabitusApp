package com.ochoastack.habitus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ochoastack.habitus.data.Habit
import com.ochoastack.habitus.data.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel de la pantalla de lista de hábitos
@HiltViewModel
class HabitsViewModel
    @Inject
    constructor(
        private val habitRepository: HabitRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<HabitsUiState>(HabitsUiState.Loading)
        val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

        init {
            cargarHabitos()
        }

        // Recarga la lista completa desde Firestore
        fun cargarHabitos() {
            viewModelScope.launch {
                _uiState.value = HabitsUiState.Loading
                habitRepository.obtenerHabitos().fold(
                    onSuccess = { habitos ->
                        _uiState.value =
                            if (habitos.isEmpty()) {
                                HabitsUiState.Empty
                            } else {
                                HabitsUiState.Success(habitos)
                            }
                    },
                    onFailure = { e ->
                        _uiState.value = HabitsUiState.Error(e.message ?: "Error al cargar hábitos")
                    },
                )
            }
        }

        /* Archiva el hábito indicado y recarga la lista.
         * @param habito Hábito a archivar. Se conserva para el undo posterior.
         * @param onArchivado Callback invocado en el hilo principal cuando la operación termina OK */
        fun archivarHabito(
            habito: Habit,
            onArchivado: () -> Unit,
        ) {
            viewModelScope.launch {
                habitRepository.archivarHabito(habito.id).fold(
                    onSuccess = {
                        cargarHabitos()
                        onArchivado()
                    },
                    onFailure = {
                        // El Fragment restaura el item visualmente; emitimos un evento de error
                        _uiState.value = HabitsUiState.Error("No se pudo archivar el hábito")
                        cargarHabitos()
                    },
                )
            }
        }

        // Restaura un hábito previamente archivado (acción de undo).
        fun restaurarHabito(habitoId: String) {
            viewModelScope.launch {
                habitRepository.restaurarHabito(habitoId)
                cargarHabitos()
            }
        }
    }

// Estados de UI

// Representación sellada del estado de la pantalla de hábitos
sealed class HabitsUiState {
    data object Loading : HabitsUiState()

    data object Empty : HabitsUiState()

    data class Success(val habitos: List<Habit>) : HabitsUiState()

    data class Error(val mensaje: String) : HabitsUiState()
}
