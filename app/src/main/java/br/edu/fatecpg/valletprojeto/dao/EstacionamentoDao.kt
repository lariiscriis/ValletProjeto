package br.edu.fatecpg.valletprojeto.dao

import br.edu.fatecpg.valletprojeto.model.Estacionamento
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class EstacionamentoDao {
    object EstacionamentoDao {
        private val auth = FirebaseAuth.getInstance()
        private val db = FirebaseFirestore.getInstance()

        fun cadastrarEstacionamento(
            est: Estacionamento,
            onSuccess: () -> Unit,
            onFailure: (String) -> Unit
        ) {
            val emailAdmin = auth.currentUser?.email ?: return onFailure("Admin não autenticado")
            val docRef = db.collection("estacionamento").document()
            val idGerado = docRef.id

            val data = hashMapOf(
                "id" to idGerado,
                "nome" to est.nome,
                "cnpj" to est.cnpj,
                "telefone" to est.telefone,
                "endereco" to est.endereco,
                "cep" to est.cep,
                "quantidadeVagasTotal" to est.quantidadeVagasTotal,
                "tiposVagasComum" to est.tiposVagasComum,
                "tiposVagasIdosoPcd" to est.tiposVagasIdosoPcd,
                "quantidadeVagasComum" to est.quantidadeVagasComum,
                "quantidadeVagasIdosoPcd" to est.quantidadeVagasIdosoPcd,
                "possuiCobertura" to est.possuiCobertura,
                "numeroPavimentos" to est.numeroPavimentos,
                "valorHora" to est.valorHora,
                "valorDiario" to est.valorDiario,
                "horarioFuncionamento" to est.horarioFuncionamento,
                "tempoMaxReservaHoras" to est.tempoMaxReservaHoras,
                "toleranciaReservaMinutos" to est.toleranciaReservaMinutos,
                "fotoEstacionamentoUri" to est.fotoEstacionamentoUri,
                "adminUid" to FirebaseAuth.getInstance().currentUser?.uid,
                "adminEmail" to emailAdmin,
                "dataCadastro" to FieldValue.serverTimestamp()
            )

            docRef.set(data)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Erro desconhecido")
                }
        }


        fun listarPorAdmin(
            onSuccess: (List<Estacionamento>) -> Unit,
            onFailure: (String) -> Unit
        ) {
            val emailAdmin = auth.currentUser?.email ?: return onFailure("Admin não autenticado")
            db.collection("estacionamento")
                .whereEqualTo("adminEmail", emailAdmin)
                .get()
                .addOnSuccessListener { res ->
                    val lista = res.documents.map { doc ->
                        val e = doc.toObject(Estacionamento::class.java)!!
                        e.id = doc.id
                        e
                    }
                    onSuccess(lista)
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Erro ao listar")
                }
        }
    }

}