package br.edu.fatecpg.valletprojeto


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import br.edu.fatecpg.valletprojeto.databinding.ActivityPerfilMotoristaBinding
import java.text.SimpleDateFormat
import java.util.Locale

class PerfilMotoristaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilMotoristaBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilMotoristaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carregarDadosUsuario()

        binding.btnEditarPerfil.setOnClickListener {
            startActivity(Intent(this, EditarPerfilMotoristaActivity::class.java))
        }

    }

    fun isoStringToMillis(isoDate: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(isoDate)
        return date?.time ?: 0L
    }

    private fun carregarDadosUsuario() {
        val email = auth.currentUser?.email
        if (email == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("usuario")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]

                    val nome = doc.getString("nome") ?: ""
                    val telefone = doc.getString("telefone") ?: ""
                    val cnh = doc.getString("cnh") ?: ""
                    val tipoUser = doc.getString("tipo_user") ?: ""
                    val fotoPerfilUrl = doc.getString("fotoPerfil")

                    binding.txtNome.text = "Nome: $nome"
                    binding.txtEmail.text = "Email: $email"
                    binding.txtTelefone.text = "Telefone: $telefone"
                    binding.txtCnh.text = "CNH: $cnh"
                    binding.txtTipoConta.text = "Tipo de conta: ${tipoUser.capitalize()}"

                    binding.txtHistoricoReservas.text = doc.getString("historicoReservas") ?: "Nenhum histórico"
                    binding.txtTotalReservas.text = "Total de reservas feitas: ${doc.getLong("totalReservas") ?: 0}"
                    binding.txtTempoTotalUso.text = "Tempo total de uso: ${doc.getString("tempoTotalUso") ?: "0h"}"
                    binding.txtLocaisMaisFrequentados.text = "Locais mais frequentes: ${doc.getString("locaisMaisFrequentados") ?: "Nenhum"}"

                    if (!fotoPerfilUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(fotoPerfilUrl)
                            .circleCrop()
                            .into(binding.imgFotoPerfil)
                    }

                } else {
                    Toast.makeText(this, "Dados do usuário não encontrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        val userId = auth.currentUser?.uid ?: return

        db.collection("reserva")
            .whereEqualTo("usuarioId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->

                val reservas = querySnapshot.documents

                // Total de reservas
                val totalReservas = reservas.size

                // Construir histórico texto
                val historicoReservas = reservas.joinToString("\n\n") { doc ->
                    val estacionamento = doc.getString("estacionamentoNome") ?: "Desconhecido"
                    val horarioEntrada = doc.getString("horarioEntrada") ?: "-"
                    val horarioSaida = doc.getString("horarioSaida") ?: "-"
                    "Estacionamento: $estacionamento\nEntrada: $horarioEntrada\nSaída: $horarioSaida"
                }

                // Calcular tempo total de uso (assumindo horários em String formato ISO ou timestamp)
                var tempoTotalMillis = 0L
                reservas.forEach { doc ->
                    val entradaStr = doc.getString("horarioEntrada")
                    val saidaStr = doc.getString("horarioSaida")
                    if (entradaStr != null && saidaStr != null) {
                        try {
                            val entrada = isoStringToMillis(entradaStr)
                            val saida = isoStringToMillis(saidaStr)
                            if (saida > entrada) {
                                tempoTotalMillis += (saida - entrada)
                            }
                        } catch (e: Exception) {
                            // ignorar erro de parsing
                        }
                    }
                }
                val tempoTotalHoras = tempoTotalMillis / (1000 * 60 * 60) // converter de ms para horas

                // Calcular locais mais frequentados
                val frequenciaLocais = mutableMapOf<String, Int>()
                reservas.forEach { doc ->
                    val estacionamento = doc.getString("estacionamentoNome") ?: "Desconhecido"
                    frequenciaLocais[estacionamento] = (frequenciaLocais[estacionamento] ?: 0) + 1
                }
                // Pega os 3 mais frequentes
                val locaisMaisFrequentados = frequenciaLocais.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .joinToString(", ") { "${it.key} (${it.value})" }

                // Atualizar a UI
                binding.txtHistoricoReservas.text = historicoReservas.ifEmpty { "Nenhum histórico" }
                binding.txtTotalReservas.text = "Total de reservas feitas: $totalReservas"
                binding.txtTempoTotalUso.text = "Tempo total de uso: ${tempoTotalHoras}h"
                binding.txtLocaisMaisFrequentados.text = if (locaisMaisFrequentados.isNotEmpty()) {
                    "Locais mais frequentes: $locaisMaisFrequentados"
                } else {
                    "Locais mais frequentes: Nenhum"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar reservas: ${it.message}", Toast.LENGTH_SHORT).show()
            }

    }
}
