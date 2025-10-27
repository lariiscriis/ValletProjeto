package br.edu.fatecpg.valletprojeto.dao

import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VagaDao {

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        fun cadastrarVaga(
            vaga: Vaga,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) {
            val vagaCadastrada = hashMapOf(
                "numero" to vaga.numero,
                "localizacao" to vaga.localizacao,
                "preco" to vaga.preco,
                "tipo" to vaga.tipo,
                "disponivel" to vaga.disponivel,
                "estacionamentoId" to vaga.estacionamentoId
            )
            vaga.id = vaga.numero
            db.collection("vaga")
                .document(vaga.id)
                .set(vagaCadastrada)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message ?: "Erro desconhecido") }

        }

}
