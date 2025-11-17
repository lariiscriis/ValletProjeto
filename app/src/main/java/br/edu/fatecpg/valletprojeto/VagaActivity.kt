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
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VagaActivity : AppCompatActivity() {

    private lateinit var viewModel: VagaViewModel
    private lateinit var binding: ActivityVagaBinding
    private lateinit var vagasAdapter: VagasAdapter
    private var isAdmin: Boolean = false
    private var estacionamentoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVagaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[VagaViewModel::class.java]
        estacionamentoId = intent.getStringExtra("estacionamentoId")

        if (estacionamentoId == null) {
            Toast.makeText(this, "Erro: ID do estacionamento não fornecido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        verificarTipoUsuario()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupObservers()
        setupFilterListeners()
        setupAdminFeatures()
        viewModel.fetchVagasComFiltro(estacionamentoId!!)
    }

    private fun verificarTipoUsuario() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            isAdmin = false
            setupUI()
            return
        }

        FirebaseFirestore.getInstance().collection("usuario").document(userId).get()
            .addOnSuccessListener { document ->
                isAdmin = document.getString("tipo_user") == "admin"
                setupUI()
            }
            .addOnFailureListener {
                isAdmin = false
                setupUI()
            }
    }

    private fun setupRecyclerView() {
        vagasAdapter = VagasAdapter(
            isAdmin = this.isAdmin,
            onEditClick = { vaga ->
                startActivity(Intent(this, EditarVagaActivity::class.java).apply {
                    putExtra("vagaId", vaga.id)
                })
            },
            onDeleteClick = { vaga ->
                showDeleteDialog(vaga)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = vagasAdapter
    }

    private fun setupObservers() {
        viewModel.vagas.observe(this) { vagas ->
            vagasAdapter.submitList(vagas)
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.veiculoPadraoTipo.observe(this) { tipo ->
            if (binding.chipGroupTipoVaga.checkedChipId == View.NO_ID) {
                when (tipo?.lowercase()) {
                    "carro" -> binding.chipCarro.isChecked = true
                    "moto" -> binding.chipMoto.isChecked = true
                    else -> binding.chipTodos.isChecked = true
                }
            }
        }
    }

    private fun setupFilterListeners() {
        binding.chipGroupTipoVaga.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener
            val chip = group.findViewById<Chip>(checkedId)
            viewModel.fetchVagasComFiltro(estacionamentoId!!, tipoFiltro = chip.text.toString())
        }
    }

    private fun setupAdminFeatures() {
        if (isAdmin) {
            binding.fabAdd.visibility = View.VISIBLE
            binding.fabAdd.setOnClickListener {
                val intent = Intent(this, CadastroVagaActivity::class.java)
                intent.putExtra("estacionamentoId", estacionamentoId)
                startActivity(intent)
            }
        } else {
            binding.fabAdd.visibility = View.GONE
        }
    }

    private fun showDeleteDialog(vaga: Vaga) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Vaga")
            .setMessage("Tem certeza que deseja excluir esta vaga?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.deleteVaga(vaga.id) { success, message ->
                    val toastMessage = if (success) "Vaga excluída com sucesso" else message ?: "Erro ao excluir"
                    Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
