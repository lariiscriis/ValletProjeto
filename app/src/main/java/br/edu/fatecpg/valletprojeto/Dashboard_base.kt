package br.edu.fatecpg.valletprojeto

import ManagementFragment
import SpotsFragment
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import br.edu.fatecpg.valletprojeto.databinding.ActivityDashboardBaseBinding
import br.edu.fatecpg.valletprojeto.fragments.AdminFragment
import br.edu.fatecpg.valletprojeto.fragments.MotoristaFragment
//import br.edu.fatecpg.valletprojeto.R

class Dashboard_base : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBaseBinding
    private var isAdmin = false  // Mude para true para ver tela de admin

        fun isUserAdmin(): Boolean {
            return isAdmin
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a navegação
        setupNavigation()

        // Carrega o fragment inicial
        loadInitialFragment()
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_spots -> {
                    replaceFragment(SpotsFragment())
                    true
                }
                R.id.nav_dashboard -> {
                    if (isAdmin) replaceFragment(AdminFragment())
                    else replaceFragment(MotoristaFragment())
                    true
                }
                R.id.nav_management -> {
                    if (isAdmin) {
                        startActivity(Intent(this, EditarVagaActivity::class.java))
                    } else {
                        startActivity(Intent(this, EditarCarroActivity::class.java))
                    }
                    false
                }

                else -> false
            }
        }
    }

    private fun loadInitialFragment() {
        if (isAdmin) {
            replaceFragment(AdminFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
        } else {
            replaceFragment(MotoristaFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}