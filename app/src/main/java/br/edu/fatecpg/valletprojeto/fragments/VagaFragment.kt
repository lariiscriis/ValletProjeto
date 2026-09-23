package br.edu.fatecpg.valletprojeto.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.CadastroVagaActivity
import br.edu.fatecpg.valletprojeto.EditarVagaActivity
import br.edu.fatecpg.valletprojeto.ReservaActivity // 🔥 ADICIONE ESTE IMPORT
import br.edu.fatecpg.valletprojeto.adapter.VagasAdapter
import br.edu.fatecpg.valletprojeto.databinding.FragmentVagaBinding
import br.edu.fatecpg.valletprojeto.model.Vaga
import br.edu.fatecpg.valletprojeto.viewmodel.VagaViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VagaFragment : Fragment() {

    private lateinit var viewModel: VagaViewModel
    private var _binding: FragmentVagaBinding? = null
    private val binding get() = _binding!!
    private lateinit var vagasAdapter: VagasAdapter
    private var isAdmin: Boolean = false
    private var estacionamentoId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVagaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[VagaViewModel::class.java]
        verificarUsuarioAdmin()
    }

    private fun verificarUsuarioAdmin() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            binding.fabAdd.visibility = View.GONE
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("usuario").document(userId).get()
            .addOnSuccessListener { document ->
                if (activity == null || !isAdded) return@addOnSuccessListener
                isAdmin = document.getString("tipo_user") == "admin"
                if (isAdmin) {
                    buscarEstacionamentoDoAdmin()
                } else {
                    binding.fabAdd.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                if (activity == null || !isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Erro ao verificar tipo de usuário.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buscarEstacionamentoDoAdmin() {
        val emailAdmin = FirebaseAuth.getInstance().currentUser?.email ?: return
        FirebaseFirestore.getInstance().collection("estacionamento")
            .whereEqualTo("adminEmail", emailAdmin)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (activity == null || !isAdded) return@addOnSuccessListener

                if (!querySnapshot.isEmpty) {
                    estacionamentoId = querySnapshot.documents[0].id
                    setupUI()
                } else {
                    Toast.makeText(requireContext(), "Nenhum estacionamento encontrado para este admin.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                if (activity == null || !isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Erro ao buscar estacionamento do admin.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUI() {
        setupRecyclerView()
        setupObservers()
        setupListeners()

        estacionamentoId?.let {
            viewModel.fetchVagasComFiltro(it, "Todos")
        }
    }

    private fun setupRecyclerView() {
        vagasAdapter = VagasAdapter(
            isAdmin = this.isAdmin,
            onEditClick = { vaga ->
                startActivity(Intent(requireContext(), EditarVagaActivity::class.java).apply {
                    putExtra("vagaId", vaga.id)
                })
            },
            onDeleteClick = { vaga ->
                showDeleteDialog(vaga)
            },
            // 🔥 CORREÇÃO: Adicione o parâmetro onVagaClick
            onVagaClick = { vaga ->
                if (isAdmin) {
                    // Admin não deve fazer reservas, apenas gerenciar
                    return@VagasAdapter
                }

                // Verifica se a vaga está disponível
                if (!vaga.disponivel) {
                    Toast.makeText(requireContext(), "Esta vaga não está disponível no momento.", Toast.LENGTH_SHORT).show()
                    return@VagasAdapter
                }

                // Abre a tela de reserva para usuários normais
                val intent = Intent(requireContext(), ReservaActivity::class.java).apply {
                    putExtra("vagaId", vaga.id)
                    putExtra("estacionamentoId", estacionamentoId)
                    putExtra("numero", vaga.numero)
                    putExtra("preco", vaga.preco)
                    putExtra("tipo", vaga.tipo)
                }
                startActivity(intent)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = vagasAdapter
    }

    private fun setupObservers() {
        viewModel.vagas.observe(viewLifecycleOwner) { vagas ->
            vagasAdapter.submitList(vagas)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        if (isAdmin) {
            binding.fabAdd.visibility = View.VISIBLE
            binding.fabAdd.setOnClickListener {
                estacionamentoId?.let {
                    val intent = Intent(requireContext(), CadastroVagaActivity::class.java)
                    intent.putExtra("estacionamentoId", it)
                    startActivity(intent)
                } ?: run {
                    Toast.makeText(requireContext(), "ID do estacionamento não disponível.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.fabAdd.visibility = View.GONE
        }
    }

    private fun showDeleteDialog(vaga: Vaga) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Vaga")
            .setMessage("Tem certeza que deseja excluir esta vaga?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.deleteVaga(vaga.id) { success, message ->
                    if (success) {
                        Toast.makeText(requireContext(), "Vaga excluída com sucesso", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), message ?: "Erro ao excluir vaga", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}