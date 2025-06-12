package br.edu.fatecpg.valletprojeto.dao

import br.edu.fatecpg.valletprojeto.model.Carro
import br.edu.fatecpg.valletprojeto.model.Usuario
import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
            )
            vaga.id = vaga.numero
            CarroDao.db.collection("carro")
                .document(vaga.id)
                .set(vagaCadastrada)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message ?: "Erro desconhecido") }

        }

}