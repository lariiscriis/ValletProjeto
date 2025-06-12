package br.edu.fatecpg.valletprojeto.viewmodel
import androidx.lifecycle.MutableLiveData
import br.edu.fatecpg.valletprojeto.dao.VagaDao
import androidx.lifecycle.ViewModel
import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class VagaViewModel : ViewModel() {
    private val VagaDao = VagaDao()
    private val db = Firebase.firestore
    private val vagasCollection = db.collection("vaga")

    val vagas = MutableLiveData<List<Vaga>>()
    val errorMessage = MutableLiveData<String>()

    fun cadastrarVaga(
        vaga: Vaga,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        VagaDao.cadastrarVaga(vaga, onSuccess, onFailure)
    }

    fun fetchVagas() {
        vagasCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage.value = error.message
                    return@addSnapshotListener
                }

                val vagasList = snapshot?.toObjects(Vaga::class.java)
                vagas.value = vagasList
            }
    }

    fun deleteVaga(vagaId: String, onComplete: (Boolean, String?) -> Unit) {
        vagasCollection.document(vagaId)
            .delete()
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }
}