package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        setupUI()
    }

    private fun setupUI() {
        setupLoginType()
        setupWindowInsets()
        setupListeners()
    }

    private fun setupLoginType() {
        if (isAdmin) {
            binding.layoutLoginUsuario.visibility = View.GONE
            binding.layoutLoginAdmin.visibility = View.VISIBLE
            binding.switchTipoLogin.text = "Sou motorista"
        } else {
            binding.layoutLoginUsuario.visibility = View.VISIBLE
            binding.layoutLoginAdmin.visibility = View.GONE
            binding.switchTipoLogin.text = "Sou administrador"
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        binding.switchTipoLogin.setOnClickListener { toggleLoginType() }

        binding.botaoCadastro.setOnClickListener { navigateToCadastro("usuario") }
        binding.botaoCadastroAdmin.setOnClickListener { navigateToCadastro("admin") }

        binding.button3.setOnClickListener {
            val email = binding.editTextText.text.toString().trim()
            val senha = binding.editTextSenha.text.toString().trim()
            if (validateCredentials(email, senha)) {
                loginUser(email, senha, false)
            }
        }

        binding.entrarAdmin.setOnClickListener {
            val email = binding.edtEmailAdmin.text.toString().trim()
            val senha = binding.edtSenhaAdmin.text.toString().trim()
            if (validateCredentials(email, senha)) {
                loginUser(email, senha, true)
            }
        }
    }

    private fun toggleLoginType() {
        isAdmin = !isAdmin
        setupLoginType()
    }

    private fun navigateToCadastro(tipo: String) {
        val intent = Intent(this, CadastroActivity::class.java)
        intent.putExtra("tipoCadastro", tipo)
        startActivity(intent)
    }

    private fun validateCredentials(email: String, senha: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Formato de email inválido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (senha.length < 6) {
            Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun loginUser(email: String, senha: String, isAdminAttempt: Boolean) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                val user = auth.currentUser
                if (user != null) {
                    checkUserType(user.uid, email, isAdminAttempt)
                } else {
                    Toast.makeText(this, "Erro ao obter usuário", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e -> handleLoginError(e) }
    }

    private fun checkUserType(uid: String, email: String, isAdminAttempt: Boolean) {
        db.collection("usuario").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val tipoUser = document.getString("tipo_user") ?: "usuario"
                    val isAdminFromDB = tipoUser == "admin"
                    val primeiroAcesso = document.getBoolean("primeiroAcesso") ?: true

                    if (isAdminAttempt && !isAdminFromDB) {
                        auth.signOut()
                        Toast.makeText(this, "Acesso restrito a administradores", Toast.LENGTH_LONG).show()
                    } else {
                        if (primeiroAcesso) {
                            db.collection("usuario").document(uid)
                                .update("primeiroAcesso", false)
                            redirectToIntro(tipoUser, email)
                        } else {
                            if (tipoUser == "admin") {
                                checkEstacionamentoCadastrado(uid, email)
                            } else {
                                redirectToHome(tipoUser, email)
                            }
                        }
                    }
                } else {
                    auth.signOut()
                    Toast.makeText(this, "Usuário não encontrado no banco de dados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao buscar tipo de usuário: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkEstacionamentoCadastrado(uid: String, email: String) {
        db.collection("estacionamento")
            .whereEqualTo("adminUid", uid)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    // Admin sem estacionamento → ir para cadastro
                    val intent = Intent(this, CadastroEstacionamento::class.java)
                    intent.putExtra("email_usuario", email)
                    startActivity(intent)
                    finish()
                } else {
                    // Admin já tem estacionamento → ir para dashboard
                    redirectToHome("admin", email)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao verificar estacionamento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun redirectToIntro(tipoUser: String, email: String) {
        val intent = if (tipoUser == "admin") {
            Intent(this, IntroCadastroEstacionamento::class.java)
        } else {
            Intent(this, IntroCadastroCarro::class.java)
        }
        intent.putExtra("email_usuario", email)
        startActivity(intent)
        finish()
    }

    private fun redirectToHome(tipoUser: String, email: String) {
        val intent = if (tipoUser == "admin") {
            Intent(this, Dashboard_base::class.java)
        } else {
            Intent(this, Dashboard_base::class.java)
        }
        intent.putExtra("email_usuario", email)
        startActivity(intent)
        finish()
    }

    private fun handleLoginError(e: Exception?) {
        val message = when {
            e?.message?.contains("badly formatted", true) == true -> "Email inválido"
            e?.message?.contains("password is invalid", true) == true -> "Senha incorreta"
            e?.message?.contains("no user record", true) == true -> "Usuário não encontrado"
            else -> "Erro no login: ${e?.message}"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        user?.let {
            checkUserType(it.uid, it.email ?: "", false)
        }
    }
}
