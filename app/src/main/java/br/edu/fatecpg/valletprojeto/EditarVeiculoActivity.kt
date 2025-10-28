package br.edu.fatecpg.valletprojeto

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityEditarVeiculoBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditarVeiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarVeiculoBinding
    private val db = FirebaseFirestore.getInstance()
    private var veiculoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarVeiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        veiculoId = intent.getStringExtra("veiculoId")
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

        binding.btnConfirmar.setOnClickListener { atualizarVeiculo() }
        binding.btnExcluir.setOnClickListener { confirmarExclusao() }
    }

    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Excluir veículo")
            .setMessage("Tem certeza que deseja excluir este veículo?")
            .setPositiveButton("Excluir") { _, _ -> excluirVeiculo() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirVeiculo() {
        if (veiculoId.isNullOrEmpty()) {
            Toast.makeText(this, "ID do veículo não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("veiculo").document(veiculoId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Veículo excluído com sucesso!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao excluir veículo: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarVeiculo() {
        val novaPlaca = binding.editPlaca.text.toString()
        val novaMarca = binding.editMarca.text.toString()
        val novoModelo = binding.editModelo.text.toString()
        val novoAno = binding.editAno.text.toString()
        val novoKM = binding.editKM.text.toString()

        if (veiculoId.isNullOrEmpty()) {
            Toast.makeText(this, "ID do veículo não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val veiculoAtualizado = mapOf(
            "placa" to novaPlaca,
            "marca" to novaMarca,
            "modelo" to novoModelo,
            "ano" to novoAno,
            "km" to novoKM
        )

        db.collection("veiculo").document(veiculoId!!)
            .update(veiculoAtualizado)
            .addOnSuccessListener {
                Toast.makeText(this, "Veículo atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar veículo: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
