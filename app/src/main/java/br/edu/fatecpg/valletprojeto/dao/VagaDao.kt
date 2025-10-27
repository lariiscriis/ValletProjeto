package br.edu.fatecpg.valletprojeto.dao

import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VagaDao {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun cadastrarVaga(
        vaga: Vaga,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (vaga.estacionamentoId.isBlank()) {
            onError("ID do estacionamento não informado")
            return
        }

        val vagaCadastrada = hashMapOf(
            "numero" to vaga.numero,
            "localizacao" to vaga.localizacao,
            "preco" to vaga.preco,
            "tipo" to vaga.tipo,
            "disponivel" to vaga.disponivel,
            "estacionamentoId" to vaga.estacionamentoId
        )

        db.collection("vaga")
            .add(vagaCadastrada)
            .addOnSuccessListener { doc ->
                vaga.id = doc.id
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Erro ao cadastrar vaga")
            }
    }
}
