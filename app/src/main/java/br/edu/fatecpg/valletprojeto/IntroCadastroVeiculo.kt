package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityIntroCadastroVeiculoBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class IntroCadastroVeiculo : AppCompatActivity() {
    private lateinit var binding: ActivityIntroCadastroVeiculoBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityIntroCadastroVeiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emailUsuario = intent.getStringExtra("email_usuario") ?: "Usuário"
        binding.txtNome.text = emailUsuario
        auth = Firebase.auth

        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        setupEdgeToEdge()
        setupGifAnimation()
        setupButtonListeners()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupGifAnimation() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.car_intro_animado)
            .into(binding.gifCarro)
    }

    private fun setupButtonListeners() {
        binding.btnIrParaCadastro.setOnClickListener {
            navigateToCarRegistration()
        }
    }

    private fun navigateToCarRegistration() {
        val intent = Intent(this, VeiculoActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            redirectToLogin()
        }
    }
}
