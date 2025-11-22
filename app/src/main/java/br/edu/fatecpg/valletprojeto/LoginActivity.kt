package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executor
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity(), ProviderInstaller.ProviderInstallListener {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private var isAdmin = false
    private var providerInstallAttempted = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
            db.firestoreSettings = settings
            Log.d("Firestore", "✅ Persistência local do Firestore desabilitada.")
        } catch (e: Exception) {
            Log.e("Firestore", "❌ Erro ao configurar FirestoreSettings: ${e.message}")
        }

        tryUpdateTlsProvider()
        blockFirebaseRecaptcha()

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        setupUI()
    }

    private fun tryUpdateTlsProvider() {
        providerInstallAttempted = true
        ProviderInstaller.installIfNeededAsync(this, this)
    }

    override fun onProviderInstalled() {
        Log.d("ProviderInstaller", "✅ TLS Provider atualizado com sucesso")
    }

    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
        Log.w("ProviderInstaller", "⚠️ Falha ao atualizar TLS Provider: $errorCode")
    }

    private fun blockFirebaseRecaptcha() {
        try {
            val field = FirebaseAuth::class.java.getDeclaredField("executor")
            field.isAccessible = true
            field.set(Firebase.auth, Executor { runnable -> runnable.run() })
        } catch (_: Exception) {}
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
        if (email.isBlank() || senha.isBlank()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return false
        }

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
        Log.d("Login", "Tentando login: $email")

        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                Log.d("Login", "✅ Firebase Auth OK: ${it.user?.uid}")
                val user = auth.currentUser
                if (user != null) {
                    checkUserType(user.uid, user.email ?: "", isAdminAttempt)
                } else {
                    binding.progressOverlay.visibility = View.GONE
                    Toast.makeText(this, "Erro ao obter usuário", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Login", "❌ Erro no Firebase Auth: ${e.message}")
                binding.progressOverlay.visibility = View.GONE
                handleLoginError(e)
            }
    }

    private fun checkUserType(uid: String, email: String, isAdminAttempt: Boolean) {
        Log.d("Firestore", "Buscando usuário por UID: $uid")

        var isTimedOut = false
        val timeoutRunnable = Runnable {
            isTimedOut = true
            binding.progressOverlay.visibility = View.GONE

            AlertDialog.Builder(this)
                .setTitle("⏱️ Tempo Esgotado")
                .setMessage("A conexão está muito lenta. Deseja tentar novamente?")
                .setPositiveButton("Tentar Novamente") { _, _ ->
                    binding.progressOverlay.visibility = View.VISIBLE
                    checkUserType(uid, email, isAdminAttempt)
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    auth.signOut()
                }
                .setCancelable(false)
                .show()
        }

        mainHandler.postDelayed(timeoutRunnable, 20000)

        db.collection("usuario").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                mainHandler.removeCallbacks(timeoutRunnable)

                if (isTimedOut) {
                    Log.w("Firestore", "⚠️ Resposta recebida após timeout")
                    return@addOnSuccessListener
                }

                binding.progressOverlay.visibility = View.GONE

                if (!documentSnapshot.exists()) {
                    Log.e("Firestore", "❌ Nenhum usuário encontrado com UID: $uid")
                    handleUserNotFound()
                    return@addOnSuccessListener
                }

                Log.d("Firestore", "✅ Usuário encontrado! ID: ${documentSnapshot.id}")

                val tipoUser = documentSnapshot.getString("tipo_user") ?: "motorista"
                val isAdminFromDB = tipoUser == "admin"

                Log.d("Login", "🎯 Tipo de usuário: $tipoUser")

                if (isAdminAttempt && !isAdminFromDB) {
                    auth.signOut()
                    Toast.makeText(this, "❌ Acesso restrito a administradores", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (isAdminFromDB) {
                    checkEstacionamentoCadastrado(uid, email)
                } else {
                    redirectToHome(tipoUser, email)
                }
            }
            .addOnFailureListener { e ->
                mainHandler.removeCallbacks(timeoutRunnable)

                if (isTimedOut) {
                    Log.w("Firestore", "⚠️ Erro recebido após timeout")
                    return@addOnFailureListener
                }

                binding.progressOverlay.visibility = View.GONE
                Log.e("Firestore", "❌ Erro na consulta: ${e.message}")

                val errorMsg = when {
                    e.message?.contains("offline", ignoreCase = true) == true ->
                        "Sem conexão com a internet"
                    e.message?.contains("permission", ignoreCase = true) == true ->
                        "🔒 Permissão negada. Verifique as regras de segurança."
                    e.message?.contains("deadline", ignoreCase = true) == true ->
                        "Servidor não respondeu a tempo"
                    else -> "Erro ao conectar: ${e.message}"
                }

                AlertDialog.Builder(this)
                    .setTitle("Erro de Conexão")
                    .setMessage("$errorMsg\n\nDeseja tentar novamente?")
                    .setPositiveButton("Tentar Novamente") { _, _ ->
                        binding.progressOverlay.visibility = View.VISIBLE
                        checkUserType(uid, email, isAdminAttempt)
                    }
                    .setNegativeButton("Cancelar") { _, _ ->
                        auth.signOut()
                    }
                    .setCancelable(false)
                    .show()
            }
    }

    private fun handleUserNotFound() {
        auth.signOut()

        AlertDialog.Builder(this)
            .setTitle("Usuário Não Encontrado")
            .setMessage("Não foi possível encontrar seus dados no sistema. Deseja fazer o cadastro?")
            .setPositiveButton("Fazer Cadastro") { _, _ ->
                navigateToCadastro("usuario")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun checkEstacionamentoCadastrado(uid: String, email: String) {
        db.collection("estacionamento")
            .whereEqualTo("adminUid", uid)
            .get()
            .addOnSuccessListener { result ->
                binding.progressOverlay.visibility = View.GONE

                if (result.isEmpty) {
                    val intent = Intent(this, CadastroEstacionamento::class.java)
                    intent.putExtra("email_usuario", email)
                    startActivity(intent)
                    finish()
                } else {
                    redirectToHome(uid, email)
                }
            }
            .addOnFailureListener { e ->
                binding.progressOverlay.visibility = View.GONE
                Toast.makeText(this, "Erro ao verificar estacionamento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun redirectToHome(uid: String, email: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("LOGIN", "Falha ao obter token FCM", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val db = FirebaseFirestore.getInstance()
            val tokenData = hashMapOf("fcm_token" to token)

            db.collection("usuario").document(uid)
                .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("LOGIN", "Token FCM salvo com sucesso após o login.")
                }
                .addOnFailureListener { e ->
                    Log.e("LOGIN", "Erro ao salvar Token FCM após o login.", e)
                }
        }

        Log.d("Login", "🚀 Redirecionando para home")
        val intent = Intent(this, DashboardBase::class.java)
        intent.putExtra("email_usuario", email)
        startActivity(intent)
        finish()
    }

    private fun handleLoginError(e: Exception?) {
        val msg = e?.message ?: "Erro no login"
        val out = when {
            msg.contains("badly formatted", ignoreCase = true) -> "Email inválido"
            msg.contains("password is invalid", ignoreCase = true) -> "Senha incorreta"
            msg.contains("no user record", ignoreCase = true) -> "Usuário não encontrado"
            msg.contains("network", ignoreCase = true) -> "Erro de conexão. Verifique sua internet"
            msg.contains("too many requests", ignoreCase = true) -> "Muitas tentativas. Aguarde um momento"
            else -> "Erro no login: $msg"
        }
        Toast.makeText(this, out, Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        val current = FirebaseAuth.getInstance().currentUser

        if (current != null) {
            Log.d("Login", "🔄 Usuário já logado, verificando sessão...")
            binding.progressOverlay.visibility = View.VISIBLE

            mainHandler.postDelayed({
                if (binding.progressOverlay.visibility == View.VISIBLE) {
                    binding.progressOverlay.visibility = View.GONE

                    AlertDialog.Builder(this)
                        .setTitle("Sessão Expirada")
                        .setMessage("Não foi possível restaurar sua sessão. Faça login novamente.")
                        .setPositiveButton("OK") { _, _ ->
                            auth.signOut()
                        }
                        .setCancelable(false)
                        .show()
                }
            }, 25000)

            checkUserType(current.uid, current.email ?: "", false)
        } else {
            binding.progressOverlay.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }
}
