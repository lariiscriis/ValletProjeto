    package br.edu.fatecpg.valletprojeto

    import Estacionamento
    import android.Manifest
    import android.content.pm.PackageManager
    import android.os.Build
    import android.os.Bundle
    import android.widget.Toast
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.content.ContextCompat
    import androidx.lifecycle.ViewModelProvider
    import br.edu.fatecpg.valletprojeto.databinding.ActivityReservaBinding
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

            solicitarPermissaoNotificacao()
            setupGifAnimation()


            viewModel = ViewModelProvider(this).get(ReservaViewModel::class.java)
            val usuarioId = FirebaseAuth.getInstance().currentUser?.uid
            if (usuarioId != null) {
                mostrarReservaAtual(usuarioId)
            }

            val vagaId = intent.getStringExtra("VAGA_ID") ?: ""

            val estacionamentoId = intent.getStringExtra("ESTACIONAMENTO_ID") ?: ""

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

            val fromNotification = intent.getBooleanExtra("FROM_NOTIFICATION", false)
            val usuarioI = FirebaseAuth.getInstance().currentUser?.uid

            if (fromNotification && usuarioI != null) {
                mostrarReservaAtual(usuarioI)
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

            binding.btnReservar.setOnClickListener {
                viewModel.iniciarReserva(vagaId, estacionamentoId, tempoMaxReservaHoras, this)
            }

            binding.btnCancelar.setOnClickListener {
                viewModel.cancelarReserva(this)
            }
        }

        private fun setupGifAnimation() {
            Glide.with(this)
                .asGif()
                .load(R.drawable.reserva_carro)
                .into(binding.gifCarro)
        }
        private fun buscarEstacionamento(estacionamentoId: String, onResult: (Estacionamento?) -> Unit) {
            val db = FirebaseFirestore.getInstance()
            db.collection("estacionamentos").document(estacionamentoId).get()
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
            db.collection("reservas")
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("status", "ativa")
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val reserva = documents.first().toObject(Reserva::class.java)
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

                        val inicioTimestamp = reserva.inicioReserva
                        val fimTimestamp = reserva.fimReserva

                        if (inicioTimestamp != null && fimTimestamp != null) {
                            val inicio = sdf.format(inicioTimestamp.toDate())
                            val fim = sdf.format(fimTimestamp.toDate())
                            binding.tvHorarioReserva.text = "Horário: das $inicio às $fim"
                        } else {
                            binding.tvHorarioReserva.text = "Horário: dados incompletos"
                        }
                    } else {
                        binding.tvHorarioReserva.text = "Nenhuma reserva ativa encontrada"
                    }
                }
                .addOnFailureListener {
                    binding.tvHorarioReserva.text = "Horário: erro ao carregar"
                }
        }



        private fun solicitarPermissaoNotificacao() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // Permissão ok, não faz nada
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
