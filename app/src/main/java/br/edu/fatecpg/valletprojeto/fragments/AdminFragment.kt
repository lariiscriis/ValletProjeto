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
            // 🔍 Buscar estacionamento do admin logado
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

    /**
     * Gera relatórios filtrando apenas as vagas e reservas do estacionamento logado.
     */
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

        // 🔍 Busca vagas apenas do estacionamento logado
        db.collection("vaga")
            .whereEqualTo("estacionamentoId", estacionamentoId)
            .get()
            .addOnSuccessListener { vagasSnapshot ->
                val vagaIds = vagasSnapshot.documents.map { it.id }

                if (vagaIds.isNotEmpty()) {
                    // 🔍 Busca reservas apenas das vagas desse estacionamento
                    db.collection("reserva")
                        .whereIn("vagaId", vagaIds)
                        .whereGreaterThanOrEqualTo("inicioReserva", Timestamp(inicioMes.time))
                        .get()
                        .addOnSuccessListener { reservasMensais ->
                        val totalReservasMensais = reservasMensais.size()
                        var receitaEstimanda = 0.0
                        val usoVagaDia = HashMap<String, Int>()
                        var reservasDia = 0
                        val horariosReservas = HashMap<Int, Int>()
                        val precoPorVaga = vagasSnapshot.associate {
                            it.id to (it.getDouble("preco") ?: 0.0)
                        }

                        reservasMensais.forEach { reservaDoc ->
                            val inicioReserva = reservaDoc.getTimestamp("inicioReserva")?.toDate()
                            val fimReserva = reservaDoc.getTimestamp("fimReserva")?.toDate()
                            val vagaId = reservaDoc.getString("vagaId") ?: ""
                            if (inicioReserva != null && fimReserva != null) {
                                val preco = precoPorVaga[vagaId] ?: 0.0
                                val horas = (fimReserva.time - inicioReserva.time) / (1000 * 60 * 60).toDouble()
                                receitaEstimanda += preco * horas

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

                        val vagasMaisUsadas = usoVagaDia.entries
                            .sortedByDescending { it.value }
                            .take(3)
                            .associate { it.key to "${it.value} vezes" }

                        val relatorioDiario = hashMapOf("peakHours" to peakHours)
                        val relatorioVagas = vagasMaisUsadas
                        val relatorioMensal = hashMapOf(
                            "monthlyReservations" to totalReservasMensais,
                            "estimatedRevenue" to receitaEstimanda
                        )

                        val batch = db.batch()
                        batch.set(db.collection("RelatorioDiario_$estacionamentoId").document("horarioPico"), relatorioDiario)
                        batch.set(db.collection("RelatorioDiario_$estacionamentoId").document("vagasMaisUsadas"), relatorioVagas)
                        batch.set(db.collection("RelatorioMensal_$estacionamentoId").document("resumo"), relatorioMensal)

                        batch.commit()
                            .addOnSuccessListener {
                                Log.d("AdminFragment", "Relatórios atualizados para $estacionamentoId")
                                onComplete()
                            }
                        }
                        .addOnFailureListener {
                            Log.e("AdminFragment", "Erro ao buscar reservas do estacionamento", it)
                            onComplete()
                        }
                } else {
                    Log.d("AdminFragment", "Não há vagas para este estacionamento")
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

        db.collection("RelatorioDiario_$estacionamentoId").document("horarioPico").get()
            .addOnSuccessListener {
                binding.tvPeakHours.text = it.getString("peakHours") ?: "N/A"
            }

        db.collection("RelatorioMensal_$estacionamentoId").document("resumo").get()
            .addOnSuccessListener {
                val monthly = it.getLong("monthlyReservations") ?: 0
                val revenue = it.getDouble("estimatedRevenue") ?: 0.0
                binding.tvMonthlyReservations.text = monthly.toString()
                binding.tvEstimatedRevenue.text = "R$ %.2f".format(revenue)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
