package br.edu.fatecpg.valletprojeto.dao

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import br.edu.fatecpg.valletprojeto.model.Carro

object CarroDao {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

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
            "ano" to carro.ano,
            "km" to carro.km,
            "tipo" to carro.tipo,
            "usuarioEmail" to emailUsuario,
            "data_cadastro" to FieldValue.serverTimestamp()
        )
        carro.id = carro.placa
        db.collection("carro")
            .document(carro.id)
            .set(carroCadastrado)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro desconhecido") }

    }

    fun listarCarrosDoUsuario(
        onSuccess: (List<Carro>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailUsuario = FirebaseAuth.getInstance().currentUser?.email ?: return

        FirebaseFirestore.getInstance()
            .collection("carro")
            .whereEqualTo("usuarioEmail", emailUsuario)
            .get()
            .addOnSuccessListener { result ->
                val lista = mutableListOf<Carro>()
                for (doc in result) {
                    val carro = doc.toObject(Carro::class.java)
                    carro.id = doc.id
                    lista.add(carro)
                }
                onSuccess(lista)
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Erro desconhecido")
            }
    }
}
