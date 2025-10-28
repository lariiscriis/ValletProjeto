package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.dao.UsuarioDao
import br.edu.fatecpg.valletprojeto.dao.VeiculoDao
import br.edu.fatecpg.valletprojeto.databinding.ActivityVeiculoBinding
import br.edu.fatecpg.valletprojeto.databinding.ItemCardVeiculoBinding
import br.edu.fatecpg.valletprojeto.fragments.VeiculoFragment
import br.edu.fatecpg.valletprojeto.model.Veiculo
import br.edu.fatecpg.valletprojeto.viewmodel.VeiculoViewModel
import br.edu.fatecpg.valletprojeto.viewmodel.VeiculoViewModelFactory

class VeiculoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVeiculoBinding
    private val veiculosCadastrados = mutableListOf<Veiculo>()
    val viewModel: VeiculoViewModel by viewModels {
        VeiculoViewModelFactory(UsuarioDao(), VeiculoDao)
    }
    private lateinit var veiculosAdapter: VeiculosAdapter

    private val editarVeiculoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            carregarVeiculosDoUsuario()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVeiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.fabAddCar.setOnClickListener {
            val veiculoFragment = VeiculoFragment()
            veiculoFragment.show(supportFragmentManager, "VeiculoFragment")
        }

        setupRecyclerView()
        carregarVeiculosDoUsuario()
    }

    fun carregarVeiculosDoUsuario() {
        viewModel.listarVeiculosDoUsuario(
            onSuccess = { lista ->
                veiculosCadastrados.clear()
                veiculosCadastrados.addAll(lista)
                veiculosAdapter.notifyDataSetChanged()
            },
            onFailure = { erro ->
                Toast.makeText(this, "Erro ao carregar veículos: $erro", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setupRecyclerView() {
        veiculosAdapter = VeiculosAdapter(veiculosCadastrados)
        binding.rvCarList.apply {
            layoutManager = LinearLayoutManager(this@VeiculoActivity)
            adapter = veiculosAdapter
        }
    }

    inner class VeiculosAdapter(private val veiculos: List<Veiculo>) :
        RecyclerView.Adapter<VeiculosAdapter.VeiculoViewHolder>() {

        inner class VeiculoViewHolder(val binding: ItemCardVeiculoBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(
            parent: android.view.ViewGroup,
            viewType: Int
        ): VeiculoViewHolder {
            val binding = ItemCardVeiculoBinding.inflate(layoutInflater, parent, false)
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
                val intent = Intent(this@VeiculoActivity, EditarVeiculoActivity::class.java).apply {
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
