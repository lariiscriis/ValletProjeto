package br.edu.fatecpg.valletprojeto

import ManagementFragment
import SpotsFragment
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Dashboard_base : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBaseBinding
    private var isAdmin = true  // Mude para true para ver tela de admin

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
            if (isAdmin == true) {
                // Abrir perfil do Admin
                startActivity(Intent(this, PerfilAdminActivity::class.java))
            } else if (isAdmin == false) {
                // Abrir perfil do Motorista
                startActivity(Intent(this, PerfilMotoristaActivity::class.java))
            } else {
                Toast.makeText(this, "Tipo de usuário indefinido.", Toast.LENGTH_SHORT).show()
            }
        }

        tvUserName.setOnClickListener { clickListener() }
        ivProfileImage.setOnClickListener { clickListener() }
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
                    val tipo = userDoc.getString("tipo") ?: "Motorista"
                    val saldo = userDoc.getDouble("saldo") ?: 0.0

                    findViewById<TextView>(R.id.tv_user_name).text = nome
                    findViewById<TextView>(R.id.tv_account_type).text = tipo.replaceFirstChar { it.uppercase() }
                    findViewById<TextView>(R.id.tv_balance).text = "R$ %.2f".format(saldo)

                    val fotoUrl = userDoc.getString("fotoPerfil")
                    if (!fotoUrl.isNullOrEmpty()) {
                        Glide.with(this@Dashboard_base)
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
                        else -> false
                    }
                    true
                }
                R.id.nav_management -> {
                    when (isAdmin) {
                        true -> replaceFragment(ManagementFragment())
                        false -> replaceFragment(SpotsFragment())
                        else -> false
                    }
                    true
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
            else -> {
                Toast.makeText(this, "Tipo de usuário indefinido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}

