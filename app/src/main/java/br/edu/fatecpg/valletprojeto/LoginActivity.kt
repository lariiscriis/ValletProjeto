package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityIntroBinding
import br.edu.fatecpg.valletprojeto.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tipoCadastro = intent.getStringExtra("tipoCadastro")
        isAdmin = tipoCadastro == "admin"

        if(isAdmin){
            binding.layoutLoginUsuario.visibility = android.view.View.GONE
            binding.layoutLoginAdmin.visibility = android.view.View.VISIBLE
            binding.switchTipoLogin.text = "Sou usuário comum"
        }
        else{
            binding.layoutLoginUsuario.visibility = android.view.View.VISIBLE
            binding.layoutLoginAdmin.visibility = android.view.View.GONE
            binding.switchTipoLogin.text = "Sou administrador"
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.botaoCarros.setOnClickListener {
            val intent = Intent(this, IntroCadastroCarro::class.java)
            startActivity(intent)
        }

        binding.botaoCadastro.setOnClickListener {
            val intent = Intent(this, CadastroActivity::class.java)
            intent.putExtra("tipoCadastro", "usuario")
            startActivity(intent)
        }

        binding.botaoCadastroAdmin.setOnClickListener{
            val intent = Intent(this, CadastroActivity::class.java)
            intent.putExtra("tipoCadastro", "admin")
            startActivity(intent)
        }

        binding.switchTipoLogin.setOnClickListener{
            isAdmin = !isAdmin

            if(isAdmin){
                binding.layoutLoginUsuario.visibility = android.view.View.GONE
                binding.layoutLoginAdmin.visibility = android.view.View.VISIBLE
                binding.switchTipoLogin.text = "Sou usuário comum"

            }
            else{
                binding.layoutLoginUsuario.visibility = android.view.View.VISIBLE
                binding.layoutLoginAdmin.visibility = android.view.View.GONE
                binding.switchTipoLogin.text = "Sou administrador"
            }
        }


    }
}