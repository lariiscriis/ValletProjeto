package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityPerfilAdminBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilAdminBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carregarDadosAdmin()

        binding.btnEditarEstacionamento.setOnClickListener {
            startActivity(Intent(this, EditarPerfilAdministradorActivity::class.java))
        }
    }

    private fun carregarDadosAdmin() {
        val email = auth.currentUser?.email
        if (email == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        db.collection("usuario")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userDoc = snapshot.documents.first()
                val nomeAdmin = userDoc.getString("nome") ?: ""
                val nomeEmpresa = userDoc.getString("nome_empresa")
                val fotoUrl = userDoc.getString("fotoPerfil")

                binding.tvNomeAdmin.text = "Nome: $nomeAdmin"
                binding.tvEmailAdmin.text = "Email: $email"

// 🔹 Carregar foto do perfil
                if (!fotoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(fotoUrl)
                        .placeholder(R.drawable.estacionamento_foto)
                        .error(R.drawable.estacionamento_foto)
                        .circleCrop()
                        .into(binding.imgFotoEstacionamento)
                } else {
                    binding.imgFotoEstacionamento.setImageResource(R.drawable.estacionamento_foto)
                }

                binding.tvNomeAdmin.text = "Nome: $nomeAdmin"
                binding.tvEmailAdmin.text = "Email: $email"

                if (nomeEmpresa.isNullOrEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Administrador não possui estacionamento vinculado.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                db.collection("estacionamento")
                    .whereEqualTo("adminUid", auth.currentUser?.uid)
                    .get()
                    .addOnSuccessListener { estSnap ->
                        binding.progressBar.visibility = View.GONE
                        if (estSnap.isEmpty) {
                            Toast.makeText(this, "Estacionamento não encontrado.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val est = estSnap.documents.first()

                        binding.tvNomeEstacionamento.text = "Nome: ${est.getString("nome") ?: "-"}"
                        binding.tvCnpj.text = "CNPJ: ${est.getString("cnpj") ?: "-"}"
                        binding.tvTelefoneEstacionamento.text = "Telefone: ${est.getString("telefone") ?: "-"}"
                        binding.tvCep.text = "CEP: ${est.getString("cep") ?: "-"}"
                        binding.tvEndereco.text = "Endereço: ${est.getString("endereco") ?: "-"}"

                        binding.tvVagasTotal.text = "Vagas totais: ${est.getLong("quantidadeVagasTotal") ?: 0}"
                        binding.tvVagasComum.text = "Vagas comuns: ${est.getLong("quantidadeVagasComum") ?: 0}"
                        binding.tvVagasPcd.text = "Vagas PCD/Idoso: ${est.getLong("quantidadeVagasIdosoPcd") ?: 0}"
                        binding.tvCobertura.text = "Possui cobertura: ${if (est.getBoolean("possuiCobertura") == true) "Sim" else "Não"}"
                        binding.tvPavimentos.text = "Número de pavimentos: ${est.getLong("numeroPavimentos") ?: 1}"

                        binding.tvHorarioAbertura.text = "Abertura: ${est.getString("horarioAbertura") ?: "-"}"
                        binding.tvHorarioFechamento.text = "Fechamento: ${est.getString("horarioFechamento") ?: "-"}"
                        binding.tvTempoMaxReserva.text = "Tempo máx. reserva: ${est.getLong("tempoMaxReservaHoras") ?: 0}h"
                        binding.tvToleranciaReserva.text = "Tolerância: ${est.getLong("toleranciaReservaMinutos") ?: 0} min"

                        binding.tvValorHora.text = "Valor hora: R$ ${String.format("%.2f", est.getDouble("valorHora") ?: 0.0)}"
                        binding.tvValorDiaria.text = "Valor diário: R$ ${String.format("%.2f", est.getDouble("valorDiario") ?: 0.0)}"

                        binding.tvLatitude.text = "Latitude: ${est.getDouble("latitude") ?: 0.0}"
                        binding.tvLongitude.text = "Longitude: ${est.getDouble("longitude") ?: 0.0}"

                    }
                    .addOnFailureListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Erro ao carregar estacionamento.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao buscar dados do administrador.", Toast.LENGTH_SHORT).show()
            }
    }
}
