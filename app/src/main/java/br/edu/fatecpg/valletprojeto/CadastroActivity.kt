package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.LoginActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroBinding
import br.edu.fatecpg.valletprojeto.model.Usuario
import br.edu.fatecpg.valletprojeto.viewmodel.CadastroViewModel

class CadastroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCadastroBinding
    private val viewModel: CadastroViewModel by viewModels()
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAdmin = intent.getStringExtra("tipoCadastro") == "admin"
        atualizarLayoutCadastro()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.switchTipoCadastro.setOnClickListener {
            isAdmin = !isAdmin
            atualizarLayoutCadastro()
        }

        binding.botaoLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("tipoCadastro", "usuario")
            startActivity(intent)
        }

        binding.botaoLoginAdmin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("tipoCadastro", "admin")
            startActivity(intent)
        }

        binding.btnCadastrar.setOnClickListener {
            val usuario = Usuario(
                email = binding.edtEmail.text.toString().trim(),
                cnh = binding.edtCnh.text.toString().trim(),
                nome = binding.edtNome.text.toString().trim(),
                senha = binding.edtSenha.text.toString().trim(),
                tipo_user = "motorista"
            )
            cadastrar(usuario)
        }

        binding.btnCadastrarAdmin.setOnClickListener {
            val usuario = Usuario(
                email = binding.edtEmailAdmin.text.toString().trim(),
                nome = binding.edtNomeEmpresa.text.toString().trim(),
                senha = binding.edtSenhaAdmin.text.toString().trim(),
                tipo_user = "admin",
                nome_empresa = binding.edtNomeEmpresa.text.toString().trim(),
                cargo = binding.edtCargoAdmin.text.toString().trim()
            )
            cadastrar(usuario)
        }
    }

    private fun cadastrar(usuario: Usuario) {
        viewModel.cadastrarUsuario(
            usuario,
            onSuccess = {
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("tipoCadastro", usuario.tipo_user)
                startActivity(intent)
                finish()
            },
            onError = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }


    private fun atualizarLayoutCadastro() {
        if (isAdmin) {
            binding.layoutCadastroUsuario.visibility = View.GONE
            binding.layoutCadastroAdmin.visibility = View.VISIBLE
            binding.switchTipoCadastro.text = "Sou motorista"
        } else {
            binding.layoutCadastroUsuario.visibility = View.VISIBLE
            binding.layoutCadastroAdmin.visibility = View.GONE
            binding.switchTipoCadastro.text = "Sou administrador"
        }
    }

}
