package br.edu.fatecpg.valletprojeto.fragments

import br.edu.fatecpg.valletprojeto.model.ReservaHistorico
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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

    // Variáveis para guardar dados da reserva ativa
    private var vagaIdAtiva: String? = null
    private var estacionamentoIdAtivo: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMotoristaDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvReservationHistory.layoutManager = GridLayoutManager(requireContext(), 1)

        // Clique para abrir a reserva ativa
        binding.btnViewReservation.setOnClickListener {
            if (vagaIdAtiva != null && estacionamentoIdAtivo != null) {
                val intent = Intent(requireContext(), ReservaActivity::class.java)
                intent.putExtra("vagaId", vagaIdAtiva)
                intent.putExtra("estacionamentoId", estacionamentoIdAtivo)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Nenhuma reserva ativa encontrada.", Toast.LENGTH_SHORT).show()
            }
        }

        // Clique para abrir a página de vagas
        binding.txvNoReservation.setOnClickListener {
            abrirPaginaDeVagas()
        }
        buscarReservasComCoroutines()
    }

    private fun abrirPaginaDeVagas() {
        val intent = Intent(requireContext(), VagaActivity::class.java)
        startActivity(intent)
        Toast.makeText(requireContext(), "Abrir página de vagas", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun buscarReservasComCoroutines() {
        val uidLogado = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                val reservas = db.collection("reserva").whereEqualTo("usuarioId", uidLogado).get().await()

                var totalHoras = 0L
                val historico = mutableListOf<ReservaHistorico>()
                val totalReservas = reservas.size()

                var temReservaAtiva = false

                for (doc in reservas) {
                    val status = doc.getString("status")
                    val vagaId = doc.getString("vagaId") ?: continue
                    val estacionamentoId = doc.getString("estacionamentoId") ?: "" // pegar estacionamentoId
                    val inicio = doc.getTimestamp("inicioReserva")?.toDate()
                    val fim = doc.getTimestamp("fimReserva")?.toDate()

                    if (inicio != null && fim != null) {
                        totalHoras += (fim.time - inicio.time)
                    }

                    val vagaDoc = db.collection("vaga").document(vagaId).get().await()
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

                        // Guardar os dados para abrir depois
                        vagaIdAtiva = vagaId
                        estacionamentoIdAtivo = estacionamentoId
                    } else {
                        historico.add(
                            ReservaHistorico(
                                vaga.numero,
                                SimpleDateFormat("dd/MM", Locale.getDefault()).format(inicio!!),
                                formatarHorario(inicio, fim)
                            )
                        )
                    }
                }

                if (!temReservaAtiva) {
                    binding.cardReservaAtual.visibility = View.GONE
                    binding.txvNoReservation.visibility = View.VISIBLE
                    // Limpa os dados da reserva ativa caso não tenha
                    vagaIdAtiva = null
                    estacionamentoIdAtivo = null
                }

                binding.txvTotalReservations.text = totalReservas.toString()
                val horasTotais = TimeUnit.MILLISECONDS.toHours(totalHoras)
                binding.txvTotalHours.text = "${horasTotais}h"

                adapter = HistoricoReservasAdapter(historico)
                binding.rvReservationHistory.adapter = adapter

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatarHorario(inicio: Date?, fim: Date?): String {
        val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
        return if (inicio != null && fim != null) "${formato.format(inicio)} - ${formato.format(fim)}" else ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

