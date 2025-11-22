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

    override fun onNewToken(token: String) {
        Log.d(TAG, "Novo Token FCM: $token")
        salvarTokenNoFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Mensagem recebida de: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d(TAG, "Notificação recebida: ${it.body}")
        }

        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Dados da mensagem: ${remoteMessage.data}")
        }
    }

    private fun salvarTokenNoFirestore(token: String) {
        val user = auth.currentUser

        if (user == null) {
            Log.w(TAG, "Usuário não logado. Token será salvo após o login.")
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