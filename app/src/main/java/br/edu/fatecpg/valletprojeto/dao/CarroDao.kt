package br.edu.fatecpg.valletprojeto.dao

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import br.edu.fatecpg.valletprojeto.model.Carro

object CarroDao {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun cadastrarCarro(
        carro: Carro,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailUsuario = auth.currentUser?.email ?: "Desconhecido"

        val carroCadastrado = hashMapOf(
            "placa" to carro.placa,
            "marca" to carro.marca,
            "modelo" to carro.modelo,
            "usuarioEmail" to emailUsuario,
            "data_cadastro" to FieldValue.serverTimestamp()
        )

        db.collection("carro")
            .add(carroCadastrado)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro desconhecido") }
    }
}
