package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityPerfilAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilAdminBinding
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carregarDadosAdmin()

        binding.btnEditarPerfilAdmin.setOnClickListener {
            startActivity(Intent(this, EditarPerfilAdministradorActivity::class.java))
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
                binding.progressBar.visibility = View.GONE
                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents[0]

                    binding.tvNomeAdmin.text = "Nome: ${userDoc.getString("nome") ?: ""}"
                    binding.tvEmailAdmin.text = "Email: ${userDoc.getString("email") ?: ""}"
                    binding.tvTelefoneAdmin.text = "Telefone: ${userDoc.getString("telefone") ?: "Não informado"}"

                    val nomeEmpresa = userDoc.getString("nome_empresa")
                    if (!nomeEmpresa.isNullOrEmpty()) {
                        binding.progressBar.visibility = View.VISIBLE
                        db.collection("estacionamento")
                            .get()
                            .addOnSuccessListener { estSnapshot ->
                                val estDoc = estSnapshot.documents.find {
                                    it.getString("nome")?.trim()?.lowercase() == nomeEmpresa.trim().lowercase()
                                }

                                binding.progressBar.visibility = View.GONE  // ✅ Adicionado aqui

                                if (estDoc != null) {
                                    binding.tvNomeEstacionamento.text = estDoc.getString("nome") ?: "Nome do estacionamento"
                                    binding.tvVagasDisponiveis.text = "Vagas disponíveis: ${estDoc.getLong("quantidadeVagasTotal") ?: 0}"
                                    binding.tvTelefoneEstacionamento.text = "Telefone: ${estDoc.getString("telefone") ?: "Não informado"}"
                                    binding.tvHorarioFuncionamento.text = "Funcionamento: ${estDoc.getString("horarioFuncionamento") ?: ""}"
                                    binding.tvValorHora.text = "Valor hora: R$ ${estDoc.getDouble("valorHora") ?: 0.0}"
                                } else {
                                    Toast.makeText(this, "Estacionamento não encontrado (filtro manual).", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Erro ao carregar dados do estacionamento.", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        Toast.makeText(this, "Nome da empresa não informado no perfil.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Perfil de usuário não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar perfil de usuário.", Toast.LENGTH_SHORT).show()
            }
    }


}
