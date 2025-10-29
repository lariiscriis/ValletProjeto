package br.edu.fatecpg.valletprojeto.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.edu.fatecpg.valletprojeto.databinding.FragmentAdminDashboardBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.collections.HashMap

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser
        if (user != null) {
            db.collection("estacionamento")
                .whereEqualTo("adminUid", user.uid)
                .get()
                .addOnSuccessListener { estacionamentos ->
                    if (!estacionamentos.isEmpty) {
                        val estacionamentoId = estacionamentos.documents[0].id
                        gerarEAtualizarRelatorios(estacionamentoId) {
                            loadAdminDashboardData(estacionamentoId)
                        }
                    } else {
                        Log.e("AdminFragment", "Nenhum estacionamento encontrado para o admin")
                    }
                }
                .addOnFailureListener {
                    Log.e("AdminFragment", "Erro ao buscar estacionamento do admin", it)
                }
        }
    }

    private fun gerarEAtualizarRelatorios(estacionamentoId: String, onComplete: () -> Unit) {
        val calendario = Calendar.getInstance()
        val anoAtual = calendario.get(Calendar.YEAR)
        val mesAtual = calendario.get(Calendar.MONTH)
        val diaAtual = calendario.get(Calendar.DAY_OF_MONTH)

        val inicioMes = Calendar.getInstance().apply {
            set(anoAtual, mesAtual, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val inicioDia = Calendar.getInstance().apply {
            set(anoAtual, mesAtual, diaAtual, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fimDia = Calendar.getInstance().apply {
            set(anoAtual, mesAtual, diaAtual, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        db.collection("vaga")
            .whereEqualTo("estacionamentoId", estacionamentoId)
            .get()
            .addOnSuccessListener { vagasSnapshot ->
                val vagaIds = vagasSnapshot.documents.map { it.id }

                if (vagaIds.isNotEmpty()) {
                    db.collection("reserva")
                        .whereIn("vagaId", vagaIds)
                        .whereGreaterThanOrEqualTo("inicioReserva", Timestamp(inicioMes.time))
                        .get()
                        .addOnSuccessListener { reservasMensais ->

                            var receitaEstimanda = 0.0
                            val usoVagaMes = HashMap<String, Int>()
                            val usoVagaDia = HashMap<String, Int>()
                            val horariosReservas = HashMap<Int, Int>()
                            var reservasDia = 0

                            val precoPorVaga = vagasSnapshot.associate {
                                it.id to (it.getDouble("preco") ?: 0.0)
                            }
                            val vagaNomes = vagasSnapshot.associate {
                                it.id to (it.getString("numero") ?: it.id)
                            }

                            reservasMensais.forEach { reservaDoc ->
                                val inicioReserva = reservaDoc.getTimestamp("inicioReserva")?.toDate()
                                val fimReserva = reservaDoc.getTimestamp("fimReserva")?.toDate()
                                val vagaId = reservaDoc.getString("vagaId") ?: return@forEach

                                if (inicioReserva != null && fimReserva != null) {
                                    val preco = precoPorVaga[vagaId] ?: 0.0
                                    val horas = (fimReserva.time - inicioReserva.time) / (1000 * 60 * 60).toDouble()
                                    receitaEstimanda += preco * horas

                                    usoVagaMes[vagaId] = usoVagaMes.getOrDefault(vagaId, 0) + 1

                                    if (inicioReserva.after(inicioDia.time) && inicioReserva.before(fimDia.time)) {
                                        reservasDia++
                                        usoVagaDia[vagaId] = usoVagaDia.getOrDefault(vagaId, 0) + 1

                                        val cal = Calendar.getInstance().apply { time = inicioReserva }
                                        val hora = cal.get(Calendar.HOUR_OF_DAY)
                                        horariosReservas[hora] = horariosReservas.getOrDefault(hora, 0) + 1
                                    }
                                }
                            }

                            val peakHours = horariosReservas.maxByOrNull { it.value }?.let {
                                val h1 = it.key; val h2 = (h1 + 1) % 24
                                "%02d:00 - %02d:00".format(h1, h2)
                            } ?: "N/A"

                            val vagasMaisUsadas = usoVagaMes.entries
                                .sortedByDescending { it.value }
                                .take(3)
                                .associate { (vagaNomes[it.key] ?: it.key) to it.value }


                            val vagasUsadasHoje = usoVagaDia.mapKeys {
                                vagaNomes[it.key] ?: it.key
                            }

                            val relatorioDiario = hashMapOf(
                                "reservasDia" to reservasDia,
                                "peakHours" to peakHours,
                                "vagasUsadasHoje" to vagasUsadasHoje
                            )


                            val relatorioVagas = vagasMaisUsadas

                            val relatorioMensal = hashMapOf(
                                "totalReservasMes" to reservasMensais.size(),
                                "estimatedRevenue" to receitaEstimanda
                            )

                            val batch = db.batch()

                            val relatoriosRef = db.collection("relatorios").document(estacionamentoId)
                            batch.set(relatoriosRef.collection("diario").document("dados"), relatorioDiario)
                            batch.set(relatoriosRef.collection("diario").document("vagasMaisUsadasHoje"), relatorioVagas)
                            batch.set(relatoriosRef.collection("mensal").document("resumo"), relatorioMensal)

                            batch.commit()
                                .addOnSuccessListener {
                                    onComplete()
                                }
                                .addOnFailureListener {
                                    onComplete()
                                }
                        }
                        .addOnFailureListener {
                            Log.e("AdminFragment", "Erro ao buscar reservas do estacionamento", it)
                            onComplete()
                        }
                } else {
                    Log.d("AdminFragment", "Nenhuma vaga encontrada para este estacionamento")
                    onComplete()
                }
            }
            .addOnFailureListener {
                Log.e("AdminFragment", "Erro ao buscar vagas do estacionamento", it)
                onComplete()
            }
    }

    private fun loadAdminDashboardData(estacionamentoId: String) {
        if (!isAdded || _binding == null) return

        val relatoriosRef = db.collection("relatorios").document(estacionamentoId)

        relatoriosRef.collection("diario").document("dados").get()
            .addOnSuccessListener {
                val peakHours = it.getString("peakHours") ?: "N/A"
                val reservasDia = it.getLong("reservasDia") ?: 0
                binding.tvPeakHours.text = peakHours
            }
            .addOnFailureListener {
                Log.e("AdminFragment", "Erro ao carregar relatório diário", it)
            }

        relatoriosRef.collection("mensal").document("resumo").get()
            .addOnSuccessListener {
                val monthly = it.getLong("totalReservasMes") ?: 0
                val revenue = it.getDouble("estimatedRevenue") ?: 0.0
                binding.tvMonthlyReservations.text = monthly.toString()
                binding.tvEstimatedRevenue.text = "R$ %.2f".format(revenue)
            }
            .addOnFailureListener {
                Log.e("AdminFragment", "Erro ao carregar relatório mensal", it)
            }

        relatoriosRef.collection("diario").document("vagasMaisUsadasHoje").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val vagasMap = snapshot.data as? Map<String, Long> ?: emptyMap()
                    val top3 = vagasMap.entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key to it.value }


                    val spots = listOf(binding.tvSpotA1, binding.tvSpotB2, binding.tvSpotB3)
                    spots.forEachIndexed { index, textView ->
                        if (index < top3.size) {
                            val (vaga, _) = top3[index]
                            textView.text = vaga
                        } else {
                            textView.text = "--"
                        }
                    }


                } else {
                    listOf(binding.tvSpotA1, binding.tvSpotB2, binding.tvSpotB3).forEach {
                        it.text = "--"
                    }
                }
            }
            .addOnFailureListener {
                Log.e("AdminFragment", "Erro ao carregar top 3 vagas usadas", it)
            }

        db.collection("vaga")
            .whereEqualTo("estacionamentoId", estacionamentoId)
            .get()
            .addOnSuccessListener { vagasSnapshot ->
                val totalSpots = vagasSnapshot.size()
                val occupiedSpots = vagasSnapshot.count { it.getBoolean("disponivel") == false }
                val availableSpots = totalSpots - occupiedSpots
                val occupancyRate = if (totalSpots > 0) (occupiedSpots * 100 / totalSpots) else 0

                binding.tvTotalSpots.text = totalSpots.toString()
                binding.tvOccupiedSpots.text = occupiedSpots.toString()
                binding.tvAvailableSpots.text = availableSpots.toString()
                binding.tvOccupancyRate.text = "$occupancyRate%"
            }
            .addOnFailureListener {
                Log.e("AdminFragment", "Erro ao carregar vagas em tempo real", it)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
