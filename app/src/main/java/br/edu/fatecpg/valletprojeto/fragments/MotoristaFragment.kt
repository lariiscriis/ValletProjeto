package br.edu.fatecpg.valletprojeto.fragments

import HistoricoReservasAdapter
import ReservaHistorico
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
import br.edu.fatecpg.valletprojeto.CadastroCarro
import br.edu.fatecpg.valletprojeto.CarroActivity
import br.edu.fatecpg.valletprojeto.VagaActivity
import br.edu.fatecpg.valletprojeto.databinding.FragmentMotoristaDashboardBinding
import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MotoristaFragment : Fragment() {

    private var _binding: FragmentMotoristaDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoricoReservasAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMotoristaDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvReservationHistory.layoutManager = GridLayoutManager(requireContext(), 1)

        // Define o clique do texto para abrir a tela de vagas
        binding.tvNoReservation.setOnClickListener {
            // Exemplo de navegação: abrir Activity ou Fragment de vagas
            abrirPaginaDeVagas()
        }

        buscarReservasComCoroutines()
    }

    private fun abrirPaginaDeVagas() {

        val intent = Intent(requireContext(), CadastroCarro::class.java)
        startActivity(intent)

        Toast.makeText(requireContext(), "Abrir página de vagas", Toast.LENGTH_SHORT).show()
    }

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
                        binding.tvNoReservation.visibility = View.GONE

                        binding.tvSpotLetter.text = vaga.numero
                        binding.tvLocation.text = "Local: ${vaga.localizacao}"
                        binding.tvTimeRange.text = formatarHorario(inicio, fim)
                        binding.tvTimeRemaining.text = "Reserva ativa"
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
                    binding.tvNoReservation.visibility = View.VISIBLE
                }

                binding.tvTotalReservations.text = totalReservas.toString()
                val horasTotais = TimeUnit.MILLISECONDS.toHours(totalHoras)
                binding.tvTotalHours.text = "${horasTotais}h"

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
