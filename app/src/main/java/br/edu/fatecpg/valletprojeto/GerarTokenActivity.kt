package br.edu.fatecpg.valletprojeto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import br.edu.fatecpg.valletprojeto.databinding.ActivityGerarTokenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class GerarTokenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGerarTokenBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerarTokenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGerarToken.setOnClickListener {
            gerarTokenFCM()
        }

        binding.btnVerificarToken.setOnClickListener {
            verificarTokenExistente()
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        // Verifica automaticamente ao abrir a tela
        verificarTokenExistente()
    }

    private fun gerarTokenFCM() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tvStatus.text = "Gerando token FCM..."

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            binding.progressBar.visibility = android.view.View.GONE

            if (task.isSuccessful) {
                val token = task.result
                binding.tvStatus.text = "✅ Token gerado: ${token.take(15)}..."
                salvarToken(token)
            } else {
                binding.tvStatus.text = "❌ Erro ao gerar token: ${task.exception?.message}"
                Log.e("GERAR_TOKEN", "Falha ao gerar token", task.exception)
            }
        }
    }

    private fun salvarToken(token: String) {
        val user = auth.currentUser
        if (user == null) {
            binding.tvStatus.text = "❌ Usuário não está logado"
            return
        }

        val uid = user.uid
        val tokenData = hashMapOf(
            "fcm_token" to token,
            "ultima_atualizacao_token" to com.google.firebase.Timestamp.now()
        )

        db.collection("usuario").document(uid)
            .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                binding.tvStatus.text = "✅ Token salvo no Firestore com sucesso!"
                Log.d("GERAR_TOKEN", "Token salvo para: $uid")

                // Verifica se foi realmente salvo
                Handler(Looper.getMainLooper()).postDelayed({
                    verificarTokenExistente()
                }, 1000)
            }
            .addOnFailureListener { e ->
                binding.tvStatus.text = "❌ Erro ao salvar token: ${e.message}"
                Log.e("GERAR_TOKEN", "Erro ao salvar token", e)
            }
    }

    private fun verificarTokenExistente() {
        val user = auth.currentUser
        if (user == null) {
            binding.tvStatus.text = "❌ Usuário não está logado"
            return
        }

        val uid = user.uid
        binding.tvStatus.text = "Verificando token existente..."

        db.collection("usuario").document(uid).get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcm_token")
                if (token != null) {
                    binding.tvStatus.text = "✅ Token já existe: ${token.take(15)}..."
                    binding.btnGerarToken.text = "Regenerar Token"
                } else {
                    binding.tvStatus.text = "❌ Nenhum token encontrado"
                    binding.btnGerarToken.text = "Gerar Token"
                }
            }
            .addOnFailureListener { e ->
                binding.tvStatus.text = "❌ Erro ao verificar token: ${e.message}"
            }
    }
}