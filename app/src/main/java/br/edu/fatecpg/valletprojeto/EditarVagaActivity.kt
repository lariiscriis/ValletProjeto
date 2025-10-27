package br.edu.fatecpg.valletprojeto

import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityEditarVagaBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditarVagaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarVagaBinding
    private val db = FirebaseFirestore.getInstance()
    private var vagaId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarVagaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vagaId = intent.getStringExtra("vagaId")

        if (vagaId != null) {
            carregarDadosDaVaga(vagaId!!)
        }

        binding.btnSalvar.setOnClickListener {
            salvarOuAtualizarVaga()
        }
    }

    private fun carregarDadosDaVaga(id: String) {
        db.collection("vaga").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    binding.edtNumero.setText(document.getString("numero"))
                    binding.edtLocalizacao.setText(document.getString("localizacao"))
                    binding.edtPreco.setText(document.getDouble("preco")?.toString())
                    binding.edtPiso.setText(document.getString("piso"))

                    when (document.getString("tipoVeiculo")) {
                        "Carro" -> binding.radioTipoVeiculo.check(binding.radioCarro.id)
                        "Moto" -> binding.radioTipoVeiculo.check(binding.radioMoto.id)
                    }
                    binding.switchPreferencial.isChecked = document.getBoolean("preferencial") ?: false
                    binding.switchDisponivel.isChecked = document.getBoolean("disponivel") ?: true
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar vaga: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarOuAtualizarVaga() {
        val numero = binding.edtNumero.text.toString().trim()
        val localizacao = binding.edtLocalizacao.text.toString().trim()
        val piso = binding.edtPiso.text.toString().trim()
        val preco = binding.edtPreco.text.toString().toDoubleOrNull()
        val tipoVeiculoId = binding.radioTipoVeiculo.checkedRadioButtonId
        val preferencial = binding.switchPreferencial.isChecked
        val disponivel = binding.switchDisponivel.isChecked

        if (numero.isEmpty() || localizacao.isEmpty() || piso.isEmpty() || preco == null || tipoVeiculoId == -1) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val tipoVeiculo = findViewById<RadioButton>(tipoVeiculoId).text.toString()

        db.collection("vaga")
            .whereEqualTo("numero", numero)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val existeDuplicado = querySnapshot.documents.any { it.id != vagaId }
                if (existeDuplicado) {
                    Toast.makeText(this, "Número da vaga já existe!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val vaga = hashMapOf(
                    "numero" to numero,
                    "localizacao" to localizacao,
                    "piso" to piso,
                    "preco" to preco,
                    "tipo" to tipoVeiculo,
                    "preferencial" to preferencial,
                    "disponivel" to disponivel
                )

                if (vagaId == null) {
                    // Criar nova vaga
                    db.collection("vaga")
                        .add(vaga)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Vaga criada com sucesso!", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao criar vaga: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Atualizar vaga existente
                    db.collection("vaga").document(vagaId!!)
                        .update(vaga as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Vaga atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao atualizar vaga: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao verificar duplicidade: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
