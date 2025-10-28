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
    private var isAdmin: Boolean = false
    private var estacionamentoId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVagaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[VagaViewModel::class.java]

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

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
                                        Toast.makeText(requireContext(), "Estacionamento não encontrado para este admin", Toast.LENGTH_SHORT).show()
                                        setupUI()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), "Erro ao buscar estacionamento do admin", Toast.LENGTH_SHORT).show()
                                    setupUI()
                                }
                        } else {
                            Toast.makeText(requireContext(), "E-mail do admin não encontrado", Toast.LENGTH_SHORT).show()
                            setupUI()
                        }
                    } else {
                        // Lógica para não-admins, se necessário
                        setupUI()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Erro ao verificar tipo de usuário", Toast.LENGTH_SHORT).show()
                    setupUI()
                }
        } else {
            Toast.makeText(requireContext(), "Usuário não logado", Toast.LENGTH_SHORT).show()
            setupUI()
        }
    }

    private fun setupUI() {
        setupListeners()
        setupRecyclerView()
        setupObservers()

        estacionamentoId?.let {
            viewModel.fetchVagasPorEstacionamento(it)
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
                    Toast.makeText(requireContext(), "Estacionamento não encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.fabAdd.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.vagas.observe(viewLifecycleOwner) { vagas ->
            binding.recyclerView.adapter = VagasAdapter(
                vagas,
                isAdmin = isAdmin,
                onEditClick = { vaga ->
                    startActivity(Intent(requireContext(), EditarVagaActivity::class.java).apply {
                        putExtra("vagaId", vaga.id)
                    })
                },
                onDeleteClick = { vaga ->
                    showDeleteDialog(vaga)
                }
            )
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
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
