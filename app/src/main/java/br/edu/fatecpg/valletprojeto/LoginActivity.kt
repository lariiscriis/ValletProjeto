package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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

        // Inicializa o Firebase Auth
        auth = Firebase.auth

        setupLoginType()
        setupWindowInsets()
        setupButtonListeners()
    }

    private fun setupLoginType() {
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

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupButtonListeners() {
        binding.botaoCarros.setOnClickListener {
            startActivity(Intent(this, IntroCadastroCarro::class.java))
        }

        binding.botaoCadastro.setOnClickListener {
            navigateToCadastro("usuario")
        }

        binding.botaoCadastroAdmin.setOnClickListener {
            navigateToCadastro("admin")
        }

        binding.switchTipoLogin.setOnClickListener {
            toggleLoginType()
        }

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

    private fun validateCredentials(email: String, password: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Formato de email inválido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "Senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String, isAdminAttempt: Boolean) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkAdminStatus(email, isAdminAttempt)
                } else {
                    handleLoginError(task.exception)
                }
            }
    }

    private fun checkAdminStatus(email: String, isAdminAttempt: Boolean) {
        db.collection("usuario")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val tipoUser = doc.getString("tipo_user")

                    val isAdminFromDB = tipoUser == "admin"

                    if (isAdminAttempt && !isAdminFromDB) {
                        auth.signOut()
                        Toast.makeText(
                            this,
                            "Acesso restrito a administradores cadastrados",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        redirectUser(isAdminFromDB, email)
                    }
                } else {
                    auth.signOut()
                    Toast.makeText(
                        this,
                        "Usuário não encontrado no banco de dados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Erro ao verificar permissões: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun redirectUser(isAdmin: Boolean, email:String) {
        val intent = if (isAdmin) {
            Intent(this, EditarPerfilAdministradorActivity::class.java)
        } else {
            Intent(this, IntroCadastroCarro::class.java)
        }
        intent.putExtra("email_usuario", email)
        startActivity(intent)
        finish()
    }

    private fun handleLoginError(exception: Exception?) {
        val errorMessage = when {
            exception?.message?.contains("email address is badly formatted") == true ->
                "Formato de email inválido"
            exception?.message?.contains("password is invalid") == true ->
                "Senha incorreta"
            exception?.message?.contains("no user record") == true ->
                "Usuário não encontrado"
            else -> "Erro no login: ${exception?.message}"
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun toggleLoginType() {
        isAdmin = !isAdmin
        setupLoginType()
    }

    private fun navigateToCadastro(userType: String) {
        val intent = Intent(this, CadastroActivity::class.java)
        intent.putExtra("tipoCadastro", userType)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let {
            // Verifica se o usuário logado é admin
            checkAdminStatus(it.email ?: "", false)
        }
    }
}
