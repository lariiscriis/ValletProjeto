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
import androidx.recyclerview.widget.GridLayoutManager
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
    private var buscaReservasJob: Job? = null // 🔥 CONTROLE DA CORROTINA

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMotoristaDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvReservationHistory.layoutManager = GridLayoutManager(requireContext(), 2)

        binding.btnViewReservation.setOnClickListener {
            // 🔥 VERIFICA SE O FRAGMENT AINDA ESTÁ ATACHADO
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
        // 🔥 BUSCA AS RESERVAS QUANDO O FRAGMENT VOLTA AO FOCO
        buscarReservasComCoroutines()
    }

    override fun onPause() {
        super.onPause()
        // 🔥 CANCELA A CORROTINA QUANDO O FRAGMENT PERDE FOCO
        buscaReservasJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 🔥 CANCELA A CORROTINA E LIMPA O BINDING
        buscaReservasJob?.cancel()
        _binding = null
    }

    private fun abrirPaginaDeVagas() {
        // 🔥 VERIFICA SE O FRAGMENT AINDA ESTÁ ATACHADO
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

        // 🔥 CANCELA QUALQUER BUSCA ANTERIOR
        buscaReservasJob?.cancel()

        // 🔥 INICIA NOVA BUSCA CONTROLADA
        buscaReservasJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                // 🔥 VERIFICA SE O FRAGMENT AINDA ESTÁ ATIVO
                if (!isAdded || context == null) return@launch

                val reservas = db.collection("reserva").whereEqualTo("usuarioId", uidLogado).get().await()

                // 🔥 VERIFICA NOVAMENTE
                if (!isAdded || context == null) return@launch

                var totalHoras = 0L
                val historico = mutableListOf<ReservaHistorico>()
                val totalReservas = reservas.size()
                var temReservaAtiva = false

                for (doc in reservas) {
                    // 🔥 VERIFICAÇÃO SIMPLES - SE NÃO ESTIVER MAIS ATIVO, PARA
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

                    // 🔥 BUSCA DADOS DA VAGA COM VERIFICAÇÃO
                    val vagaDoc = try {
                        db.collection("vaga").document(vagaId).get().await()
                    } catch (e: Exception) {
                        continue // PULA SE HOUVER ERRO AO BUSCAR VAGA
                    }

                    // 🔥 VERIFICA SE AINDA ESTÁ ATIVO
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
                        // 🔥 BUSCA NOME DO ESTACIONAMENTO SE NECESSÁRIO
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

                // 🔥 ATUALIZA UI APENAS SE AINDA ESTIVER ATIVO
                if (isAdded && context != null) {
                    atualizarUI(totalHoras, historico, temReservaAtiva, totalReservas)
                }

            } catch (e: Exception) {
                if (isAdded && context != null) {
                    Log.e("MotoristaFragment", "Erro ao carregar reservas", e)
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

    // 🔥 NOVO MÉTODO: Busca o nome do estacionamento se não estiver na reserva
    private suspend fun buscarNomeEstacionamento(estacionamentoId: String): String {
        // 🔥 VERIFICA SE AINDA ESTÁ ATIVO
        if (!isAdded || context == null) return "Estacionamento"

        return try {
            if (estacionamentoId.isEmpty()) return "Estacionamento"

            val estacionamentoDoc = db.collection("estacionamento").document(estacionamentoId).get().await()

            // 🔥 VERIFICA NOVAMENTE ANTES DE RETORNAR
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