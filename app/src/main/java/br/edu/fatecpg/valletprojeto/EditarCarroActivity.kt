package br.edu.fatecpg.valletprojeto

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityEditarCarroBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditarCarroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarCarroBinding
    private val db = FirebaseFirestore.getInstance()
    private var carroId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarCarroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carroId = intent.getStringExtra("carroId")
        val placa = intent.getStringExtra("placa")
        val marca = intent.getStringExtra("marca")
        val modelo = intent.getStringExtra("modelo")
        val ano = intent.getStringExtra("ano")
        val km = intent.getStringExtra("km")

        binding.editPlaca.setText(placa)
        binding.editMarca.setText(marca)
        binding.editModelo.setText(modelo)
        binding.editAno.setText(ano)
        binding.editKM.setText(km)

        binding.btnConfirmar.setOnClickListener {
            atualizarCarro()
        }
    }

    private fun atualizarCarro() {
        val novaPlaca = binding.editPlaca.text.toString()
        val novaMarca = binding.editMarca.text.toString()
        val novoModelo = binding.editModelo.text.toString()
        val novoAno = binding.editAno.text.toString()
        val novoKM = binding.editKM.text.toString()

        if (carroId != null) {
            val carroAtualizado = hashMapOf(
                "placa" to novaPlaca,
                "marca" to novaMarca,
                "modelo" to novoModelo,
                "ano" to novoAno,
                "km" to novoKM
            )

            db.collection("carro").document(carroId!!)
                .update(carroAtualizado as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Carro atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK) // <- notifica que deu certo
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao atualizar carro", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "ID do carro não encontrado", Toast.LENGTH_SHORT).show()
        }
    }
}
