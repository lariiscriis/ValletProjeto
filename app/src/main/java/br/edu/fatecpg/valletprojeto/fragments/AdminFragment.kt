package br.edu.fatecpg.valletprojeto.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.edu.fatecpg.valletprojeto.databinding.FragmentAdminDashboardBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

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

        // Primeiro calcula e salva os relatórios
        gerarEAtualizarRelatorios {
            // Depois carrega para a UI
            loadAdminDashboardData()
        }
    }

    private fun gerarEAtualizarRelatorios(onComplete: () -> Unit) {
        // Primeiro vamos buscar todas as reservas e vagas para calcular os dados

        // Busca todas as reservas do mês atual e do dia atual
        val calendario = Calendar.getInstance()
        val anoAtual = calendario.get(Calendar.YEAR)
        val mesAtual = calendario.get(Calendar.MONTH) // zero-based
        val diaAtual = calendario.get(Calendar.DAY_OF_MONTH)

        // Datas limites para filtro mensal e diário
        val inicioMes = Calendar.getInstance()
        inicioMes.set(anoAtual, mesAtual, 1, 0, 0, 0)
        inicioMes.set(Calendar.MILLISECOND, 0)

        val inicioDia = Calendar.getInstance()
        inicioDia.set(anoAtual, mesAtual, diaAtual, 0, 0, 0)
        inicioDia.set(Calendar.MILLISECOND, 0)

        val fimDia = Calendar.getInstance()
        fimDia.set(anoAtual, mesAtual, diaAtual, 23, 59, 59)
        fimDia.set(Calendar.MILLISECOND, 999)

        db.collection("reserva")
            .whereGreaterThanOrEqualTo("inicioReserva", Timestamp(inicioMes.time))
            .get()
            .addOnSuccessListener { reservasMensais ->

                db.collection("vaga")
                    .get()
                    .addOnSuccessListener { vagasSnapshot ->
                        // Calcular totais para relatório mensal
                        val totalReservasMensais = reservasMensais.size()
                        var receitaEstimanda = 0.0

                        // Mapa para contar uso diário por vaga (para relatório diário)
                        val usoVagaDia = HashMap<String, Int>()

                        // Variável para contar reservas do dia atual
                        var reservasDia = 0

                        // Horários de reserva para cálculo horário de pico (ex: contagem de reservas por hora)
                        val horariosReservas = HashMap<Int, Int>() // chave = hora do início, valor = quantidade

                        // Prepara mapa de vagas para acessar preço
                        val precoPorVaga = HashMap<String, Double>()
                        vagasSnapshot.forEach { vagaDoc ->
                            val vagaId = vagaDoc.id
                            val preco = vagaDoc.getDouble("preco") ?: 0.0
                            precoPorVaga[vagaId] = preco
                        }

                        // Processa reservas mensais
                        reservasMensais.forEach { reservaDoc ->

                            val inicioReserva = reservaDoc.getTimestamp("inicioReserva")?.toDate()
                            val fimReserva = reservaDoc.getTimestamp("fimReserva")?.toDate()
                            val vagaId = reservaDoc.getString("vagaId") ?: ""
                            if (inicioReserva != null && fimReserva != null) {

                                // Cálculo receita: preço da vaga * horas de reserva
                                val preco = precoPorVaga[vagaId] ?: 0.0

                                val horasReservadas = (fimReserva.time - inicioReserva.time) / (1000 * 60 * 60).toDouble()
                                receitaEstimanda += preco * horasReservadas

                                // Se reserva é do dia atual (compara datas ignorando horário)
                                if (inicioReserva.after(inicioDia.time) && inicioReserva.before(fimDia.time)) {
                                    reservasDia++
                                    // Conta uso da vaga no dia
                                    usoVagaDia[vagaId] = usoVagaDia.getOrDefault(vagaId, 0) + 1

                                    // Conta horário de pico por hora de início da reserva
                                    val cal = Calendar.getInstance()
                                    cal.time = inicioReserva
                                    val hora = cal.get(Calendar.HOUR_OF_DAY)
                                    horariosReservas[hora] = horariosReservas.getOrDefault(hora, 0) + 1
                                }
                            }
                        }

                        // Calcula horário de pico (hora com maior uso)
                        val maxUsoHora = horariosReservas.maxByOrNull { it.value }
                        val peakHours = if (maxUsoHora != null) {
                            val horaInicio = maxUsoHora.key
                            val horaFim = (horaInicio + 1) % 24
                            String.format("%02d:00 - %02d:00", horaInicio, horaFim)
                        } else {
                            "N/A"
                        }

                        // Calcula vagas mais usadas no dia, pega as 3 primeiras (ou predefinidas)
                        val vagasMaisUsadas = usoVagaDia.entries
                            .sortedByDescending { it.value }
                            .take(3)
                            .map { it.key to "${it.value} vezes" }
                            .toMap()

                        // Se não tiver 3 vagas usadas, completa com vagas fixas padrão
                        val vagasFixas = listOf("spotA1", "spotB2", "spotB3")
                        val vagasMaisUsadasCompletas = vagasFixas.mapIndexed { index, vagaId ->
                            vagaId to (vagasMaisUsadas[vagaId] ?: "0 vezes")
                        }.toMap()

                        // Atualiza relatório diário
                        val relatorioDiario = hashMapOf(
                            "peakHours" to peakHours
                        )
                        val relatorioVagas = hashMapOf(
                            "spotA1" to vagasMaisUsadasCompletas["spotA1"],
                            "spotB2" to vagasMaisUsadasCompletas["spotB2"],
                            "spotB3" to vagasMaisUsadasCompletas["spotB3"]
                        )

                        // Atualiza relatório mensal
                        val relatorioMensal = hashMapOf(
                            "monthlyReservations" to totalReservasMensais,
                            "estimatedRevenue" to receitaEstimanda
                        )

                        // Agora salva tudo no Firestore
                        val batch = db.batch()

                        val docHorarioPico = db.collection("RelatorioDiario").document("horarioPico")
                        batch.set(docHorarioPico, relatorioDiario)

                        val docVagasMaisUsadas = db.collection("RelatorioDiario").document("vagasMaisUsadas")
                        batch.set(docVagasMaisUsadas, relatorioVagas)

                        val docResumoMensal = db.collection("RelatorioMensal").document("resumo")
                        batch.set(docResumoMensal, relatorioMensal)

                        batch.commit()
                            .addOnSuccessListener {
                                Log.d("AdminFragment", "Relatórios atualizados com sucesso")
                                onComplete()
                            }
                            .addOnFailureListener { e ->
                                Log.e("AdminFragment", "Erro ao atualizar relatórios", e)
                                onComplete()
                            }
                    }
                    .addOnFailureListener {
                        Log.e("AdminFragment", "Erro ao carregar vagas")
                        onComplete()
                    }
            }
            .addOnFailureListener {
                Log.e("AdminFragment", "Erro ao carregar reservas")
                onComplete()
            }
    }

    private fun loadAdminDashboardData() {
        if (!isAdded || _binding == null) return

        db.collection("RelatorioDiario")
            .document("horarioPico")
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                binding.tvPeakHours.text = doc.getString("peakHours") ?: "N/A"
            }

        db.collection("RelatorioDiario")
            .document("vagasMaisUsadas")
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                binding.tvSpotA1.text = doc.getString("spotA1") ?: "A1"
                binding.tvSpotB2.text = doc.getString("spotB2") ?: "B2"
                binding.tvSpotB3.text = doc.getString("spotB3") ?: "B3"
            }

        db.collection("RelatorioMensal")
            .document("resumo")
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                val monthlyReservations = doc.getLong("monthlyReservations") ?: 0L
                val estimatedRevenue = doc.getDouble("estimatedRevenue") ?: 0.0
                binding.tvMonthlyReservations.text = monthlyReservations.toString()
                binding.tvEstimatedRevenue.text = "$${"%,.2f".format(estimatedRevenue)}"
            }

        db.collection("vaga")
            .get()
            .addOnSuccessListener { vagasSnapshot ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
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
