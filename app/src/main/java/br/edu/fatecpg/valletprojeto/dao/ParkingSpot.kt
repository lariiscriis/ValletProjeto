package br.edu.fatecpg.valletprojeto.dao

import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ParkingSpotDao {

    private val db = FirebaseFirestore.getInstance()

    // Função para listar todas as vagas
    fun listarTodasAsVagas(
        onSuccess: (List<Vaga>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("vaga")
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.mapNotNull { doc ->
                    try {
                        val numero = doc.getString("numero") ?: ""
                        val localizacao = doc.getString("localizacao") ?: ""
                        val preco = doc.getDouble("preco") ?: 0.0
                        val tipo = doc.getString("tipo") ?: ""
                        val disponivel = doc.getBoolean("disponivel") ?: true
                        Vaga(
                            id = doc.id,
                            numero = numero,
                            localizacao = localizacao,
                            preco = preco,
                            tipo = tipo,
                            disponivel = disponivel
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onSuccess(lista)
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Erro desconhecido")
            }
    }

}
