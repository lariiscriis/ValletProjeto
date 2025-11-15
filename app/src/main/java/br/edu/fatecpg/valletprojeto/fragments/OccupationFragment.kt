package br.edu.fatecpg.valletprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.adapter.OccupationAdapter
import br.edu.fatecpg.valletprojeto.databinding.FragmentOccupationBinding
import br.edu.fatecpg.valletprojeto.model.VagaOcupada
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OccupationFragment : Fragment( ) {

    private var _binding: FragmentOccupationBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OccupationAdapter
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOccupationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OccupationAdapter()
        binding.rvVagasOcupadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVagasOcupadas.adapter = adapter

        iniciarListenerVagasOcupadas()
    }

    private fun iniciarListenerVagasOcupadas() {
        listener = db.collection("reserva")
            .whereEqualTo("status", "ativa")
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || _binding == null) {
                    return@addSnapshotListener
                }

                if (error != null || snapshot == null) {
                    atualizarVisibilidade(true)
                    return@addSnapshotListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val vagasOcupadas = processarReservas(snapshot)

                    if (isAdded && _binding != null) {
                        adapter.submitList(vagasOcupadas)
                        atualizarVisibilidade(vagasOcupadas.isEmpty())
                    }
                }
            }
    }


    private fun atualizarVisibilidade(isListaVazia: Boolean) {
        if (isListaVazia) {
            binding.rvVagasOcupadas.visibility = View.GONE
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else {
            binding.rvVagasOcupadas.visibility = View.VISIBLE
            binding.tvEmptyMessage.visibility = View.GONE
        }
    }
    private suspend fun processarReservas(snapshot: QuerySnapshot): List<VagaOcupada> = withContext(Dispatchers.IO) {
        val listaTemporaria = mutableListOf<VagaOcupada>()

        for (reservaDoc in snapshot.documents) {
            val usuarioId = reservaDoc.getString("usuarioId") ?: continue
            val vagaId = reservaDoc.getString("vagaId") ?: continue
            val inicio = reservaDoc.getTimestamp("inicioReserva")
            val fim = reservaDoc.getTimestamp("fimReserva")

            try {
                val usuarioDeferred = async { db.collection("usuario").document(usuarioId).get().await() }
                val vagaDeferred = async { db.collection("vaga").document(vagaId).get().await() }

                val usuarioDoc = usuarioDeferred.await()
                val vagaDoc = vagaDeferred.await()

                val email = usuarioDoc.getString("email") ?: ""

                val veiculoSnapshot = db.collection("veiculo")
                    .whereEqualTo("usuarioEmail", email)
                    .limit(1)
                    .get()
                    .await()

                val veiculoDoc = veiculoSnapshot.documents.firstOrNull()

                val vagaOcupada = VagaOcupada(
                    numeroVaga = vagaDoc.getString("numero") ?: "N/A",
                    motoristaNome = usuarioDoc.getString("nome") ?: "N/A",
                    motoristaEmail = email,
                    motoristaTelefone = usuarioDoc.getString("telefone") ?: "N/A",
                    motoristaFoto = usuarioDoc.getString("fotoPerfil"),
                    carroModelo = veiculoDoc?.getString("modelo") ?: "N/A",
                    carroPlaca = veiculoDoc?.getString("placa") ?: "N/A",
                    horaInicio = inicio?.toDate()?.toString() ?: "",
                    horaFim = fim?.toDate()?.toString() ?: "",
                    localizacao = vagaDoc.getString("localizacao") ?: "N/A",
                    preferencial = vagaDoc.getBoolean("preferencial") ?: false
                )
                listaTemporaria.add(vagaOcupada)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext listaTemporaria
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }

}
