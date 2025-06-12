package br.edu.fatecpg.valletprojeto

import android.app.Activity
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

        // Recebe o ID da vaga que será editada
        vagaId = intent.getStringExtra("vagaId")

        binding.btnSalvar.setOnClickListener {
            salvarOuAtualizarVaga()
        }
    }

    private fun salvarOuAtualizarVaga() {
        // Valida todos os campos primeiro
        if (binding.edtNumero.text.isNullOrEmpty() ||
            binding.edtLocalizacao.text.isNullOrEmpty() ||
            binding.edtPreco.text.isNullOrEmpty() ||
            binding.radioGroup.checkedRadioButtonId == -1
        ) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val preco = binding.edtPreco.text.toString().toDoubleOrNull()
        if (preco == null) {
            Toast.makeText(this, "Preço inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val vaga = hashMapOf(
            "numero" to binding.edtNumero.text.toString(),
            "localizacao" to binding.edtLocalizacao.text.toString(),
            "preco" to preco,
            "tipo" to findViewById<RadioButton>(binding.radioGroup.checkedRadioButtonId).text.toString(),
            "disponivel" to binding.switchDisponivel.isChecked
        )

        if (vagaId == null) {
            // Cria nova vaga
            db.collection("vaga")
                .add(vaga)
                .addOnSuccessListener {
                    Toast.makeText(this, "Vaga criada com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao criar vaga: ${it.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            // Atualiza vaga existente
            db.collection("vaga").document(vagaId!!)
                .update(vaga as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Vaga atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Erro ao atualizar vaga: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}