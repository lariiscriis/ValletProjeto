package br.edu.fatecpg.valletprojeto.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

class SpotsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _parkings = MutableLiveData<List<Estacionamento>>()
    val parkings: LiveData<List<Estacionamento>> = _parkings

    private val _favoriteParkings = MutableLiveData<List<Estacionamento>>()
    val favoriteParkings: LiveData<List<Estacionamento>> = _favoriteParkings

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var allParkingsMasterList: List<Estacionamento> = listOf()

    fun loadData(useLocation: Boolean = true) {
        if (_isLoading.value == true) return
        Log.d("SpotsViewModel", "Iniciando loadData...")

        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                val userLocation = if (useLocation) {
                    Log.d("SpotsViewModel", "Buscando localização...")
                    withTimeoutOrNull(10000) { getCurrentLocation() }
                } else null

                if (useLocation && userLocation == null) {
                    Log.w("SpotsViewModel", "Falha ao obter localização.")
                    _error.postValue("Não foi possível obter a localização.")
                } else {
                    Log.d("SpotsViewModel", "Localização obtida: $userLocation")
                }

                val favoriteIdsDeferred = async { fetchFavoriteIds() }
                val parkingsDeferred = async {
                    if (userLocation != null) {
                        fetchParkingsNear(userLocation)
                    } else {
                        Log.d("SpotsViewModel", "Fallback: buscando todos os estacionamentos (sem geofire).")
                        fetchAllParkings()
                    }
                }

                val favoriteIds = favoriteIdsDeferred.await()
                val allFetchedParkings = parkingsDeferred.await()
                Log.d("SpotsViewModel", "Busca concluída. ${allFetchedParkings.size} estacionamentos encontrados.")

                val processedParkings = allFetchedParkings.map { est ->
                    val distancia = if (userLocation != null && est.latitude != null && est.longitude != null) {
                        val parkingLocation = Location("").apply { latitude = est.latitude!!; longitude = est.longitude!! }
                        userLocation.distanceTo(parkingLocation).roundToInt()
                    } else null
                    est.copy(distanciaMetros = distancia)
                }.sortedBy { it.distanciaMetros ?: Int.MAX_VALUE }

                allParkingsMasterList = processedParkings
                val (favorites, others) = allParkingsMasterList.partition { it.id in favoriteIds }

                Log.d("SpotsViewModel", "Separando ${favorites.size} favoritos e ${others.size} outros.")
                _favoriteParkings.postValue(favorites)
                _parkings.postValue(others)

            } catch (e: Exception) {
                Log.e("SpotsViewModel", "Erro em loadData", e)
                _error.postValue("Erro crítico: ${e.message}")
            } finally {
                _isLoading.postValue(false)
                Log.d("SpotsViewModel", "loadData finalizado.")
            }
        }
    }

    // SpotsViewModel.kt

    private suspend fun fetchParkingsNear(location: Location): List<Estacionamento> = withContext(Dispatchers.IO) {
        Log.d("SpotsViewModel", "Executando fetchParkingsNear em ${Thread.currentThread().name}")
        val center = GeoLocation(location.latitude, location.longitude)
        val radiusInM = 100 * 1000.0

        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
        val tasks = bounds.map { bound ->
            db.collection("estacionamento")
                .orderBy("geohash")
                .startAt(bound.startHash)
                .endAt(bound.endHash)
                .get()
        }

        val snapshots = Tasks.await(Tasks.whenAllSuccess<QuerySnapshot>(tasks))
        Log.d("SpotsViewModel", "Geohash query retornou ${snapshots.sumOf { it.size() }} documentos brutos.")

        // --- INÍCIO DA MUDANÇA COM LOGS ---
        val matchingDocs = snapshots.flatMap { snap ->
            snap.documents.filter { doc ->
                // Tenta ler como Double, se falhar, tenta ler como String e converter.
                val lat = doc.get("latitude") as? Double ?: (doc.get("latitude") as? String)?.toDoubleOrNull() ?: 0.0
                val lng = doc.get("longitude") as? Double ?: (doc.get("longitude") as? String)?.toDoubleOrNull() ?: 0.0

                // Se lat ou lng for 0.0, é um sinal de alerta.
                if (lat == 0.0 || lng == 0.0) {
                    Log.w("SpotsViewModel", "ALERTA: Coordenadas inválidas para o doc ${doc.id}. Lat: $lat, Lng: $lng")
                }

                val docLocation = GeoLocation(lat, lng)
                val distancia = GeoFireUtils.getDistanceBetween(docLocation, center)

                Log.d("SpotsViewModel", "Filtrando doc ${doc.id}: Distância = ${"%.2f".format(distancia / 1000)} km. (Raio: ${radiusInM / 1000} km)")

                distancia <= radiusInM
            }
        }

        Log.d("SpotsViewModel", "Documentos filtrados por distância: ${matchingDocs.size}")

        matchingDocs.mapNotNull { doc ->
            doc.toObject(Estacionamento::class.java)?.copy(id = doc.id)
        }
    }

    private suspend fun fetchAllParkings(): List<Estacionamento> = withContext(Dispatchers.IO) {
        Log.d("SpotsViewModel", "Executando fetchAllParkings em ${Thread.currentThread().name}")
        val snapshot = db.collection("estacionamento").limit(100).get().await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Estacionamento::class.java)?.copy(id = doc.id)
        }
    }

    private suspend fun fetchFavoriteIds(): Set<String> = withContext(Dispatchers.IO) {
        Log.d("SpotsViewModel", "Executando fetchFavoriteIds em ${Thread.currentThread().name}")
        val userId = auth.currentUser?.uid ?: return@withContext emptySet()
        try {
            val snapshot = db.collection("favoritos").whereEqualTo("usuarioId", userId).get().await()
            snapshot.documents.mapNotNull { it.getString("estacionamentoId") }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        Log.d("SpotsViewModel", "Executando getCurrentLocation em ${Thread.currentThread().name}")
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        } catch (e: Exception) {
            Log.e("SpotsViewModel", "Erro ao obter localização", e)
            null
        }
    }

    fun searchParkings(query: String) {
        val filteredList = if (query.isBlank()) {
            allParkingsMasterList.filterNot { it.id in (_favoriteParkings.value?.map { fav -> fav.id } ?: emptySet()) }
        } else {
            allParkingsMasterList.filter {
                (it.nome.contains(query, ignoreCase = true) || it.endereco.contains(query, ignoreCase = true)) &&
                    it.id !in (_favoriteParkings.value?.map { fav -> fav.id } ?: emptySet())
            }
        }
        _parkings.value = filteredList
    }
}
