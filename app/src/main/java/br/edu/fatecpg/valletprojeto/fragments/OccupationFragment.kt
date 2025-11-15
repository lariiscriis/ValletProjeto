package br.edu.fatecpg.valletprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.adapter.OccupationAdapter
import br.edu.fatecpg.valletprojeto.databinding.FragmentOccupationBinding
import br.edu.fatecpg.valletprojeto.model.VagaOcupada
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class OccupationFragment : Fragment() {

    private var _binding: FragmentOccupationBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OccupationAdapter
    private val vagasOcupadas = mutableListOf<VagaOcupada>()
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOccupationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OccupationAdapter(vagasOcupadas)
        binding.rvVagasOcupadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVagasOcupadas.adapter = adapter

        iniciarListenerVagasOcupadas()
    }

    private fun iniciarListenerVagasOcupadas() {
        listener = db.collection("reserva")
            .whereEqualTo("status", "ativa")
            .addSnapshotListener { snapshot, _ ->
                vagasOcupadas.clear()
                if (snapshot == null || snapshot.isEmpty) {
                    adapter.atualizarLista(vagasOcupadas)
                    return@addSnapshotListener
                }

                for (reservaDoc in snapshot.documents) {
                    val usuarioId = reservaDoc.getString("usuarioId") ?: continue
                    val vagaId = reservaDoc.getString("vagaId") ?: continue
                    val inicio = reservaDoc.getTimestamp("inicioReserva")
                    val fim = reservaDoc.getTimestamp("fimReserva")

                    db.collection("usuario").document(usuarioId).get()
                        .addOnSuccessListener { usuarioDoc ->
                            val nome = usuarioDoc.getString("nome") ?: ""
                            val email = usuarioDoc.getString("email") ?: ""
                            val telefone = usuarioDoc.getString("telefone") ?: ""
                            val fotoPerfil = usuarioDoc.getString("fotoPerfil") ?: ""

                            db.collection("vaga").document(vagaId).get()
                                .addOnSuccessListener { vagaDoc ->
                                    val numeroVaga = vagaDoc.getString("numero") ?: ""
                                    val estacionamentoId = vagaDoc.getString("estacionamentoId") ?: ""

                                    db.collection("veiculo")
                                        .whereEqualTo("usuarioEmail", email)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener { veiculoSnapshot ->
                                            val veiculoDoc = veiculoSnapshot.documents.firstOrNull()
                                            val modelo = veiculoDoc?.getString("modelo") ?: ""
                                            val placa = veiculoDoc?.getString("placa") ?: ""

                                            val vagaOcupada = VagaOcupada(
                                                numeroVaga = numeroVaga,
                                                motoristaNome = nome,
                                                motoristaEmail = email,
                                                motoristaTelefone = telefone,
                                                motoristaFoto = fotoPerfil,
                                                carroModelo = modelo,
                                                carroPlaca = placa,
                                                horaInicio = inicio?.toDate()?.toString() ?: "",
                                                horaFim = fim?.toDate()?.toString() ?: "",
                                                localizacao = vagaDoc.getString("localizacao") ?: "",
                                                preferencial = vagaDoc.getBoolean("preferencial") ?: false
                                            )


                                            vagasOcupadas.add(vagaOcupada)
                                            adapter.atualizarLista(vagasOcupadas)
                                        }
                                }
                        }
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }
}
