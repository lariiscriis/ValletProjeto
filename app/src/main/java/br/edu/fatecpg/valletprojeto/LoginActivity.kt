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

    private var isLoading = false
    private var currentLoadingStep = ""

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
        binding.switchTipoLogin.setOnClickListener {
            if (!isLoading) {
                toggleLoginType()
            }
        }

        binding.botaoCadastro.setOnClickListener {
            if (!isLoading) {
                navigateToCadastro("usuario")
            }
        }

        binding.botaoCadastroAdmin.setOnClickListener {
            if (!isLoading) {
                navigateToCadastro("admin")
            }
        }

        binding.button3.setOnClickListener {
            if (isLoading) return@setOnClickListener

            val email = binding.editTextText.text.toString().trim()
            val senha = binding.editTextSenha.text.toString().trim()

            if (validateCredentials(email, senha)) {
                startLoginProcess(email, senha, false)
            }
        }

        binding.entrarAdmin.setOnClickListener {
            if (isLoading) return@setOnClickListener

            val email = binding.edtEmailAdmin.text.toString().trim()
            val senha = binding.edtSenhaAdmin.text.toString().trim()

            if (validateCredentials(email, senha)) {
                startLoginProcess(email, senha, true)
            }
        }
    }

    private fun startLoginProcess(email: String, senha: String, isAdminAttempt: Boolean) {
        isLoading = true
        updateLoadingStep("Verificando credenciais...")
        showButtonLoading(isAdminAttempt)
        disableUI()

        loginUser(email, senha, isAdminAttempt)
    }

    private fun showButtonLoading(isAdmin: Boolean) {
        if (isAdmin) {
            binding.entrarAdmin.visibility = View.GONE
            binding.loadingButtonAdmin.visibility = View.VISIBLE
        } else {
            binding.button3.visibility = View.GONE
            binding.loadingButtonLogin.visibility = View.VISIBLE
        }
    }

    private fun hideButtonLoading(isAdmin: Boolean) {
        if (isAdmin) {
            binding.entrarAdmin.visibility = View.VISIBLE
            binding.loadingButtonAdmin.visibility = View.GONE
        } else {
            binding.button3.visibility = View.VISIBLE
            binding.loadingButtonLogin.visibility = View.GONE
        }
    }

    private fun updateLoadingStep(step: String) {
        currentLoadingStep = step
        binding.txvLoadingStep.text = step
        Log.d("LoginLoading", step)
    }

    private fun disableUI() {
        binding.switchTipoLogin.isEnabled = false
        binding.botaoCadastro.isEnabled = false
        binding.botaoCadastroAdmin.isEnabled = false
        binding.editTextText.isEnabled = false
        binding.editTextSenha.isEnabled = false
        binding.edtEmailAdmin.isEnabled = false
        binding.edtSenhaAdmin.isEnabled = false
        binding.edtCodigoAcesso.isEnabled = false
    }

    private fun enableUI() {
        binding.switchTipoLogin.isEnabled = true
        binding.botaoCadastro.isEnabled = true
        binding.botaoCadastroAdmin.isEnabled = true
        binding.editTextText.isEnabled = true
        binding.editTextSenha.isEnabled = true
        binding.edtEmailAdmin.isEnabled = true
        binding.edtSenhaAdmin.isEnabled = true
        binding.edtCodigoAcesso.isEnabled = true
    }

    private fun resetLoadingState(isAdminAttempt: Boolean) {
        isLoading = false
        hideButtonLoading(isAdminAttempt)
        enableUI()
        binding.loadingState.visibility = View.GONE
        binding.contentState.visibility = View.VISIBLE
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
        updateLoadingStep("Conectando ao servidor...")

        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                Log.d("Login", "✅ Firebase Auth OK: ${it.user?.uid}")
                updateLoadingStep("Autenticação realizada com sucesso!")
                val user = auth.currentUser
                if (user != null) {
                    checkUserType(user.uid, user.email ?: "", isAdminAttempt)
                } else {
                    resetLoadingState(isAdminAttempt)
                    Toast.makeText(this, "Erro ao obter usuário", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Login", "❌ Erro no Firebase Auth: ${e.message}")
                resetLoadingState(isAdminAttempt)
                handleLoginError(e)
            }
    }

    private fun checkUserType(uid: String, email: String, isAdminAttempt: Boolean) {
        Log.d("Firestore", "Buscando usuário por UID: $uid")
        updateLoadingStep("Buscando dados do usuário...")

        var isTimedOut = false
        val timeoutRunnable = Runnable {
            isTimedOut = true
            resetLoadingState(isAdminAttempt)

            AlertDialog.Builder(this)
                .setTitle("⏱️ Tempo Esgotado")
                .setMessage("A conexão está muito lenta. Deseja tentar novamente?")
                .setPositiveButton("Tentar Novamente") { _, _ ->
                    startLoginProcess(email, "", isAdminAttempt)
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

                updateLoadingStep("Processando informações...")

                if (!documentSnapshot.exists()) {
                    Log.e("Firestore", "❌ Nenhum usuário encontrado com UID: $uid")
                    resetLoadingState(isAdminAttempt)
                    handleUserNotFound()
                    return@addOnSuccessListener
                }

                Log.d("Firestore", "✅ Usuário encontrado! ID: ${documentSnapshot.id}")

                val tipoUser = documentSnapshot.getString("tipo_user") ?: "motorista"
                val isAdminFromDB = tipoUser == "admin"

                Log.d("Login", "🎯 Tipo de usuário: $tipoUser")

                if (isAdminAttempt && !isAdminFromDB) {
                    auth.signOut()
                    resetLoadingState(isAdminAttempt)
                    Toast.makeText(this, "❌ Acesso restrito a administradores", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (isAdminFromDB) {
                    updateLoadingStep("Verificando estacionamento...")
                    checkEstacionamentoCadastrado(uid, email)
                } else {
                    updateLoadingStep("Preparando ambiente...")
                    redirectToHome(uid, email)
                }
            }
            .addOnFailureListener { e ->
                mainHandler.removeCallbacks(timeoutRunnable)

                if (isTimedOut) {
                    Log.w("Firestore", "⚠️ Erro recebido após timeout")
                    return@addOnFailureListener
                }

                resetLoadingState(isAdminAttempt)
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
                        startLoginProcess(email, "", isAdminAttempt)
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
        updateLoadingStep("Carregando dados do estacionamento...")

        db.collection("estacionamento")
            .whereEqualTo("adminUid", uid)
            .get()
            .addOnSuccessListener { result ->
                resetLoadingState(true)

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
                resetLoadingState(true)
                Toast.makeText(this, "Erro ao verificar estacionamento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun redirectToHome(uid: String, email: String) {
        Log.d("LOGIN", "🚀 Iniciando redirecionamento para home")
        updateLoadingStep("Quase lá...")

        binding.contentState.visibility = View.GONE
        binding.loadingState.visibility = View.VISIBLE
        updateLoadingStep("Preparando sua área de trabalho...")

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, DashboardBase::class.java)
            intent.putExtra("email_usuario", email)
            startActivity(intent)
            finish()
        }, 1000)

        gerarESalvarTokenFCM(uid)
    }

    private fun gerarESalvarTokenFCM(uid: String) {
        Log.d("FCM", "🔄 Iniciando geração do token FCM para UID: $uid")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "✅ Token FCM gerado: ${token.take(10)}...")
                salvarTokenNoFirestore(uid, token)
            } else {
                Log.e("FCM", "❌ Falha ao gerar token FCM", task.exception)
                Handler(Looper.getMainLooper()).postDelayed({
                    gerarESalvarTokenFCM(uid)
                }, 3000)
            }
        }
    }

    private fun salvarTokenNoFirestore(uid: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val tokenData = hashMapOf(
            "fcm_token" to token,
            "ultima_atualizacao_token" to com.google.firebase.Timestamp.now()
        )

        db.collection("usuario").document(uid)
            .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FCM", "✅ Token FCM salvo com sucesso para UID: $uid")
                verificarTokenSalvo(uid)
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Erro ao salvar Token FCM", e)
                Handler(Looper.getMainLooper()).postDelayed({
                    salvarTokenNoFirestore(uid, token)
                }, 2000)
            }
    }

    private fun verificarTokenSalvo(uid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("usuario").document(uid).get()
            .addOnSuccessListener { document ->
                val tokenSalvo = document.getString("fcm_token")
                if (tokenSalvo != null) {
                    Log.d("FCM", "✅ Token confirmado no Firestore: ${tokenSalvo.take(10)}...")
                } else {
                    Log.e("FCM", "❌ Token NÃO foi salvo no Firestore!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Erro ao verificar token salvo", e)
            }
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

            binding.contentState.visibility = View.GONE
            binding.loadingState.visibility = View.VISIBLE
            updateLoadingStep("Restaurando sua sessão...")

            mainHandler.postDelayed({
                if (binding.loadingState.visibility == View.VISIBLE) {
                    resetLoadingState(false)

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
            binding.loadingState.visibility = View.GONE
            binding.contentState.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }
}
