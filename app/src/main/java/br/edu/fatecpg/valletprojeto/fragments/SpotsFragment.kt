package br.edu.fatecpg.valletprojeto.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.VagaActivity
import br.edu.fatecpg.valletprojeto.adapter.FavoriteParkingAdapter
import br.edu.fatecpg.valletprojeto.adapter.SimpleParkingAdapter
import br.edu.fatecpg.valletprojeto.databinding.FragmentSpotsBinding
import br.edu.fatecpg.valletprojeto.viewmodel.SpotsViewModel

class SpotsFragment : Fragment() {

    private var _binding: FragmentSpotsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SpotsViewModel by viewModels()

    private lateinit var simpleAdapter: SimpleParkingAdapter
    private lateinit var favoriteAdapter: FavoriteParkingAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.loadData()
            } else {
                Toast.makeText(requireContext(), "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
                viewModel.loadData(useLocation = false)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpotsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSearchView()
        observeViewModel()

        verificarPermissaoLocalizacao()
    }

    private fun setupRecyclerViews() {
        simpleAdapter = SimpleParkingAdapter(
            onFavoriteClicked = { estacionamento ->
                viewModel.toggleFavoriteStatus(estacionamento)
            },
            onItemClicked = { estacionamento ->
                viewModel.onEstacionamentoClicked(estacionamento)
            }
        )
        binding.rvOtherParkings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOtherParkings.adapter = simpleAdapter

        favoriteAdapter = FavoriteParkingAdapter(
            onFavoriteClicked = { estacionamento ->
                viewModel.toggleFavoriteStatus(estacionamento)
            },
            onItemClicked = { estacionamento ->
                viewModel.onEstacionamentoClicked(estacionamento)
            }
        )
        binding.rvFavorites.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFavorites.adapter = favoriteAdapter
    }

    private fun setupSearchView() {
        binding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchParkings(newText.orEmpty())
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.parkings.observe(viewLifecycleOwner, Observer { parkings ->
            simpleAdapter.submitList(parkings)
        })

        viewModel.favoriteParkings.observe(viewLifecycleOwner, Observer { favorites ->
            favoriteAdapter.submitList(favorites)
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        })

        viewModel.toastMessage.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.navigateToVagas.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { estacionamentoId ->
                val intent = Intent(requireContext(), VagaActivity::class.java)
                intent.putExtra("estacionamentoId", estacionamentoId)
                startActivity(intent)
            }
        })
    }


    private fun verificarPermissaoLocalizacao() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadData()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
