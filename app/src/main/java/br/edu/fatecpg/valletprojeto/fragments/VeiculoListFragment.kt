package br.edu.fatecpg.valletprojeto.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.EditarVeiculoActivity
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.dao.UsuarioDao
import br.edu.fatecpg.valletprojeto.dao.VeiculoDao
import br.edu.fatecpg.valletprojeto.databinding.ActivityVeiculoBinding
import br.edu.fatecpg.valletprojeto.databinding.ItemCardVeiculoBinding
import br.edu.fatecpg.valletprojeto.model.Veiculo
import br.edu.fatecpg.valletprojeto.viewmodel.VeiculoViewModel
import br.edu.fatecpg.valletprojeto.viewmodel.VeiculoViewModelFactory

class VeiculoListFragment : Fragment() {

    private var _binding: ActivityVeiculoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VeiculoViewModel by activityViewModels {
        VeiculoViewModelFactory(UsuarioDao(), VeiculoDao)
    }

    private lateinit var veiculosAdapter: VeiculosAdapter
    private val veiculosCadastrados = mutableListOf<Veiculo>()

    private val editarVeiculoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            carregarVeiculosDoUsuario()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityVeiculoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        carregarVeiculosDoUsuario()

        binding.fabAddCar.setOnClickListener {
            showCadastroVeiculoFragment()
        }
    }

    private fun showCadastroVeiculoFragment() {
        val veiculoFragment = VeiculoFragment()
        veiculoFragment.show(parentFragmentManager, "VeiculoFragment")
    }

    private fun carregarVeiculosDoUsuario() {
        viewModel.listarVeiculosDoUsuario(
            onSuccess = { lista ->
                veiculosCadastrados.clear()
                veiculosCadastrados.addAll(lista)
                veiculosAdapter.notifyDataSetChanged()
            },
            onFailure = { erro ->
                Toast.makeText(requireContext(), "Erro ao carregar veículos: $erro", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setupRecyclerView() {
        veiculosAdapter = VeiculosAdapter(veiculosCadastrados)
        binding.rvCarList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = veiculosAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class VeiculosAdapter(private val veiculos: List<Veiculo>) :
        RecyclerView.Adapter<VeiculosAdapter.VeiculoViewHolder>() {

        inner class VeiculoViewHolder(val binding: ItemCardVeiculoBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VeiculoViewHolder {
            val binding = ItemCardVeiculoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VeiculoViewHolder(binding)
        }

        override fun onBindViewHolder(holder: VeiculoViewHolder, position: Int) {
            val veiculo = veiculos[position]

            if (veiculo.tipo == "moto") {
                holder.binding.ivCarIcon.setImageResource(R.drawable.ic_moto)
            } else {
                holder.binding.ivCarIcon.setImageResource(R.drawable.ic_vehicle)
            }

            holder.binding.tvCarModel.text = "${veiculo.marca} ${veiculo.modelo}"
            holder.binding.tvCarPlate.text = veiculo.placa

            holder.binding.toggleFavoriteCar.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Toast.makeText(
                        holder.itemView.context,
                        "${veiculo.placa} definido como principal!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(requireContext(), EditarVeiculoActivity::class.java).apply {
                    putExtra("veiculoId", veiculo.id)
                    putExtra("placa", veiculo.placa)
                    putExtra("marca", veiculo.marca)
                    putExtra("modelo", veiculo.modelo)
                    putExtra("ano", veiculo.ano)
                    putExtra("km", veiculo.km)
                    putExtra("tipo", veiculo.tipo)
                }
                editarVeiculoLauncher.launch(intent)
            }
        }

        override fun getItemCount(): Int = veiculos.size
    }
}
