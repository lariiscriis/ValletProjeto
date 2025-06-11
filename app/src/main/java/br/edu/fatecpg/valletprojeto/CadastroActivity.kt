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
import com.google.firebase.firestore.firestore

class CadastroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCadastroBinding
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tipoCadastro = intent.getStringExtra("tipoCadastro")
        isAdmin = tipoCadastro == "admin"

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

        // Botões de login
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

        // Botões de cadastro
        binding.btnCadastrar.setOnClickListener {
            cadastrarMotorista(
                email = binding.edtEmail.text.toString(),
                cnh = binding.edtCnh.text.toString(),
                nome = binding.edtNome.text.toString(),
                senha = binding.edtSenha.text.toString()
            )
        }

        binding.btnCadastrarAdmin.setOnClickListener {
            cadastrarAdmin(
                email = binding.edtEmailAdmin.text.toString(),
                nomeEmpresa = binding.edtNomeEmpresa.text.toString(),
                cargo = binding.edtCargoAdmin.text.toString(),
                senha = binding.edtSenhaAdmin.text.toString()
            )
        }
    }
    private fun atualizarLayoutCadastro() {
        if (isAdmin) {
            binding.layoutCadastroUsuario.visibility = android.view.View.GONE
            binding.layoutCadastroAdmin.visibility = android.view.View.VISIBLE
            binding.switchTipoCadastro.text = "Sou usuário comum"
        } else {
            binding.layoutCadastroUsuario.visibility = android.view.View.VISIBLE
            binding.layoutCadastroAdmin.visibility = android.view.View.GONE
            binding.switchTipoCadastro.text = "Sou administrador"
        }
    }
    private fun cadastrarMotorista(email: String, cnh: String, nome: String, senha: String) {
        val db = com.google.firebase.Firebase.firestore
        val emailTrim = email.trim()
        val cnhTrim = cnh.trim()
        val nomeTrim = nome.trim()

        if (emailTrim.isEmpty() || cnhTrim.isEmpty() || nomeTrim.isEmpty() || senha.isEmpty()) {
            showAlertDialog("Campo vazio", "Preencha todos os campos")
            return
        }

        if (cnhTrim.length != 11 || !cnhTrim.all { it.isDigit() }) {
            showAlertDialog("CNH inválida", "A CNH deve conter exatamente 11 dígitos numéricos.")
            return
        }

        db.collection("usuario").whereEqualTo("email", emailTrim).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    showAlertDialog("Erro", "Esse e-mail já está cadastrado.")
                } else {
                    val user = hashMapOf(
                        "email" to emailTrim,
                        "cnh" to cnhTrim,
                        "nome" to nomeTrim,
                        "senha" to senha,
                        "tipo_user" to "comum",
                        "data_criacao" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    db.collection("usuario").add(user)
                        .addOnSuccessListener {
                            showAlertDialog("Sucesso", "Usuário cadastrado com sucesso!") {
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.putExtra("tipoCadastro", "comum")
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            showAlertDialog("Erro", "Erro ao cadastrar: ${it.message}")
                        }
                }
            }
            .addOnFailureListener {
                showAlertDialog("Erro", "Erro ao verificar e-mail: ${it.message}")
            }
    }
    private fun cadastrarAdmin(email: String, nomeEmpresa: String, cargo: String, senha: String) {
        val db = com.google.firebase.Firebase.firestore
        val emailTrim = email.trim()
        val nomeEmpresaTrim = nomeEmpresa.trim()
        val cargoTrim = cargo.trim()

        if (emailTrim.isEmpty() || nomeEmpresaTrim.isEmpty() || cargoTrim.isEmpty() || senha.isEmpty()) {
            showAlertDialog("Campo vazio", "Preencha todos os campos")
            return
        }

        db.collection("usuario").whereEqualTo("email", emailTrim).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    showAlertDialog("Erro", "Esse e-mail já está cadastrado.")
                } else {
                    val user = hashMapOf(
                        "email" to emailTrim,
                        "nome_empresa" to nomeEmpresaTrim,
                        "cargo" to cargoTrim,
                        "senha" to senha,
                        "tipo_user" to "admin",
                        "data_criacao" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    db.collection("usuario").add(user)
                        .addOnSuccessListener {
                            showAlertDialog("Sucesso", "Administrador cadastrado com sucesso!") {
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.putExtra("tipoCadastro", "admin")
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            showAlertDialog("Erro", "Erro ao cadastrar: ${it.message}")
                        }
                }
            }
            .addOnFailureListener {
                showAlertDialog("Erro", "Erro ao verificar e-mail: ${it.message}")
            }
    }


    private fun showAlertDialog(title: String, message: String, onDismiss: (() -> Unit)? = null) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
        builder.create().show()
    }

}
