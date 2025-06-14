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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVagaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[VagaViewModel::class.java]

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("usuario")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val tipo = document.getString("tipo_user")
                    isAdmin = tipo == "admin"

                    setupListeners()
                    setupRecyclerView()
                    setupObservers()
                    viewModel.fetchVagas()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao verificar tipo de usuário", Toast.LENGTH_SHORT).show()
                    isAdmin = false
                    setupListeners()
                    setupRecyclerView()
                    setupObservers()
                    viewModel.fetchVagas()
                }
        } else {
            // Caso não tenha usuário logado
            isAdmin = false
            setupListeners()
            setupRecyclerView()
            setupObservers()
            viewModel.fetchVagas()
        }


    }

    private fun setupListeners() {
        if (isAdmin) {
            binding.fabAdd.visibility = View.VISIBLE
            binding.fabAdd.setOnClickListener {
                val emailAdmin = FirebaseAuth.getInstance().currentUser?.email

                if (emailAdmin != null) {
                    FirebaseFirestore.getInstance().collection("estacionamento")
                        .whereEqualTo("adminEmail", emailAdmin)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val estacionamentoDoc = querySnapshot.documents[0]
                                val estacionamentoId = estacionamentoDoc.id

                                // Aqui sim, dentro do callback, você já pode iniciar a Activity
                                val intent = Intent(this, CadastroVagaActivity::class.java)
                                intent.putExtra("estacionamentoId", estacionamentoId)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "Estacionamento não encontrado para o admin", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao buscar estacionamento", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
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
