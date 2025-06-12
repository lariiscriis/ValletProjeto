package br.edu.fatecpg.valletprojeto.fragments

import HistoricoReservasAdapter
import ReservaHistorico
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import br.edu.fatecpg.valletprojeto.databinding.FragmentMotoristaDashboardBinding
import br.edu.fatecpg.valletprojeto.databinding.ItemReservaHistoricoRecenteBinding

class MotoristaFragment : Fragment() {

    private var _binding: FragmentMotoristaDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoricoReservasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMotoristaDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val layoutManager = GridLayoutManager(requireContext(), 3, GridLayoutManager.HORIZONTAL, false)
        binding.rvReservationHistory.layoutManager = layoutManager
        binding.rvReservationHistory.setHasFixedSize(true)
        binding.rvReservationHistory.isNestedScrollingEnabled = false // Desabilita scroll

        val listaExemplo = listOf(
            ReservaHistorico("A1", "15/06", "14:00-16:00"),
            ReservaHistorico("B2", "16/06", "10:00-12:00"),
            ReservaHistorico("C3", "17/06", "09:00-11:00"),
            ReservaHistorico("D4", "18/06", "13:00-15:00"),
            ReservaHistorico("D4", "18/06", "13:00-15:00"),
            ReservaHistorico("D4", "18/06", "13:00-15:00")

            )

        adapter = HistoricoReservasAdapter(listaExemplo)
        binding.rvReservationHistory.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

