package br.edu.fatecpg.valletprojeto

import SpotsFragment
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import br.edu.fatecpg.valletprojeto.fragments.AdminFragment
import br.edu.fatecpg.valletprojeto.fragments.MotoristaFragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import br.edu.fatecpg.valletprojeto.databinding.ActivityDashboardBaseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DashboardBase : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBaseBinding
    private lateinit var auth: FirebaseAuth
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        setupUI()
        verificarTipoUsuarioEConfigurarTela()
    }

    private fun setupUI() {
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
        setupProfileClickListeners()
    }

    private fun verificarTipoUsuarioEConfigurarTela() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val email = auth.currentUser?.email ?: throw Exception("Email do usuário não encontrado.")
                val documents = FirebaseFirestore.getInstance().collection("usuario")
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                if (documents.isEmpty) {
                    throw Exception("Perfil de usuário não encontrado no Firestore.")
                }

                val userDoc = documents.first()
                val tipo = userDoc.getString("tipo_user") ?: "motorista"
                isAdmin = tipo == "admin"

                val nome = userDoc.getString("nome") ?: "Usuário"
                val fotoUrl = userDoc.getString("fotoPerfil")

                // Toda atualização de UI deve ser feita na thread principal
                withContext(Dispatchers.Main) {
                    // Carrega os dados na UI
                    findViewById<TextView>(R.id.tv_user_name).text = nome
                    findViewById<TextView>(R.id.tv_account_type).text = tipo.replaceFirstChar { it.uppercase() }

                    if (!fotoUrl.isNullOrEmpty()) {
                        Glide.with(this@DashboardBase)
                            .load(fotoUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .into(findViewById(R.id.iv_profile_image))
                    }

                    setupNavigation()
                    loadInitialFragment()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardBase, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
                    performLogout()
                }
            }
        }
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
    }

    private fun setupNavigation() {
        val menu = binding.bottomNavigation.menu

        val vehiclesItem = menu.findItem(R.id.nav_vehicles)
        val managementItem = menu.findItem(R.id.nav_management)

        if (isAdmin) {
            // Admin vê TUDO.
            vehiclesItem.isVisible = true
            managementItem.isVisible = true
        } else {
            vehiclesItem.isVisible = true
            managementItem.isVisible = false
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_spots -> {
                    replaceFragment(SpotsFragment())
                    true
                }
                R.id.nav_dashboard -> {
                    if (isAdmin) replaceFragment(AdminFragment()) else replaceFragment(MotoristaFragment())
                    true
                }
                R.id.nav_vehicles -> {
                    startActivity(Intent(this, CarroActivity::class.java))
                    false
                }
                R.id.nav_management -> {
                    startActivity(Intent(this, VagaActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }



    private fun loadInitialFragment() {
        if (isAdmin) {
            replaceFragment(AdminFragment())
        } else {
            replaceFragment(MotoristaFragment())
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
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
        redirectToLogin()
    }
}
