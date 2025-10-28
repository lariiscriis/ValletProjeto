package br.edu.fatecpg.valletprojeto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import br.edu.fatecpg.valletprojeto.databinding.ActivityReservaBinding
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import br.edu.fatecpg.valletprojeto.model.Reserva
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaState
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaViewModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ReservaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservaBinding
    private lateinit var viewModel: ReservaViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Permissão para notificações negada. Ative nas configurações do app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vagaId = intent.getStringExtra("vagaId") ?: ""
        val estacionamentoId = intent.getStringExtra("estacionamentoId") ?: ""
        val fromNotification = intent.getBooleanExtra("FROM_NOTIFICATION", false)
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid

        solicitarPermissaoNotificacao()
        setupGifAnimation()

        viewModel = ViewModelProvider(this)[ReservaViewModel::class.java]

        // ✅ Retomar reserva ao abrir via notificação
        if (usuarioId != null) {
            if (fromNotification) {
                FirebaseFirestore.getInstance()
                    .collection("reserva")
                    .whereEqualTo("usuarioId", usuarioId)
                    .whereEqualTo("status", "ativa")
                    .get()
                    .addOnSuccessListener { docs ->
                        if (!docs.isEmpty) {
                            val reserva = docs.first().toObject(Reserva::class.java)
                            reserva.fimReserva?.toDate()?.let { fim ->
                                viewModel.retomarReservaAtiva(fim, reserva.vagaId, reserva.estacionamentoId, this)
                            }
                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            reserva.inicioReserva?.let { inicio ->
                                reserva.fimReserva?.let { fimRes ->
                                    val inicioStr = sdf.format(inicio.toDate())
                                    val fimStr = sdf.format(fimRes.toDate())
                                    binding.tvHorarioReserva.text = "Horário: das $inicioStr às $fimStr"
                                }
                            }
                            binding.btnReservar.visibility = android.view.View.GONE
                            binding.btnCancelar.visibility = android.view.View.VISIBLE
                        }
                    }
            } else {
                mostrarReservaAtual(usuarioId)
            }
        }

        if (vagaId.isBlank() || estacionamentoId.isBlank()) {
            Toast.makeText(this, "Erro ao abrir reserva: dados incompletos", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        var tempoMaxReservaHoras = 2
        buscarEstacionamento(estacionamentoId) { estacionamento ->
            if (estacionamento != null) {
                binding.tvTempoMaximo.text = "Tempo máximo: ${estacionamento.tempoMaxReservaHoras} hora(s)"
                tempoMaxReservaHoras = estacionamento.tempoMaxReservaHoras
            } else {
                binding.tvTempoMaximo.text = "Tempo máximo: 2 horas"
            }
        }

        binding.tvVagaId.text = "Vaga: $vagaId"

        // Observadores LiveData
        viewModel.tempoRestante.observe(this) {
            binding.tvTimer.text = it
        }

        viewModel.reservaStatus.observe(this) { state ->
            when (state) {
                is ReservaState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.btnReservar.isEnabled = false
                }
                is ReservaState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnReservar.visibility = android.view.View.GONE
                    binding.btnCancelar.visibility = android.view.View.VISIBLE
                    Toast.makeText(this, "Reserva feita com sucesso!", Toast.LENGTH_SHORT).show()
                }
                is ReservaState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnReservar.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Botão reservar
        binding.btnReservar.setOnClickListener {
            viewModel.iniciarReserva(vagaId, estacionamentoId, tempoMaxReservaHoras, this)
        }

        // Botão cancelar
        binding.btnCancelar.setOnClickListener {
            viewModel.cancelarReserva(this, vagaId, estacionamentoId)
        }
    }

    // Mostra GIF animado do carro
    private fun setupGifAnimation() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.reserva_carro)
            .into(binding.gifCarro)
    }

    private fun buscarEstacionamento(estacionamentoId: String, onResult: (Estacionamento?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("estacionamento").document(estacionamentoId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val estacionamento = doc.toObject(Estacionamento::class.java)
                    onResult(estacionamento)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun mostrarReservaAtual(usuarioId: String) {
        val db = FirebaseFirestore.getInstance()
        val vagaAtual = intent.getStringExtra("vagaId")
        val estacionamentoAtual = intent.getStringExtra("estacionamentoId")

        db.collection("reserva")
            .whereEqualTo("usuarioId", usuarioId)
            .whereEqualTo("status", "ativa")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.first()
                    val reserva = doc.toObject(Reserva::class.java)

                    // ✅ Já está na reserva ativa → não mostrar alerta
                    if (reserva.vagaId == vagaAtual && reserva.estacionamentoId == estacionamentoAtual) {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        reserva.inicioReserva?.let { inicio ->
                            reserva.fimReserva?.let { fim ->
                                val inicioStr = sdf.format(inicio.toDate())
                                val fimStr = sdf.format(fim.toDate())
                                binding.tvHorarioReserva.text = "Horário: das $inicioStr às $fimStr"
                            }
                        }
                        binding.btnReservar.visibility = android.view.View.GONE
                        binding.btnCancelar.visibility = android.view.View.VISIBLE

                        // 🧩 Retoma contagem da reserva ativa
                        reserva.fimReserva?.toDate()?.let { fim ->
                            viewModel.retomarReservaAtiva(fim, reserva.vagaId, reserva.estacionamentoId, this)
                        }

                        return@addOnSuccessListener
                    }

                    // ⚠️ Caso contrário, mostra alerta
                    AlertDialog.Builder(this)
                        .setTitle("Reserva ativa encontrada")
                        .setMessage("Você já possui uma reserva em andamento. Deseja visualizá-la agora?")
                        .setPositiveButton("Sim") { _, _ ->
                            val intent = Intent(this, ReservaActivity::class.java).apply {
                                putExtra("vagaId", reserva.vagaId)
                                putExtra("estacionamentoId", reserva.estacionamentoId)
                                putExtra("FROM_NOTIFICATION", true)
                            }
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Não") { _, _ ->
                            val intent = Intent(this, DashboardBase::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                        .setCancelable(false)
                        .show()

                    // Esconde botões até o usuário decidir
                    binding.btnReservar.visibility = android.view.View.GONE
                    binding.btnCancelar.visibility = android.view.View.GONE

                } else {
                    // Nenhuma reserva ativa
                    binding.tvHorarioReserva.text = "Nenhuma reserva ativa encontrada"
                    binding.btnReservar.visibility = android.view.View.VISIBLE
                    binding.btnCancelar.visibility = android.view.View.GONE
                    binding.btnReservar.isEnabled = true
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao verificar reservas", Toast.LENGTH_LONG).show()
            }
    }

    private fun solicitarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permissão ok
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        this,
                        "Permissão para notificações é necessária para avisos da reserva",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
