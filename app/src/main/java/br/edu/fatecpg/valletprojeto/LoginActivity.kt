package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase Auth
        auth = Firebase.auth

        val tipoCadastro = intent.getStringExtra("tipoCadastro")
        isAdmin = tipoCadastro == "admin"

        setupLoginType(isAdmin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.botaoCarros.setOnClickListener {
            val intent = Intent(this, IntroCadastroCarro::class.java)
            startActivity(intent)
        }

        binding.botaoCadastro.setOnClickListener {
            val intent = Intent(this, CadastroActivity::class.java)
            intent.putExtra("tipoCadastro", "usuario")
            startActivity(intent)
        }

        binding.botaoCadastroAdmin.setOnClickListener {
            val intent = Intent(this, CadastroActivity::class.java)
            intent.putExtra("tipoCadastro", "admin")
            startActivity(intent)
        }

        binding.switchTipoLogin.setOnClickListener {
            isAdmin = !isAdmin
            setupLoginType(isAdmin)
        }


        binding.button3.setOnClickListener {
            val email = binding.editTextText.text.toString().trim()
            val senha = binding.editTextSenha.text.toString().trim()

            if (validateInputs(email, senha)) {
                loginUser(email, senha, isAdmin = false)
            }
        }

        // Configura o botão de login para admin
        binding.entrarAdmin.setOnClickListener {
            val email = binding.txvEmailAdmin.text.toString().trim()
            val senha = binding.txvSenhaAdmin.text.toString().trim()

            if (validateInputs(email, senha)) {
                loginUser(email, senha, isAdmin = true)
            }
        }
    }

    private fun setupLoginType(isAdmin: Boolean) {
        if (isAdmin) {
            binding.layoutLoginUsuario.visibility = android.view.View.GONE
            binding.layoutLoginAdmin.visibility = android.view.View.VISIBLE
            binding.switchTipoLogin.text = "Sou usuário comum"
        } else {
            binding.layoutLoginUsuario.visibility = android.view.View.VISIBLE
            binding.layoutLoginAdmin.visibility = android.view.View.GONE
            binding.switchTipoLogin.text = "Sou administrador"
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor, insira sua senha", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String, isAdmin: Boolean) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login bem-sucedido
                    val user = auth.currentUser
                    Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()

                    // Aqui você pode verificar se o usuário é admin (talvez com um campo no Firestore)
                    // Por enquanto, vamos apenas passar o tipo como extra
                    val intent = if (isAdmin) {
                        Intent(this, Carro::class.java)
                    } else {
                        Intent(this, IntroCadastroCarro::class.java)
                    }

                    startActivity(intent)
                    finish()
                } else {
                    // Se o login falhar
                    Toast.makeText(
                        this,
                        "Falha no login: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // Verifica se o usuário já está logado quando a atividade é iniciada
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuário já está logado, redirecione para a tela apropriada
            // Você precisará verificar no seu banco de dados se é admin ou não
            val intent = Intent(this, IntroCadastroCarro::class.java)
            startActivity(intent)
            finish()
        }
    }
}