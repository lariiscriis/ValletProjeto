package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityIntroCadastroCarroBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class IntroCadastroCarro : AppCompatActivity() {
    private lateinit var binding: ActivityIntroCadastroCarroBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityIntroCadastroCarroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emailUsuario = intent.getStringExtra("email_usuario") ?: "Usuário"
        binding.txtNome.text = "$emailUsuario"

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
        // Botão para cadastro de carro
        binding.btnIrParaCadastro.setOnClickListener {
            navigateToCarRegistration()
        }

//        binding.btnSair?.setOnClickListener {
//            performLogout()
//        }
//
//        binding.btnVaga?.setOnClickListener {
//            val intent = Intent(this, CadastroVagaActivity::class.java)
//            startActivity(intent)
//        }
//
//
//        binding.btnadmin.setOnClickListener{
//            val intent = Intent(this, IntroCadastroEstacionamento::class.java)
//            startActivity(intent)
//        }

        binding.btnDashboard.setOnClickListener{
            val intent = Intent(this, Dashboard_base::class.java)
            startActivity(intent)
        }

    }

    private fun navigateToCarRegistration() {
        val intent = Intent(this, CadastroCarro::class.java)
        startActivity(intent)
        // Adicione animação de transição se desejar
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Logout realizado com sucesso", Toast.LENGTH_SHORT).show()
        redirectToLogin()
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
