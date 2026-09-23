package br.edu.fatecpg.valletprojeto.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import br.edu.fatecpg.valletprojeto.model.Reserva
import br.edu.fatecpg.valletprojeto.model.Vaga
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

/**
 * BroadcastReceiver para lidar com as ações da notificação de confirmação de reserva.
 */
class ConfirmacaoReceiver : BroadcastReceiver() {

    private val db = FirebaseFirestore.getInstance()

    override fun onReceive(context: Context, intent: Intent) {
        // Extrai os dados do Intent
        val confirmacao = intent.getBooleanExtra("confirmacao", false)
        val vagaId = intent.getStringExtra("vagaId") ?: ""
        val usuarioId = intent.getStringExtra("usuarioId") ?: ""
        val placa = intent.getStringExtra("placa") ?: "Não detectada"
        val notificationId = intent.getIntExtra("notification_id", -1)

        // Fecha a notificação
        if (notificationId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationId)
        }

        Log.d("CONFIRMACAO", "Resposta do usuário: $confirmacao para a vaga $vagaId")

        if (vagaId.isBlank() || usuarioId.isBlank()) {
            Log.e("CONFIRMACAO", "vagaId ou usuarioId está vazio. Abortando.")
            return
        }

        if (confirmacao) {
            // Usuário confirmou: criar a reserva
            criarReservaComEstacionamento(context, vagaId, usuarioId, placa)
        } else {
            // Usuário negou: notificar o administrador
            notificarAdmin(context, vagaId, placa, usuarioId, "Estacionou sem Reserva e Recusou Criar")
        }
    }

    /**
     * Cria uma nova reserva de 1 hora para a vaga, buscando o nome do estacionamento.
     */
    private fun criarReservaComEstacionamento(context: Context, vagaId: String, usuarioId: String, placa: String) {
        // Primeiro, buscamos os detalhes da vaga
        db.collection("vaga").document(vagaId).get()
            .addOnSuccessListener { vagaSnapshot ->
                if (!vagaSnapshot.exists()) {
                    Log.e("CONFIRMACAO", "Vaga $vagaId não encontrada")
                    notificarAdmin(context, vagaId, placa, usuarioId, "Falha ao Criar Reserva (Vaga não encontrada)")
                    return@addOnSuccessListener
                }

                val vaga = vagaSnapshot.toObject(Vaga::class.java)
                val estacionamentoId = vaga?.estacionamentoId ?: vagaSnapshot.getString("id_estacionamento")

                if (estacionamentoId.isNullOrEmpty()) {
                    Log.e("CONFIRMACAO", "EstacionamentoId não encontrado para vaga $vagaId")
                    notificarAdmin(context, vagaId, placa, usuarioId, "Falha ao Criar Reserva (Estacionamento não encontrado)")
                    return@addOnSuccessListener
                }

                // Agora busca o nome do estacionamento
                buscarNomeEstacionamentoECriarReserva(context, vagaId, usuarioId, placa, estacionamentoId, vaga)

            }
            .addOnFailureListener { e ->
                Log.e("CONFIRMACAO", "Erro ao buscar vaga $vagaId", e)
                notificarAdmin(context, vagaId, placa, usuarioId, "Falha ao Buscar Vaga para Reserva")
            }
    }

    /**
     * Busca o nome do estacionamento e então cria a reserva
     */
    private fun buscarNomeEstacionamentoECriarReserva(
        context: Context,
        vagaId: String,
        usuarioId: String,
        placa: String,
        estacionamentoId: String,
        vaga: Vaga?
    ) {
        db.collection("estacionamento").document(estacionamentoId).get()
            .addOnSuccessListener { estacionamentoSnapshot ->
                if (!estacionamentoSnapshot.exists()) {
                    Log.e("CONFIRMACAO", "Estacionamento $estacionamentoId não encontrado")
                    notificarAdmin(context, vagaId, placa, usuarioId, "Falha ao Criar Reserva (Estacionamento não existe)")
                    return@addOnSuccessListener
                }

                val nomeEstacionamento = estacionamentoSnapshot.getString("nome") ?: "Estacionamento"
                val numeroVaga = vaga?.numero ?: vagaId

                Log.d("CONFIRMACAO", "📝 Criando reserva para: Estacionamento=$nomeEstacionamento, Vaga=$numeroVaga")

                // Agora cria a reserva com todas as informações
                criarReservaFinal(vagaId, usuarioId, placa, estacionamentoId, nomeEstacionamento, numeroVaga, context)

            }
            .addOnFailureListener { e ->
                Log.e("CONFIRMACAO", "Erro ao buscar estacionamento $estacionamentoId", e)
                // Cria reserva mesmo sem nome do estacionamento (com nome padrão)
                val numeroVaga = vaga?.numero ?: vagaId
                criarReservaFinal(vagaId, usuarioId, placa, estacionamentoId, "Estacionamento", numeroVaga, context)
            }
    }

    /**
     * Cria a reserva final com todas as informações
     */
    private fun criarReservaFinal(
        vagaId: String,
        usuarioId: String,
        placa: String,
        estacionamentoId: String,
        nomeEstacionamento: String,
        numeroVaga: String,
        context: Context
    ) {
        val inicio = Timestamp.now()
        val fim = Timestamp(inicio.seconds + TimeUnit.HOURS.toSeconds(1), inicio.nanoseconds)

        val novaReserva = hashMapOf(
            "usuarioId" to usuarioId,
            "vagaId" to vagaId,
            "estacionamentoId" to estacionamentoId,
            "estacionamentoNome" to nomeEstacionamento,
            "status" to "ativa",
            "inicioReserva" to inicio,
            "fimReserva" to fim,
            "placaVeiculo" to placa,
            "criadoAutomaticamente" to true,
            "numeroVaga" to numeroVaga
        )

        db.collection("reserva").add(novaReserva)
            .addOnSuccessListener { docRef ->
                Log.d("CONFIRMACAO", "✅ Reserva automática criada com sucesso: ${docRef.id}")
                Log.d("CONFIRMACAO", "   📍 Estacionamento: $nomeEstacionamento")
                Log.d("CONFIRMACAO", "   🅿️  Vaga: $numeroVaga")
                Log.d("CONFIRMACAO", "   👤 Usuário: $usuarioId")
                Log.d("CONFIRMACAO", "   🚗 Placa: $placa")

                // Atualiza a vaga como ocupada
                atualizarVagaComoOcupada(vagaId)

                // Opcional: Enviar notificação de sucesso
                enviarNotificacaoSucesso(context, nomeEstacionamento, numeroVaga)
            }
            .addOnFailureListener { e ->
                Log.e("CONFIRMACAO", "❌ Erro ao criar reserva automática", e)
                notificarAdmin(context, vagaId, placa, usuarioId, "Falha ao Salvar Reserva no DB: ${e.message}")
            }
    }

    /**
     * Atualiza a vaga como ocupada
     */
    /**
     * Atualiza a vaga como ocupada - CORREÇÃO DA SINTAXE
     */
    private fun atualizarVagaComoOcupada(vagaId: String) {
        val updates = hashMapOf<String, Any>(
            "disponivel" to false,
            "status" to "OCUPADA"
        )

        db.collection("vaga").document(vagaId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("CONFIRMACAO", "✅ Vaga $vagaId atualizada como ocupada")
            }
            .addOnFailureListener { e ->
                Log.e("CONFIRMACAO", "❌ Erro ao atualizar vaga $vagaId", e)
            }
    }

    /**
     * Envia notificação de sucesso (opcional)
     */
    private fun enviarNotificacaoSucesso(context: Context, nomeEstacionamento: String, numeroVaga: String) {
        // Aqui você pode implementar uma notificação local de sucesso
        // usando NotificationManager se desejar
        Log.d("CONFIRMACAO", "🎉 Reserva criada com sucesso no $nomeEstacionamento - Vaga $numeroVaga")
    }

    /**
     * Envia uma notificação para a coleção de administradores.
     */
    private fun notificarAdmin(context: Context, vagaId: String, placa: String, uid: String, motivo: String) {
        val notificacao = hashMapOf(
            "vagaId" to vagaId,
            "placa" to placa,
            "usuarioId" to uid,
            "motivo" to motivo,
            "timestamp" to Timestamp.now(),
            "tipo" to "sistema_alerta_estacionamento"
        )

        db.collection("notificacoes_admin").add(notificacao)
            .addOnSuccessListener {
                Log.d("CONFIRMACAO", "Admin notificado com sucesso: $motivo")
            }
            .addOnFailureListener { e ->
                Log.e("CONFIRMACAO", "Erro ao notificar admin", e)
            }
    }
}