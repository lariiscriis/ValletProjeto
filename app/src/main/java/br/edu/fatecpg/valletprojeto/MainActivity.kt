package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityIntroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntroBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.progressOverlay.visibility = View.VISIBLE
        binding.btnIniciar.visibility = View.GONE

        val user = auth.currentUser
        if (user != null) {
            checkUserTypeAndRedirect(user.uid, user.email ?: "")
        } else {
            binding.progressOverlay.visibility = View.GONE
            binding.btnIniciar.visibility = View.VISIBLE

            binding.btnIniciar.setOnClickListener {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun checkUserTypeAndRedirect(uid: String, email: String) {
        db.collection("usuario").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val tipoUser = document.getString("tipo_user") ?: "usuario"
                    if (tipoUser == "admin") {
                        checkEstacionamentoCadastrado(uid, email)
                    } else {
                        redirectToHome(tipoUser, email)
                    }
                } else {
                    auth.signOut()
                    goToLogin()
                }
            }
            .addOnFailureListener {
                auth.signOut()
                goToLogin()
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
                } else {
                    redirectToHome("admin", email)
                }
                finish()
            }
            .addOnFailureListener {
                redirectToHome("admin", email)
                finish()
            }
    }

    private fun redirectToHome(tipoUser: String, email: String) {
        val intent = Intent(this, DashboardBase::class.java)
        intent.putExtra("email_usuario", email)
        intent.putExtra("tipo_user", tipoUser)
        startActivity(intent)
        finish()
    }

    private fun goToLogin() {
        binding.progressOverlay.visibility = View.GONE
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
