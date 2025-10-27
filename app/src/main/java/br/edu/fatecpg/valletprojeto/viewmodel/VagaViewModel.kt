package br.edu.fatecpg.valletprojeto.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.dao.VagaDao
import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class VagaViewModel : ViewModel() {
    private val vagaDao = VagaDao()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val vagasCollection = db.collection("vaga")

    val vagas = MutableLiveData<List<Vaga>>()
    val errorMessage = MutableLiveData<String>()
    private var listenerRegistration: ListenerRegistration? = null

    fun cadastrarVaga(
        vaga: Vaga,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        vagaDao.cadastrarVaga(vaga, onSuccess, onFailure)
    }

    fun fetchTodasVagas() {
        listenerRegistration?.remove()
        listenerRegistration = vagasCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage.value = "Erro ao buscar vagas: ${error.message}"
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Vaga::class.java)?.apply { id = doc.id }
                } ?: emptyList()

                vagas.value = lista
            }
    }


    fun fetchVagasPorEstacionamento(estacionamentoId: String) {
        if (estacionamentoId.isBlank()) {
            errorMessage.value = "ID do estacionamento não informado."
            vagas.value = emptyList()
            return
        }

        listenerRegistration?.remove()

        listenerRegistration = vagasCollection
            .whereEqualTo("estacionamentoId", estacionamentoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage.value = "Erro ao buscar vagas: ${error.message}"
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Vaga::class.java)?.apply { id = doc.id }
                } ?: emptyList()

                vagas.value = lista
            }
    }

    fun verificarSeTemVagas(
        estacionamentoId: String,
        onResult: (Boolean) -> Unit
    ) {
        if (estacionamentoId.isBlank()) {
            onResult(false)
            return
        }

        vagasCollection
            .whereEqualTo("estacionamentoId", estacionamentoId)
            .get()
            .addOnSuccessListener { snapshot ->
                val temVagas = snapshot != null && !snapshot.isEmpty
                onResult(temVagas)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }


    fun deleteVaga(vagaId: String, onComplete: (Boolean, String?) -> Unit) {
        vagasCollection.document(vagaId)
            .delete()
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }


    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
