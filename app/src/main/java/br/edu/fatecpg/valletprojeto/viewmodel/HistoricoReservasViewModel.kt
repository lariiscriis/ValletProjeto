package br.edu.fatecpg.valletprojeto.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.edu.fatecpg.valletprojeto.model.Reserva
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class HistoricoUIState(
    val reservas: List<Reserva> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HistoricoReservasViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableLiveData(HistoricoUIState())
    val uiState: LiveData<HistoricoUIState> = _uiState

    fun carregarHistoricoCompleto() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null)
            val userId = auth.currentUser?.uid

            if (userId == null) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Usuário não autenticado."
                )
                return@launch
            }

            try {
                val snapshot = db.collection("reserva")
                    .whereEqualTo("usuarioId", userId)
                    .orderBy("fimReserva", Query.Direction.DESCENDING)
                    .get().await()

                val reservas = snapshot.documents.mapNotNull { document ->
                    document.toObject(Reserva::class.java)?.apply {
                        id = document.id
                    }
                }

                _uiState.value = _uiState.value?.copy(
                    reservas = reservas,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Erro ao carregar histórico: ${e.message}"
                )
            }
        }
    }
}