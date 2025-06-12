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

        vagaId = intent.getStringExtra("vagaId")
        val numero = intent.getStringExtra("numero")
        val localizacao = intent.getStringExtra("localizacao")
        val preco = intent.getStringExtra("preco")
        val tipo = intent.getStringExtra("tipo")
        val disponivel = intent.getStringExtra("disponivel")

        binding.edtNumero.setText(numero)
        binding.edtLocalizacao.setText(localizacao)
        binding.edtPreco.setText(preco)

        for (i in 0 until binding.radioGroup.childCount) {
            val radio = binding.radioGroup.getChildAt(i) as? RadioButton
            if (radio?.text.toString() == tipo) {
                if (radio != null) {
                    radio.isChecked = true
                }
                break
            }
        }

        binding.switchDisponivel.isChecked = disponivel == "true"

        binding.btnSalvar.setOnClickListener {
            atualizarVaga()
        }
    }

    private fun atualizarVaga() {
        val novoNumero = binding.edtNumero.text.toString()
        val novaLocalizacao = binding.edtLocalizacao.text.toString()
        val novoPreco = binding.edtPreco.text.toString()
        val radioButtonId = binding.radioGroup.checkedRadioButtonId
        val radioButton = findViewById<RadioButton>(radioButtonId)
        val novoTipo = radioButton?.text.toString()

        val novoStatus = binding.switchDisponivel.isChecked

        if (vagaId != null) {
            val vagaAtualizada = hashMapOf(
                "numero" to novoNumero,
                "localizacao" to novaLocalizacao,
                "preco" to novoPreco,
                "tipo" to novoTipo,
                "disponivel" to novoStatus
            )

            db.collection("vaga").document(vagaId!!)
                .update(vagaAtualizada as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Vaga atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK) // <- notifica que deu certo
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao atualizar vaga", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "ID da vaga não encontrado", Toast.LENGTH_SHORT).show()
        }
    }
}
