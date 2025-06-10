package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroBinding

class CadastroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCadastroBinding
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //codigo pra definir o tipo de linearLayout se é admin ou usuario
        val tipoCadastro = intent.getStringExtra("tipoCadastro")
        isAdmin = tipoCadastro == "admin"

        if(isAdmin){
            binding.layoutCadastroUsuario.visibility = android.view.View.GONE
            binding.layoutCadastroAdmin.visibility = android.view.View.VISIBLE
            binding.switchTipoCadastro.text = "Sou usuário comum"
        }
        else{
            binding.layoutCadastroUsuario.visibility = android.view.View.VISIBLE
            binding.layoutCadastroAdmin.visibility = android.view.View.GONE
            binding.switchTipoCadastro.text = "Sou administrador"
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.botaoLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("tipoCadastro", "usuario")
            startActivity(intent)
        }
        binding.botaoLoginAdmin.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("tipoCadastro", "admin")
            startActivity(intent)
        }

        binding.switchTipoCadastro.setOnClickListener{
            isAdmin = !isAdmin

            if(isAdmin){
                binding.layoutCadastroUsuario.visibility = android.view.View.GONE
                binding.layoutCadastroAdmin.visibility = android.view.View.VISIBLE
                binding.switchTipoCadastro.text = "Sou usuário comum"
            }
            else{
                binding.layoutCadastroUsuario.visibility = android.view.View.VISIBLE
                binding.layoutCadastroAdmin.visibility = android.view.View.GONE
                binding.switchTipoCadastro.text = "Sou administrador"
            }

        }
    }
}