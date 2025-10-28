package br.edu.fatecpg.valletprojeto.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.VagaActivity
import br.edu.fatecpg.valletprojeto.adapter.FavoriteParkingAdapter
import br.edu.fatecpg.valletprojeto.adapter.SimpleParkingAdapter
import br.edu.fatecpg.valletprojeto.databinding.FragmentSpotsBinding
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class SpotsFragment : Fragment() {

    private var _binding: FragmentSpotsBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val estacionamentos = mutableListOf<Estacionamento>()
    private lateinit var adapter: SimpleParkingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpotsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        adapter = SimpleParkingAdapter { estacionamento ->
            val intent = Intent(requireContext(), VagaActivity::class.java)
            intent.putExtra("estacionamentoId", estacionamento.id)
            startActivity(intent)
        }

        binding.rvOtherParkings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOtherParkings.adapter = adapter

        verificarPermissaoLocalizacao()

        // 🔍 Campo de pesquisa
        binding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val termo = newText.orEmpty().trim().lowercase()
                val listaFiltrada = estacionamentos.filter {
                    it.nome.lowercase().contains(termo) ||
                        it.endereco?.lowercase()?.contains(termo) == true
                }
                adapter.submitList(listaFiltrada)
                return true
            }
        })
    }

    private fun carregarFavoritos(usuarioId: String?, callback: (List<String>) -> Unit) {
        if (usuarioId == null) {
            callback(emptyList())
            return
        }

        db.collection("favoritos")
            .whereEqualTo("usuarioId", usuarioId)
            .get()
            .addOnSuccessListener { snapshot ->
                val favoritosIds = snapshot.documents.mapNotNull { it.getString("estacionamentoId") }
                callback(favoritosIds)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    private fun verificarPermissaoLocalizacao() {
        val context = requireContext()
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            obterLocalizacaoUsuario()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }
    private fun obterLocalizacaoUsuario() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            carregarEstacionamentos(null)
            return
        }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                val usuarioId = FirebaseAuth.getInstance().currentUser?.uid
                carregarFavoritos(usuarioId) { favoritosIds ->
                    carregarEstacionamentos(location, favoritosIds)
                }
            } else {
                Toast.makeText(requireContext(), "Não foi possível obter sua localização.", Toast.LENGTH_SHORT).show()
                carregarEstacionamentos(null)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Erro ao obter localização.", Toast.LENGTH_SHORT).show()
            carregarEstacionamentos(null)
        }
    }


    private fun carregarEstacionamentos(
        userLocation: Location?,
        favoritosIds: List<String> = emptyList()
    ) {
        db.collection("estacionamento").get()
            .addOnSuccessListener { snapshot ->
                estacionamentos.clear()
                if (snapshot.isEmpty) {
                    adapter.submitList(emptyList())
                    return@addOnSuccessListener
                }

                var carregados = 0
                val total = snapshot.size()

                for (doc in snapshot.documents) {
                    val est = doc.toObject(Estacionamento::class.java)
                    est?.let {
                        val id = doc.id
                        val lat = doc.getDouble("latitude")
                        val lon = doc.getDouble("longitude")
                        var distanciaMetros: Int? = null

                        if (userLocation != null && lat != null && lon != null) {
                            val estLoc = Location("").apply {
                                latitude = lat
                                longitude = lon
                            }
                            distanciaMetros = userLocation.distanceTo(estLoc).roundToInt()
                        }

                        // Consulta vagas disponíveis
                        db.collection("vaga")
                            .whereEqualTo("estacionamentoId", id)
                            .whereEqualTo("ocupada", false)
                            .get()
                            .addOnSuccessListener { vagasSnapshot ->
                                val vagasDisponiveis = vagasSnapshot.size()
                                val atualizado = it.copy(
                                    id = id,
                                    distanciaMetros = distanciaMetros,
                                    vagasDisponiveis = vagasDisponiveis
                                )
                                estacionamentos.add(atualizado)
                            }
                            .addOnCompleteListener {
                                carregados++
                                if (carregados == total) {
                                    val listaOrdenada = estacionamentos.sortedBy { it.distanciaMetros ?: Int.MAX_VALUE }
                                    adapter.submitList(listaOrdenada)
                                    val favoritos = estacionamentos.filter { it.id in favoritosIds }
                                    atualizarFavoritosUI(favoritos)
                                }
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar estacionamentos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun atualizarFavoritosUI(favoritos: List<Estacionamento>) {
        if (_binding == null || !isAdded) return

        val adapter = FavoriteParkingAdapter(favoritos) { estacionamento ->
            startActivity(Intent(requireContext(), VagaActivity::class.java).apply {
                putExtra("estacionamentoId", estacionamento.id)
            })
        }

        binding.rvFavorites.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFavorites.adapter = adapter
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obterLocalizacaoUsuario()
        } else {
            carregarEstacionamentos(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
