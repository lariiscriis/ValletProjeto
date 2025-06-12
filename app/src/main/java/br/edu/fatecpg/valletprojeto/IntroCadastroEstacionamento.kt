package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityIntroCadastroCarroBinding
import br.edu.fatecpg.valletprojeto.databinding.ActivityIntroCadastroEstacionamentoBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class IntroCadastroEstacionamento : AppCompatActivity() {
    private lateinit var binding: ActivityIntroCadastroEstacionamentoBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityIntroCadastroEstacionamentoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase Auth
        auth = Firebase.auth

        // Verifica se o usuário está autenticado
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        setupEdgeToEdge()
        setupGifAnimation()
        setupButtonListeners()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupButtonListeners() {
        binding.btnIrParaCadastro.setOnClickListener {
            val intent = Intent(this, CadastroEstacionamento::class.java)
            startActivity(intent)
        }


    }

    private fun setupGifAnimation() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.parking)
            .into(binding.gifCarro)
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        // Verifica novamente se o usuário está autenticado quando a activity retorna ao foco
        if (auth.currentUser == null) {
            redirectToLogin()
        }
    }
}