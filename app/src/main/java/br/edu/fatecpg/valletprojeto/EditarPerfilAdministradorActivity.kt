package br.edu.fatecpg.valletprojeto

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityEditarPerfilAdministradorBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilAdministradorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilAdministradorBinding
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado. Faça login novamente.", Toast.LENGTH_LONG).show()
            finish() // Fecha a activity
            return
        }

        binding = ActivityEditarPerfilAdministradorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carregarDadosAdmin()

        binding.btnSalvarPerfilAdmin.setOnClickListener {
            salvarPerfilAdmin()
        }
    }


    private fun carregarDadosAdmin() {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email == null) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        db.collection("usuario")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]

                    binding.edtNomeAdmin.setText(doc.getString("nome") ?: "")
                    binding.edtEmailAdmin.setText(doc.getString("email") ?: "")
                    binding.edtTelefoneAdmin.setText(doc.getString("telefone") ?: "")

                    val nomeEmpresa = doc.getString("nome_empresa")

                    if (!nomeEmpresa.isNullOrEmpty()) {
                        db.collection("estacionamento")
                            .get()
                            .addOnSuccessListener { estSnapshot ->
                                binding.progressBar.visibility = View.GONE
                                val estDoc = estSnapshot.documents.find {
                                    it.getString("nome")?.trim()?.lowercase() == nomeEmpresa.trim().lowercase()
                                }

                                if (estDoc != null) {
                                    binding.edtNomeEstacionamento.setText(estDoc.getString("nome") ?: "")
                                    binding.edtEnderecoEstacionamento.setText(estDoc.getString("endereco") ?: "")
                                    binding.edtVagasEstacionamento.setText(estDoc.getLong("quantidadeVagasTotal")?.toString() ?: "")
                                    binding.edtHorarioEstacionamento.setText(estDoc.getString("horarioFuncionamento") ?: "")
                                } else {
                                    Toast.makeText(this, "Estacionamento não encontrado (filtro manual).", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Erro ao carregar estacionamento.", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Nome da empresa não informado no perfil.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Dados do perfil não encontrados.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar dados do usuário.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarPerfilAdmin() {
        val nome = binding.edtNomeAdmin.text.toString().trim()
        val email = binding.edtEmailAdmin.text.toString().trim()
        val telefone = binding.edtTelefoneAdmin.text.toString().trim()
        val nomeEstacionamento = binding.edtNomeEstacionamento.text.toString().trim()
        val enderecoEstacionamento = binding.edtEnderecoEstacionamento.text.toString().trim()
        val vagas = binding.edtVagasEstacionamento.text.toString().trim().toLongOrNull() ?: 0L
        val horario = binding.edtHorarioEstacionamento.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nome e email são obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        userId?.let { id ->
            // 1. Atualiza o perfil do administrador na coleção "usuario"
            val dadosAdmin = mapOf(
                "nome" to nome,
                "email" to email,
                "telefone" to telefone
            )

            db.collection("usuario").document(id)
                .set(dadosAdmin)
                .addOnSuccessListener {
                    // 2. Depois de atualizar o perfil, atualiza o estacionamento
                    db.collection("estacionamento")
                        .whereEqualTo("nome", nomeEstacionamento)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val estDocId = querySnapshot.documents[0].id
                                val dadosEstacionamento = mapOf(
                                    "nome" to nomeEstacionamento,
                                    "endereco" to enderecoEstacionamento,
                                    "quantidadeVagasTotal" to vagas,
                                    "horarioFuncionamento" to horario
                                )

                                db.collection("estacionamento").document(estDocId)
                                    .update(dadosEstacionamento)
                                    .addOnSuccessListener {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(this, "Perfil atualizado com sucesso.", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(this, "Erro ao atualizar estacionamento.", Toast.LENGTH_SHORT).show()
                                    }

                            } else {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Estacionamento não encontrado.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this, "Erro ao buscar estacionamento.", Toast.LENGTH_SHORT).show()
                        }

                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Erro ao atualizar perfil do administrador.", Toast.LENGTH_SHORT).show()
                }

        } ?: run {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
        }
    }

}
