package br.edu.fatecpg.valletprojeto.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_SERVICE"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Chamado quando um novo token FCM é gerado ou atualizado.
     * O token deve ser salvo no Firestore para que o script Python possa usá-lo.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Novo Token FCM: $token")

        // Salva o token no Firestore
        salvarTokenNoFirestore(token)
    }

    /**
     * Chamado quando uma mensagem FCM é recebida enquanto o app está em primeiro plano.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Mensagem recebida de: ${remoteMessage.from}")

        // Aqui você pode processar a mensagem e mostrar uma notificação local
        remoteMessage.notification?.let {
            Log.d(TAG, "Notificação recebida: ${it.body}")
            // Exemplo: mostrarNotificacao(it.title, it.body)
        }

        // Processar dados (usados pelo script Python)
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Dados da mensagem: ${remoteMessage.data}")
            // Exemplo: processarDados(remoteMessage.data)
        }
    }

    /**
     * Salva o token FCM no documento do usuário logado no Firestore.
     * Coleção: 'usuario', Documento ID: UID do usuário.
     */
    private fun salvarTokenNoFirestore(token: String) {
        val user = auth.currentUser

        if (user == null) {
            Log.w(TAG, "Usuário não logado. Token será salvo após o login.")
            // O token será salvo na LoginActivity ou MainActivity após o login
            return
        }

        val uid = user.uid
        val tokenData = hashMapOf("fcm_token" to token)

        db.collection("usuario").document(uid)
            .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Token FCM salvo com sucesso para o UID: $uid")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar Token FCM para o UID: $uid", e)
            }
    }
}