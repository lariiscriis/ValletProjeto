package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.adapter.VagasAdapter
import br.edu.fatecpg.valletprojeto.databinding.ActivityVagaBinding
import br.edu.fatecpg.valletprojeto.model.Vaga
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaUIState
import br.edu.fatecpg.valletprojeto.viewmodel.ReservaViewModel
import br.edu.fatecpg.valletprojeto.viewmodel.VagaViewModel
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VagaActivity : AppCompatActivity() {

    private lateinit var viewModel: VagaViewModel
    private lateinit var reservaViewModel: ReservaViewModel
    private lateinit var binding: ActivityVagaBinding
    private lateinit var vagasAdapter: VagasAdapter
    private var isAdmin: Boolean = false
    private var estacionamentoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVagaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[VagaViewModel::class.java]
        reservaViewModel = ViewModelProvider(this)[ReservaViewModel::class.java]
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
        setupReservaObserver()
        viewModel.fetchVagasComFiltro(estacionamentoId!!)

        reservaViewModel.verificarReservaAtiva()
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

    private fun setupReservaObserver() {
        reservaViewModel.temReservaAtiva.observe(this) { temReserva ->
            if (temReserva) {
                binding.recyclerView.visibility = View.GONE
                binding.chipGroupTipoVaga.visibility = View.GONE

                mostrarTelaBloqueio()
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.chipGroupTipoVaga.visibility = View.VISIBLE
                esconderTelaBloqueio()

                viewModel.fetchVagasComFiltro(estacionamentoId!!)
            }
        }

    }

    private fun mostrarTelaBloqueio() {
        val existingBloqueio = findViewById<View>(R.id.layoutBloqueio)
        if (existingBloqueio != null) {
            binding.root.removeView(existingBloqueio)
        }

        val layoutBloqueio = View.inflate(this, R.layout.layout_bloqueio_reserva, null)
        layoutBloqueio.id = R.id.layoutBloqueio
        binding.root.addView(layoutBloqueio)

        val btnVerReserva = layoutBloqueio.findViewById<Button>(R.id.btnVerReserva)
        val textBloqueio = layoutBloqueio.findViewById<TextView>(R.id.textBloqueio)

        reservaViewModel.uiState.observe(this) { uiState ->
            when (uiState) {
                is ReservaUIState.Active -> {
                    val mensagem = "Você já tem uma reserva ativa na vaga ${uiState.vaga.numero}.\n\nFinalize sua reserva atual antes de reservar outra vaga."
                    textBloqueio.text = mensagem

                    btnVerReserva.setOnClickListener {
                        val intent = Intent(this, ReservaActivity::class.java).apply {
                            putExtra("vagaId", uiState.reserva.vagaId)
                            putExtra("estacionamentoId", uiState.reserva.estacionamentoId)
                        }
                        startActivity(intent)
                    }
                }
                else -> {
                    textBloqueio.text = "Você já tem uma reserva ativa em andamento.\n\nFinalize sua reserva atual antes de reservar outra vaga."

                    btnVerReserva.setOnClickListener {
                        reservaViewModel.verificarReservaAtiva()
                        Toast.makeText(this, "Buscando reserva ativa...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun esconderTelaBloqueio() {
        val layoutBloqueio = findViewById<View>(R.id.layoutBloqueio)
        layoutBloqueio?.let {
            binding.root.removeView(it)
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
            },
            onVagaClick = { vaga ->
                if (reservaViewModel.temReservaAtiva.value == true) {
                    Toast.makeText(this, "Você já tem uma reserva ativa. Finalize-a antes de reservar outra vaga.", Toast.LENGTH_LONG).show()
                    return@VagasAdapter
                }

                if (!vaga.disponivel) {
                    Toast.makeText(this, "Esta vaga não está disponível no momento.", Toast.LENGTH_SHORT).show()
                    return@VagasAdapter
                }

                val intent = Intent(this, ReservaActivity::class.java).apply {
                    putExtra("vagaId", vaga.id)
                    putExtra("estacionamentoId", estacionamentoId)
                    putExtra("numero", vaga.numero)
                    putExtra("preco", vaga.preco)
                    putExtra("tipo", vaga.tipo)
                }
                startActivity(intent)
            }
        )
        binding.recyclerView.layoutManager = GridLayoutManager(this,2)
        binding.recyclerView.adapter = vagasAdapter
    }

    private fun setupObservers() {
        viewModel.vagas.observe(this) { vagas ->
            if (reservaViewModel.temReservaAtiva.value != true) {
                vagasAdapter.submitList(vagas)
            }
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

            if (reservaViewModel.temReservaAtiva.value == true) return@setOnCheckedChangeListener

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

    override fun onResume() {
        super.onResume()
        reservaViewModel.verificarReservaAtiva()
    }
}
