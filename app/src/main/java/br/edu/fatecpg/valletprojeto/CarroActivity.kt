package br.edu.fatecpg.valletprojeto

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.databinding.ActivityCarroBinding
import br.edu.fatecpg.valletprojeto.databinding.ItemCardCarroBinding
import br.edu.fatecpg.valletprojeto.model.Carro
import br.edu.fatecpg.valletprojeto.viewmodel.CarroViewModel

class CarroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarroBinding
    private val carrosCadastrados = mutableListOf<Carro>()
    private val viewModel: CarroViewModel by viewModels()
    private lateinit var carrosAdapter: CarrosAdapter

    private val editarCarroLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            carregarCarrosDoUsuario()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCarroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnVerVagas2.setOnClickListener{
            val intent = Intent(this, VagaActivity::class.java)
            startActivity(intent)
        }
        binding.btnAddCarro.setOnClickListener{
            val intent = Intent(this, CadastroCarro::class.java)
            startActivity(intent)
        }

        binding.btnDashboard.setOnClickListener{
            val intent = Intent(this, Dashboard_base::class.java)
            startActivity(intent)
        }

        setupRecyclerView()
        carregarCarrosDoUsuario()
    }

    private fun carregarCarrosDoUsuario() {
        viewModel.listarCarrosDoUsuario(
            onSuccess = { lista ->
                carrosCadastrados.clear()
                carrosCadastrados.addAll(lista)
                carrosAdapter.notifyDataSetChanged()
            },
            onFailure = { erro ->
                Toast.makeText(this, "Erro ao carregar carros: $erro", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setupRecyclerView() {
        carrosAdapter = CarrosAdapter(carrosCadastrados)
        binding.recyclerViewCarros.apply {
            layoutManager = LinearLayoutManager(this@CarroActivity)
            adapter = carrosAdapter
        }
    }

    inner class CarrosAdapter(private val carros: List<Carro>) :
        RecyclerView.Adapter<CarrosAdapter.CarroViewHolder>() {

        inner class CarroViewHolder(val binding: ItemCardCarroBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CarroViewHolder {
            val binding = ItemCardCarroBinding.inflate(layoutInflater, parent, false)
            return CarroViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CarroViewHolder, position: Int) {
            val carro = carros[position]
            holder.binding.txtPlaca.text = carro.placa
            holder.binding.txtMarcaModelo.text = "${carro.marca} ${carro.modelo}"
            holder.binding.txtDetalhes.text = "${carro.ano} • ${carro.km} km"

            holder.itemView.setOnClickListener {
                val intent = Intent(this@CarroActivity, EditarCarroActivity::class.java).apply {
                    putExtra("carroId", carro.id)
                    putExtra("placa", carro.placa)
                    putExtra("marca", carro.marca)
                    putExtra("modelo", carro.modelo)
                    putExtra("ano", carro.ano)
                    putExtra("km", carro.km)
                }
                editarCarroLauncher.launch(intent)
            }
        }

        override fun getItemCount(): Int = carros.size
    }
}
