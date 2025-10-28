package br.edu.fatecpg.valletprojeto.dao

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import br.edu.fatecpg.valletprojeto.model.Veiculo

object VeiculoDao {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    fun cadastrarVeiculo(
        veiculo: Veiculo,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailUsuario = auth.currentUser?.email ?: "Desconhecido"

        val veiculoCadastrado = hashMapOf(
            "placa" to veiculo.placa,
            "marca" to veiculo.marca,
            "modelo" to veiculo.modelo,
            "ano" to veiculo.ano,
            "km" to veiculo.km,
            "tipo" to veiculo.tipo,
            "usuarioEmail" to emailUsuario,
            "data_cadastro" to FieldValue.serverTimestamp()
        )
        veiculo.id = veiculo.placa
        db.collection("veiculo")
            .document(veiculo.id)
            .set(veiculoCadastrado)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro desconhecido") }

    }

    fun listarVeiculosDoUsuario(
        onSuccess: (List<Veiculo>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val emailUsuario = FirebaseAuth.getInstance().currentUser?.email ?: return

        FirebaseFirestore.getInstance()
            .collection("veiculo")
            .whereEqualTo("usuarioEmail", emailUsuario)
            .get()
            .addOnSuccessListener { result ->
                val lista = mutableListOf<Veiculo>()
                for (doc in result) {
                    val veiculo = doc.toObject(Veiculo::class.java)
                    veiculo.id = doc.id
                    lista.add(veiculo)
                }
                onSuccess(lista)
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Erro desconhecido")
            }
    }
}
