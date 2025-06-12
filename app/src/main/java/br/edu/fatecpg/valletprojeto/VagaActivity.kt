package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.adapter.VagasAdapter
import br.edu.fatecpg.valletprojeto.databinding.ActivityVagaBinding
import br.edu.fatecpg.valletprojeto.model.Vaga
import br.edu.fatecpg.valletprojeto.viewmodel.VagaViewModel

class VagaActivity : AppCompatActivity() {
    private lateinit var viewModel: VagaViewModel
    private lateinit var binding: ActivityVagaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVagaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(VagaViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.fetchVagas()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupObservers() {
        viewModel.vagas.observe(this) { vagas ->
            binding.recyclerView.adapter = VagasAdapter(
                vagas,
                onEditClick = { vaga ->
                    startActivity(
                        Intent(this, EditarVagaActivity::class.java).apply {
                            putExtra("vagaId", vaga.id)
                        }
                    )
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

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, EditarVagaActivity::class.java))
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