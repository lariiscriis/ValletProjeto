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

        // Adapter e RecyclerView
        adapter = SimpleParkingAdapter { estacionamento ->
            startActivity(Intent(requireContext(), VagaActivity::class.java).apply {
                putExtra("estacionamentoId", estacionamento.id)
            })
        }

        binding.rvOtherParkings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOtherParkings.adapter = adapter

        binding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
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

        verificarPermissaoLocalizacao()
    }

    private fun verificarPermissaoLocalizacao() {
        val context = requireContext()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            carregarLocalizacao()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    private fun carregarLocalizacao() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            carregarEstacionamentos(null)
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                val usuarioId = FirebaseAuth.getInstance().currentUser?.uid
                carregarFavoritos(usuarioId) { favoritosIds ->
                    if (!isAdded) return@carregarFavoritos
                    carregarEstacionamentos(location, favoritosIds)
                }
            }
            .addOnFailureListener {
                context?.let {
                    Toast.makeText(it, "Erro ao obter localização.", Toast.LENGTH_SHORT).show()
                }
                carregarEstacionamentos(null)
            }
    }

    private fun carregarFavoritos(usuarioId: String?, callback: (List<String>) -> Unit) {
        if (usuarioId == null) { callback(emptyList()); return }
        db.collection("favoritos")
            .whereEqualTo("usuarioId", usuarioId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val favoritosIds = snapshot.documents.mapNotNull { it.getString("estacionamentoId") }
                callback(favoritosIds)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    private fun carregarEstacionamentos(
        userLocation: Location?,
        favoritosIds: List<String> = emptyList()
    ) {
        db.collection("estacionamento").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                estacionamentos.clear()

                snapshot.documents.forEach { doc ->
                    val est = doc.toObject(Estacionamento::class.java) ?: return@forEach
                    val id = doc.id
                    val lat = doc.getDouble("latitude") ?: doc.getString("latitude")?.toDoubleOrNull()
                    val lon = doc.getDouble("longitude") ?: doc.getString("longitude")?.toDoubleOrNull()

                    val distanciaMetros = if (lat != null && lon != null && userLocation != null) {
                        Location("").apply { latitude = lat; longitude = lon }
                            .let { userLocation.distanceTo(it).roundToInt() }
                    } else null


                    estacionamentos.add(est.copy(
                        id = id,
                        distanciaMetros = distanciaMetros
                    ))
                }

                val listaOrdenada = if (userLocation != null) {
                    estacionamentos.sortedBy { it.distanciaMetros ?: Int.MAX_VALUE }
                } else estacionamentos

                adapter.submitList(listaOrdenada)

                // Atualiza favoritos
                val favoritos = estacionamentos.filter { it.id in favoritosIds }
                atualizarFavoritosUI(favoritos)
            }
            .addOnFailureListener {
                context?.let {
                    Toast.makeText(it, "Erro ao carregar estacionamentos", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun atualizarFavoritosUI(favoritos: List<Estacionamento>) {
        if (!isAdded || _binding == null) return
        binding.rvFavorites.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFavorites.adapter = FavoriteParkingAdapter(favoritos) { estacionamento ->
            startActivity(Intent(requireContext(), VagaActivity::class.java).apply {
                putExtra("estacionamentoId", estacionamento.id)
            })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            carregarLocalizacao()
        } else {
            carregarEstacionamentos(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
