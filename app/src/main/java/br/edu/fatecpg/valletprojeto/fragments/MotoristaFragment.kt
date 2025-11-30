package br.edu.fatecpg.valletprojeto.fragments

import br.edu.fatecpg.valletprojeto.model.ReservaHistorico
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.ReservaActivity
import br.edu.fatecpg.valletprojeto.VagaActivity
import br.edu.fatecpg.valletprojeto.adapter.HistoricoReservasAdapter
import br.edu.fatecpg.valletprojeto.databinding.FragmentMotoristaDashboardBinding
import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MotoristaFragment : Fragment() {

    private var _binding: FragmentMotoristaDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoricoReservasAdapter
    private val db = FirebaseFirestore.getInstance()

    private var vagaIdAtiva: String? = null
    private var estacionamentoIdAtivo: String? = null
    private var buscaReservasJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMotoristaDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mostrarLoadingGeral()

        binding.rvReservationHistory.layoutManager = LinearLayoutManager(requireContext())

        binding.btnViewReservation.setOnClickListener {
            if (!isAdded || context == null) return@setOnClickListener

            if (vagaIdAtiva != null && estacionamentoIdAtivo != null) {
                val intent = Intent(requireContext(), ReservaActivity::class.java)
                intent.putExtra("vagaId", vagaIdAtiva)
                intent.putExtra("estacionamentoId", estacionamentoIdAtivo)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Nenhuma reserva ativa encontrada.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.txvNoReservation.setOnClickListener {
            abrirPaginaDeVagas()
        }
    }

    override fun onResume() {
        super.onResume()
        buscarReservasComCoroutines()
    }

    override fun onPause() {
        super.onPause()
        buscaReservasJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        buscaReservasJob?.cancel()
        _binding = null
    }

    private fun mostrarLoadingGeral() {
        binding.loadingState.visibility = View.VISIBLE
        binding.contentState.visibility = View.GONE

        binding.loadingCardReserva.visibility = View.VISIBLE
        binding.contentArea.visibility = View.GONE
        binding.footerArea.visibility = View.GONE
        binding.divider.visibility = View.GONE

        binding.loadingEstatisticas.visibility = View.VISIBLE
        binding.contentEstatisticas.visibility = View.GONE

        binding.loadingHistorico.visibility = View.VISIBLE
        binding.rvReservationHistory.visibility = View.GONE
    }

    private fun esconderLoadingGeral() {
        binding.loadingState.visibility = View.GONE
        binding.contentState.visibility = View.VISIBLE
    }

    private fun mostrarConteudoGradualmente() {
        binding.loadingCardReserva.visibility = View.GONE
        binding.contentArea.visibility = View.VISIBLE
        binding.footerArea.visibility = View.VISIBLE
        binding.divider.visibility = View.VISIBLE

        binding.loadingEstatisticas.visibility = View.GONE
        binding.contentEstatisticas.visibility = View.VISIBLE

        binding.loadingHistorico.visibility = View.GONE
        binding.rvReservationHistory.visibility = View.VISIBLE
    }

    private fun abrirPaginaDeVagas() {
        if (!isAdded || context == null) return

        val intent = Intent(requireContext(), VagaActivity::class.java)
        startActivity(intent)
        if (isAdded) {
            Toast.makeText(requireContext(), "Abrir página de vagas", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun buscarReservasComCoroutines() {
        val uidLogado = FirebaseAuth.getInstance().currentUser?.uid ?: return

        buscaReservasJob?.cancel()

        buscaReservasJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                if (!isAdded || context == null) return@launch

                val reservas = db.collection("reserva").whereEqualTo("usuarioId", uidLogado).get().await()

                if (!isAdded || context == null) return@launch

                var totalHoras = 0L
                val historico = mutableListOf<ReservaHistorico>()
                val totalReservas = reservas.size()
                var temReservaAtiva = false

                for (doc in reservas) {
                    if (!isAdded || context == null) return@launch

                    val status = doc.getString("status")
                    val vagaId = doc.getString("vagaId") ?: continue
                    val estacionamentoId = doc.getString("estacionamentoId") ?: ""
                    val estacionamentoNome = doc.getString("estacionamentoNome") ?: ""
                    val inicio = doc.getTimestamp("inicioReserva")?.toDate()
                    val fim = doc.getTimestamp("fimReserva")?.toDate()

                    if (inicio != null && fim != null) {
                        totalHoras += (fim.time - inicio.time)
                    }

                    val vagaDoc = try {
                        db.collection("vaga").document(vagaId).get().await()
                    } catch (e: Exception) {
                        continue
                    }

                    if (!isAdded || context == null) return@launch

                    val vaga = Vaga(
                        numero = vagaDoc.getString("numero") ?: "-",
                        localizacao = vagaDoc.getString("localizacao") ?: "-"
                    )

                    if (status == "ativa") {
                        temReservaAtiva = true
                        binding.cardReservaAtual.visibility = View.VISIBLE
                        binding.txvNoReservation.visibility = View.GONE

                        binding.txvSpotLetter.text = vaga.numero
                        binding.txvLocation.text = "Local: ${vaga.localizacao}"
                        binding.txvTimeRange.text = formatarHorario(inicio, fim)
                        binding.txvTimeRemaining.text = "Reserva ativa"
                        binding.txvEstacionamento.text = estacionamentoNome

                        vagaIdAtiva = vagaId
                        estacionamentoIdAtivo = estacionamentoId
                    } else {
                        val nomeEstacionamentoFinal = if (estacionamentoNome.isNotEmpty()) {
                            estacionamentoNome
                        } else {
                            buscarNomeEstacionamento(estacionamentoId)
                        }

                        if (inicio != null) {
                            historico.add(
                                ReservaHistorico(
                                    vaga = vaga.numero,
                                    data = SimpleDateFormat("dd/MM", Locale.getDefault()).format(inicio),
                                    horario = formatarHorario(inicio, fim),
                                    estacionamentoNome = nomeEstacionamentoFinal
                                )
                            )
                        }
                    }
                }

                if (isAdded && context != null) {
                    esconderLoadingGeral()
                    mostrarConteudoGradualmente()
                    atualizarUI(totalHoras, historico, temReservaAtiva, totalReservas)
                }

            } catch (e: Exception) {
                if (isAdded && context != null) {
                    Log.e("MotoristaFragment", "Erro ao carregar reservas", e)

                    esconderLoadingGeral()
                    mostrarConteudoGradualmente()

                    Toast.makeText(requireContext(), "Erro ao carregar reservas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun atualizarUI(totalHoras: Long, historico: List<ReservaHistorico>, temReservaAtiva: Boolean, totalReservas: Int) {
        binding.txvTotalReservations.text = totalReservas.toString()
        val horasTotais = TimeUnit.MILLISECONDS.toHours(totalHoras)
        binding.txvTotalHours.text = "${horasTotais}h"

        adapter = HistoricoReservasAdapter(historico)
        binding.rvReservationHistory.adapter = adapter

        if (temReservaAtiva) {
            binding.cardReservaAtual.visibility = View.VISIBLE
            binding.txvNoReservation.visibility = View.GONE
        } else {
            binding.cardReservaAtual.visibility = View.GONE
            binding.txvNoReservation.visibility = View.VISIBLE
            vagaIdAtiva = null
            estacionamentoIdAtivo = null
        }
    }

    private suspend fun buscarNomeEstacionamento(estacionamentoId: String): String {
        if (!isAdded || context == null) return "Estacionamento"

        return try {
            if (estacionamentoId.isEmpty()) return "Estacionamento"

            val estacionamentoDoc = db.collection("estacionamento").document(estacionamentoId).get().await()

            if (!isAdded || context == null) return "Estacionamento"

            estacionamentoDoc.getString("nome") ?: "Estacionamento"
        } catch (e: Exception) {
            "Estacionamento"
        }
    }

    private fun formatarHorario(inicio: Date?, fim: Date?): String {
        val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
        return if (inicio != null && fim != null) "${formato.format(inicio)} - ${formato.format(fim)}" else ""
    }
}
