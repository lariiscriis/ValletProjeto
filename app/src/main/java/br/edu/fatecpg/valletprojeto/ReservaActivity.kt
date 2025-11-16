package br.edu.fatecpg.valletprojeto

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.edu.fatecpg.valletprojeto.databinding.ActivityReservaBinding
import br.edu.fatecpg.valletprojeto.model.Reserva
import br.edu.fatecpg.valletprojeto.model.Vaga
import br.edu.fatecpg.valletprojeto.model.Veiculo
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaUIState
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaViewModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ReservaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservaBinding
    private lateinit var viewModel: ReservaViewModel
    private var vagaId: String? = null
    private var estacionamentoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vagaId = intent.getStringExtra("vagaId")
        estacionamentoId = intent.getStringExtra("estacionamentoId")

        if (vagaId == null || estacionamentoId == null) {
            Toast.makeText(this, "Dados da vaga incompletos.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        viewModel = ViewModelProvider(this)[ReservaViewModel::class.java]

        setupGifAnimation()
        setupListeners()
        setupObservers()

        viewModel.carregarDadosIniciais(vagaId!!)
    }

    private fun setupGifAnimation() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.reserva_carro)
            .into(binding.gifCarro)
    }

    private fun setupListeners() {
        binding.sliderTempo.addOnChangeListener { _, value, _ ->
            val horas = value.toInt()
            binding.tvTempoSelecionado.text = String.format("%02d:00", horas)
        }
        binding.btnReservar.setOnClickListener {
            val horasSelecionadas = binding.sliderTempo.value.toInt()
            viewModel.criarReserva(vagaId!!, estacionamentoId!!, horasSelecionadas)
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility = View.GONE
            binding.layoutConfigReserva.visibility = View.GONE
            binding.layoutTimerReserva.visibility = View.GONE
            binding.btnReservar.visibility = View.GONE
            binding.btnRenovar.visibility = View.GONE
            binding.btnCancelar.visibility = View.GONE

            when (state) {
                is ReservaUIState.Initial, is ReservaUIState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is ReservaUIState.Idle -> {
                    binding.layoutConfigReserva.visibility = View.VISIBLE
                    binding.btnReservar.visibility = View.VISIBLE

                    binding.tvVagaNumero.text = "Vaga ${state.vaga.numero}"
                    binding.tvVagaDetalhes.text = "Tipo: ${state.vaga.tipo} • Local: ${state.vaga.localizacao}"
                    binding.tvVeiculoInfo.text = "Veículo: ${state.veiculo.modelo} (${state.veiculo.placa})"
                }

                is ReservaUIState.Active -> {
                    binding.layoutTimerReserva.visibility = View.VISIBLE
                    binding.btnRenovar.visibility = View.VISIBLE
                    binding.btnCancelar.visibility = View.VISIBLE

                    binding.tvVagaNumero.text = "Vaga ${state.vaga.numero}"
                    binding.tvVagaDetalhes.text = "Tipo: ${state.vaga.tipo} • Local: ${state.vaga.localizacao}"
                    binding.tvVeiculoInfo.text = "Veículo: ${state.veiculo.modelo} (${state.veiculo.placa})"

                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val inicioStr = sdf.format(state.reserva.inicioReserva!!.toDate())
                    val fimStr = sdf.format(state.reserva.fimReserva!!.toDate())
                    binding.tvHorarioReserva.text = "Reserva das $inicioStr às $fimStr"

                    binding.btnRenovar.setOnClickListener { viewModel.renovarReserva(state.reserva) }
                    binding.btnCancelar.setOnClickListener { viewModel.cancelarReserva(state.vaga) }
                }

                is ReservaUIState.Finished -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    finish()
                }

                is ReservaUIState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        viewModel.tempoRestante.observe(this) { tempo ->
            binding.tvTimer.text = tempo
        }
    }


    private fun updateUiForIdle() {
        binding.layoutConfigReserva.visibility = View.VISIBLE
        binding.layoutTimerReserva.visibility = View.GONE
        binding.btnReservar.visibility = View.VISIBLE
        binding.btnRenovar.visibility = View.GONE
        binding.btnCancelar.visibility = View.GONE
    }

    private fun updateUiForActive(reserva: Reserva, vaga: Vaga, veiculo: Veiculo) {
        binding.layoutConfigReserva.visibility = View.GONE
        binding.layoutTimerReserva.visibility = View.VISIBLE
        binding.btnReservar.visibility = View.GONE
        binding.btnRenovar.visibility = View.VISIBLE
        binding.btnCancelar.visibility = View.VISIBLE

        binding.tvVagaNumero.text = "Vaga ${vaga.numero}"
        binding.tvVagaDetalhes.text = "Tipo: ${vaga.tipo} • Local: ${vaga.localizacao}"
        binding.tvVeiculoInfo.text = "Veículo: ${veiculo.modelo} (${veiculo.placa})"

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val inicioStr = sdf.format(reserva.inicioReserva!!.toDate())
        val fimStr = sdf.format(reserva.fimReserva!!.toDate())
        binding.tvHorarioReserva.text = "Reserva das $inicioStr às $fimStr"

        binding.btnRenovar.setOnClickListener { viewModel.renovarReserva(reserva) }
        binding.btnCancelar.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (currentState is ReservaUIState.Active) {
                viewModel.cancelarReserva(currentState.vaga)
            } else {
                Toast.makeText(this, "Não há reserva ativa para cancelar.", Toast.LENGTH_SHORT).show()
            }
        }    }
}
