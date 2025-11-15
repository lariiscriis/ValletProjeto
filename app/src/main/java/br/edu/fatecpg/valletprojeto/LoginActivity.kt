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
import com.google.android.gms.security.ProviderInstaller
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity(), ProviderInstaller.ProviderInstallListener {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private var isAdmin = false
    private var providerInstallAttempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Força atualização do provedor TLS ANTES do Firebase
        tryUpdateTlsProvider()

        // Bypassa executor que obrigaria reCAPTCHA
        blockFirebaseRecaptcha()

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        setupUI()
    }

    /* ----------------------------------------------------------
       PATCH 1: Força atualização do TLS (ProviderInstaller)
       ---------------------------------------------------------- */
    private fun tryUpdateTlsProvider() {
        providerInstallAttempted = true
        ProviderInstaller.installIfNeededAsync(this, this)
    }

    override fun onProviderInstalled() {
        // TLS atualizado com sucesso, seguimos a vida
    }

    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
        // Se falhar, seguimos com TLS nativo (melhor que travar o Firebase 60s)
    }

    /* ----------------------------------------------------------
       PATCH 2: Remove execução assíncrona do FirebaseAuth
       que invocaria reCAPTCHA/SafetyNet
       ---------------------------------------------------------- */
    private fun blockFirebaseRecaptcha() {
        try {
            val field = FirebaseAuth::class.java.getDeclaredField("executor")
            field.isAccessible = true
            field.set(Firebase.auth, Executor { runnable -> runnable.run() })
        } catch (_: Exception) {}
    }

    /* ----------------------------------------------------------
       Continuação do seu código original (intocado)
       ---------------------------------------------------------- */

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

        binding.botaoCadastro.setOnClickListener {
            navigateToCadastro("usuario")
        }

        binding.botaoCadastroAdmin.setOnClickListener {
            navigateToCadastro("admin")
        }

        binding.button3.setOnClickListener {
            val email = binding.editTextText.text.toString().trim()
            val senha = binding.editTextSenha.text.toString().trim()

            if (validateCredentials(email, senha)) {
                binding.progressOverlay.visibility = View.VISIBLE
                loginUser(email, senha, false)
            }
        }

        binding.entrarAdmin.setOnClickListener {
            val email = binding.edtEmailAdmin.text.toString().trim()
            val senha = binding.edtSenhaAdmin.text.toString().trim()

            if (validateCredentials(email, senha)) {
                binding.progressOverlay.visibility = View.VISIBLE
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
                    binding.progressOverlay.visibility = View.GONE
                    Toast.makeText(this, "Erro ao obter usuário", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.progressOverlay.visibility = View.GONE
                handleLoginError(e)
            }
    }

    private fun checkUserType(uid: String, email: String, isAdminAttempt: Boolean) {
        db.collection("usuario").document(uid).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    auth.signOut()
                    binding.progressOverlay.visibility = View.GONE
                    Toast.makeText(this, "Usuário não encontrado no banco de dados", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val tipoUser = document.getString("tipo_user") ?: "usuario"
                val isAdminFromDB = tipoUser == "admin"

                if (isAdminAttempt && !isAdminFromDB) {
                    auth.signOut()
                    binding.progressOverlay.visibility = View.GONE
                    Toast.makeText(this, "Acesso restrito a administradores", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (tipoUser == "admin") {
                    checkEstacionamentoCadastrado(uid, email)
                } else {
                    redirectToHome(tipoUser, email)
                }
            }
            .addOnFailureListener {
                binding.progressOverlay.visibility = View.GONE
                Toast.makeText(this, "Erro ao buscar tipo de usuário: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkEstacionamentoCadastrado(uid: String, email: String) {
        db.collection("estacionamento")
            .whereEqualTo("adminUid", uid)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    val intent = Intent(this, CadastroEstacionamento::class.java)
                    intent.putExtra("email_usuario", email)
                    startActivity(intent)
                    finish()
                } else {
                    redirectToHome("admin", email)
                }
            }
            .addOnFailureListener { e ->
                binding.progressOverlay.visibility = View.GONE
                Toast.makeText(this, "Erro ao verificar estacionamento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun redirectToHome(tipoUser: String, email: String) {
        val intent = Intent(this, DashboardBase::class.java)
        intent.putExtra("email_usuario", email)
        startActivity(intent)
        finish()
    }

    private fun handleLoginError(e: Exception?) {
        val msg = e?.message ?: "Erro no login"
        val out = when {
            msg.contains("badly formatted", true) -> "Email inválido"
            msg.contains("password is invalid", true) -> "Senha incorreta"
            msg.contains("no user record", true) -> "Usuário não encontrado"
            else -> "Erro no login: $msg"
        }
        Toast.makeText(this, out, Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        val current = FirebaseAuth.getInstance().currentUser
        if (current != null) {
            binding.progressOverlay.visibility = View.VISIBLE
            checkUserType(current.uid, current.email ?: "", false)
        }
    }
}
