package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import br.edu.fatecpg.valletprojeto.databinding.ActivityDashboardBaseBinding
import br.edu.fatecpg.valletprojeto.fragments.AdminFragment
import br.edu.fatecpg.valletprojeto.fragments.MotoristaFragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import br.edu.fatecpg.valletprojeto.dao.CarroDao.auth
import br.edu.fatecpg.valletprojeto.fragments.SpotsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DashboardBase : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBaseBinding
    private var isAdmin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verificarTipoUsuario()
        carregarDadosUsuario()

        setupProfileClickListeners()
    }
    private fun setupProfileClickListeners() {
        val tvUserName = findViewById<TextView>(R.id.tv_user_name)
        val ivProfileImage = findViewById<ImageView>(R.id.iv_profile_image)

        val clickListener = {
            if (isAdmin) {
                startActivity(Intent(this, PerfilAdminActivity::class.java))
            } else {
                startActivity(Intent(this, PerfilMotoristaActivity::class.java))
            }
        }

        tvUserName.setOnClickListener { clickListener() }
        ivProfileImage.setOnClickListener { clickListener() }
        setupButtonListeners()
    }


    private fun verificarTipoUsuario() {
        val email = FirebaseAuth.getInstance().currentUser?.email

        if (email == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        FirebaseFirestore.getInstance().collection("usuario")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val tipo = document.getString("tipo_user") ?: "motorista"
                    isAdmin = tipo == "admin"
                    setupNavigation()
                    loadInitialFragment()
                } else {
                    Toast.makeText(this, "Perfil não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao verificar tipo de usuário.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupButtonListeners() {
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun carregarDadosUsuario() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val usuarioSnapshot = FirebaseFirestore.getInstance()
                    .collection("usuario")
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                if (!usuarioSnapshot.isEmpty) {
                    val userDoc = usuarioSnapshot.documents.first()
                    val nome = userDoc.getString("nome") ?: "Usuário"
                    val tipo = userDoc.getString("tipo_user") ?: "Motorista"
                    val saldo = userDoc.getDouble("saldo") ?: 0.0

                    findViewById<TextView>(R.id.tv_user_name).text = nome
                    findViewById<TextView>(R.id.tv_account_type).text = tipo.replaceFirstChar { it.uppercase() }

                    val fotoUrl = userDoc.getString("fotoPerfil")
                    if (!fotoUrl.isNullOrEmpty()) {
                        Glide.with(this@DashboardBase)
                            .load(fotoUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .into(findViewById(R.id.iv_profile_image))

                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_spots -> {
                    replaceFragment(SpotsFragment())
                    true
                }
                R.id.nav_dashboard -> {
                    when (isAdmin) {
                        true -> replaceFragment(AdminFragment())
                        false -> replaceFragment(MotoristaFragment())
                    }
                    true
                }
                R.id.nav_management -> {
                    if (isAdmin) {
                        val intent = Intent(this, VagaActivity::class.java)
                        startActivity(intent)
                        true
                    } else {
                        val intent = Intent(this, CarroActivity::class.java)
                        startActivity(intent)
                        false
                    }
                }

                else -> false
            }
        }
    }

    private fun loadInitialFragment() {
        when (isAdmin) {
            true -> {
                replaceFragment(AdminFragment())
                binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
            }
            false -> {
                replaceFragment(MotoristaFragment())
                binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_container, fragment)
            .commit()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Logout realizado com sucesso", Toast.LENGTH_SHORT).show()
        redirectToLogin()
    }

}

