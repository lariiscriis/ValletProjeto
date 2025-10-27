package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.adapter.VagasAdapter
import br.edu.fatecpg.valletprojeto.databinding.ActivityVagaBinding
import br.edu.fatecpg.valletprojeto.model.Vaga
import br.edu.fatecpg.valletprojeto.viewmodel.VagaViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VagaActivity : AppCompatActivity() {

    private lateinit var viewModel: VagaViewModel
    private lateinit var binding: ActivityVagaBinding
    private var isAdmin: Boolean = false
    private var estacionamentoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVagaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[VagaViewModel::class.java]

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        estacionamentoId = intent.getStringExtra("estacionamentoId")

        if (userId != null) {
            db.collection("usuario").document(userId).get()
                .addOnSuccessListener { document ->
                    val tipo = document.getString("tipo_user")
                    isAdmin = tipo == "admin"

                    if (isAdmin) {
                        val emailAdmin = FirebaseAuth.getInstance().currentUser?.email
                        if (emailAdmin != null) {
                            db.collection("estacionamento")
                                .whereEqualTo("adminEmail", emailAdmin)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (!querySnapshot.isEmpty) {
                                        estacionamentoId = querySnapshot.documents[0].id
                                        setupUI()
                                    } else {
                                        Toast.makeText(this, "Estacionamento não encontrado para este admin", Toast.LENGTH_SHORT).show()
                                        setupUI()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Erro ao buscar estacionamento do admin", Toast.LENGTH_SHORT).show()
                                    setupUI()
                                }
                        } else {
                            Toast.makeText(this, "E-mail do admin não encontrado", Toast.LENGTH_SHORT).show()
                            setupUI()
                        }
                    } else {
                        setupUI()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao verificar tipo de usuário", Toast.LENGTH_SHORT).show()
                    setupUI()
                }
        } else {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            setupUI()
        }
    }

    private fun setupUI() {
        setupListeners()
        setupRecyclerView()
        setupObservers()

        if (estacionamentoId == null) {
            Toast.makeText(this, "Erro: ID do estacionamento não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.fetchVagasPorEstacionamento(estacionamentoId!!)
    }

    private fun setupListeners() {
        if (isAdmin) {
            binding.fabAdd.visibility = View.VISIBLE
            binding.fabAdd.setOnClickListener {
                estacionamentoId?.let {
                    val intent = Intent(this, CadastroVagaActivity::class.java)
                    intent.putExtra("estacionamentoId", it)
                    startActivity(intent)
                } ?: run {
                    Toast.makeText(this, "Estacionamento não encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.fabAdd.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupObservers() {
        viewModel.vagas.observe(this) { vagas ->
            binding.recyclerView.adapter = VagasAdapter(
                vagas,
                isAdmin = isAdmin,
                onEditClick = { vaga ->
                    startActivity(Intent(this, EditarVagaActivity::class.java).apply {
                        putExtra("vagaId", vaga.id)
                    })
                },
                onDeleteClick = { vaga ->
                    showDeleteDialog(vaga)
                }
            )
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteDialog(vaga: Vaga) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Vaga")
            .setMessage("Tem certeza que deseja excluir esta vaga?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.deleteVaga(vaga.id) { success, message ->
                    if (success) {
                        Toast.makeText(this, "Vaga excluída com sucesso", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, message ?: "Erro ao excluir vaga", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
