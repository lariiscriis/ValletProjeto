package br.edu.fatecpg.valletprojeto

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroVagaBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CadastroVagaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroVagaBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroVagaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore

        binding.btnCadastrarVaga.setOnClickListener {
            cadastrarNovaVaga()
        }
    }

    private fun cadastrarNovaVaga() {
        val estacionamentoId = intent.getStringExtra("estacionamentoId") ?: ""
        val piso = binding.edtPiso.text.toString().trim()
        val qtdTotal = binding.edtQtdTotalVagas.text.toString().toIntOrNull() ?: 0
        val qtdIdosoPcd = binding.edtQtdIdosoPcd.text.toString().toIntOrNull() ?: 0
        val qtdMoto = binding.edtQtdMoto.text.toString().toIntOrNull() ?: 0
        val precoHora = binding.edtPrecoHora.text.toString().toDoubleOrNull() ?: 0.0

        if (estacionamentoId.isEmpty() || piso.isEmpty() || qtdTotal <= 0) {
            Toast.makeText(this, "Preencha todos os campos corretamente.", Toast.LENGTH_SHORT).show()
            return
        }

        val qtdCarro = qtdTotal - qtdIdosoPcd - qtdMoto
        if (qtdCarro < 0) {
            Toast.makeText(this, "A soma dos tipos de vaga excede o total informado.", Toast.LENGTH_SHORT).show()
            return
        }

        val vagasGeradas = mutableListOf<Map<String, Any>>()
        val letras = ('A'..'Z').toList()

        // 🔹 Passo 1: buscar vagas já existentes no piso
        db.collection("vaga")
            .whereEqualTo("estacionamentoId", estacionamentoId)
            .whereEqualTo("localizacao", "Piso $piso")
            .get()
            .addOnSuccessListener { snapshot ->
                val letrasUsadas = snapshot.documents.mapNotNull { doc ->
                    val numero = doc.getString("numero") ?: ""
                    numero.firstOrNull()?.uppercaseChar()
                }.toSet()

                var letraIndex = 0

                fun proximaLetra(): Char {
                    while (letraIndex < letras.size) {
                        val letra = letras[letraIndex]
                        letraIndex++
                        if (!letrasUsadas.contains(letra)) return letra
                    }
                    throw Exception("Não há mais letras disponíveis para este piso")
                }

                fun gerarVagas(qtd: Int, tipo: String, preferencial: Boolean) {
                    repeat(qtd) {
                        val letra = proximaLetra()
                        val numeroVaga = "$letra$piso"
                        vagasGeradas.add(
                            mapOf(
                                "estacionamentoId" to estacionamentoId,
                                "numero" to numeroVaga,
                                "localizacao" to "Piso $piso",
                                "preco" to precoHora,
                                "tipo" to tipo,         // carro ou moto
                                "preferencial" to preferencial,
                                "disponivel" to true
                            )
                        )
                    }
                }

                gerarVagas(qtdIdosoPcd, "carro", true)
                gerarVagas(qtdMoto, "moto", false)
                gerarVagas(qtdCarro, "carro", false)

                // 🔹 Passo 3: salvar no Firestore em batch
                val batch = db.batch()
                vagasGeradas.forEach { vaga ->
                    val docRef = db.collection("vaga").document()
                    batch.set(docRef, vaga)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Vagas cadastradas com sucesso!", Toast.LENGTH_LONG).show()
                        limparCampos()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro ao cadastrar vagas: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao verificar vagas existentes.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun limparCampos() {
        binding.edtPiso.text?.clear()
        binding.edtQtdTotalVagas.text?.clear()
        binding.edtQtdIdosoPcd.text?.clear()
        binding.edtQtdMoto.text?.clear()
        binding.edtPrecoHora.text?.clear()
    }

}
