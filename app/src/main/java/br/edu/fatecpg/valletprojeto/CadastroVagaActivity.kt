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
        val numeroVaga = binding.edtNumeroVaga.text.toString().trim()
        val localizacao = binding.edtLocalizacao.text.toString().trim()
        val precoHora = binding.edtPrecoHora.text.toString().trim()
        val tipoVaga = when {
            binding.radioCovered.isChecked -> "preferencial"
            binding.radioUncovered.isChecked -> "comum"
            else -> ""
        }

        if (validarCampos(estacionamentoId,numeroVaga, localizacao, precoHora, tipoVaga)) {
            val vaga = hashMapOf(
                "estacionamentoId" to estacionamentoId,
                "numero" to numeroVaga,
                "localizacao" to localizacao,
                "preco" to precoHora.toDouble(),
                "tipo" to tipoVaga,
                "disponivel" to true,
            )

            db.collection("vaga")
                .add(vaga)
                .addOnSuccessListener {
                    Toast.makeText(this, "Vaga cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                    limparCampos()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao cadastrar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun validarCampos(
        estacionamentoId: String,
        numero: String,
        localizacao: String,
        preco: String,
        tipo: String
    ): Boolean {
        return when {
            estacionamentoId.isEmpty() -> {
                Toast.makeText(this, "Estacionamento inválido", Toast.LENGTH_SHORT).show()
                false
            }
            numero.isEmpty() -> {
                binding.edtNumeroVaga.error = "Informe o número da vaga"
                false
            }
            localizacao.isEmpty() -> {
                binding.edtLocalizacao.error = "Informe a localização"
                false
            }
            preco.isEmpty() || preco.toDoubleOrNull() == null -> {
                binding.edtPrecoHora.error = "Preço inválido"
                false
            }
            tipo.isEmpty() -> {
                Toast.makeText(this, "Selecione o tipo da vaga", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }


    private fun limparCampos() {
        binding.edtNumeroVaga.text?.clear()
        binding.edtLocalizacao.text?.clear()
        binding.edtPrecoHora.text?.clear()
        binding.radioGroup.clearCheck()
    }
}
