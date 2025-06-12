package br.edu.fatecpg.valletprojeto

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroCarroBinding
import br.edu.fatecpg.valletprojeto.databinding.DialogCadastroCarroBinding
import br.edu.fatecpg.valletprojeto.databinding.ItemCardCarroBinding
import br.edu.fatecpg.valletprojeto.model.Carro
import br.edu.fatecpg.valletprojeto.viewmodel.CarroViewModel

class CadastroCarro : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroCarroBinding
    private val carrosCadastrados = mutableListOf<Carro>()
    private val viewModel: CarroViewModel by viewModels()
    private lateinit var carrosAdapter: CarrosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCadastroCarroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar RecyclerView
        setupRecyclerView()

        carregarCarrosDoUsuario()

        binding.btnAdicionarCarro.setOnClickListener {
            showAddCarDialog()
        }

        binding.btnFinalizar.setOnClickListener {
            Toast.makeText(
                this,
                "Cadastro finalizado com ${carrosCadastrados.size} carro(s)!",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(this, CarroActivity::class.java)
         startActivity(intent)
            //val vagaId = "123"
            //val estacionamentoId = "123"
//            val intent = Intent(this, ReservaActivity::class.java)
//            intent.putExtra("VAGA_ID", vagaId)
//            intent.putExtra("ESTACIONAMENTO_ID", estacionamentoId)
//            startActivity(intent)
        }
    }

    private fun carregarCarrosDoUsuario() {
        viewModel.listarCarrosDoUsuario(
            onSuccess = { lista ->
                carrosCadastrados.clear()
                carrosCadastrados.addAll(lista)
                carrosAdapter.notifyDataSetChanged()
                binding.btnFinalizar.visibility = if (carrosCadastrados.isNotEmpty()) View.VISIBLE else View.GONE
            },
            onFailure = { erro ->
                Toast.makeText(this, "Erro ao carregar carros: $erro", Toast.LENGTH_LONG).show()
            }
        )
    }


    private fun setupRecyclerView() {
        carrosAdapter = CarrosAdapter(carrosCadastrados)
        binding.recyclerViewCarros.apply {
            layoutManager = LinearLayoutManager(this@CadastroCarro)
            adapter = carrosAdapter
        }
    }

    private fun showAddCarDialog() {
        val dialogBinding = DialogCadastroCarroBinding.inflate(layoutInflater)

        val dialog = Dialog(this, R.style.DialogTheme).apply {
            setContentView(dialogBinding.root)

            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            setCancelable(true)
        }

        dialogBinding.btnConfirmar.setOnClickListener {
            val placa = dialogBinding.editPlaca.text.toString().trim()
            val marca = dialogBinding.editMarca.text.toString().trim()
            val modelo = dialogBinding.editModelo.text.toString().trim()
            val ano = dialogBinding.editAno.text.toString().trim()
            val km = dialogBinding.editKM.text.toString().trim()

            if (placa.isNotEmpty() && marca.isNotEmpty() && modelo.isNotEmpty() && ano.isNotEmpty() && km.isNotEmpty()) {
                val novoCarro = Carro(placa, marca, modelo, ano, km)

                viewModel.cadastrarCarro(
                    novoCarro,
                    onSuccess = {
                        carrosCadastrados.add(novoCarro)
                        carrosAdapter.notifyItemInserted(carrosCadastrados.size - 1)

                        binding.btnFinalizar.visibility = View.VISIBLE

                        dialog.dismiss()
                        Toast.makeText(this, "Carro salvo!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { erro ->
                        Toast.makeText(this, "Erro ao salvar: $erro", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 7. Mostrar o dialog
        Log.d("DialogTest", "Tentando mostrar o dialog")
        dialog.show()
        Log.d("DialogTest", "Dialog mostrado")    }
    // Adapter para o RecyclerView
    inner class CarrosAdapter(private val carros: List<Carro>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<CarrosAdapter.CarroViewHolder>() {

        inner class CarroViewHolder(val binding: ItemCardCarroBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CarroViewHolder {
            val binding = ItemCardCarroBinding.inflate(
                layoutInflater,
                parent,
                false
            )
            return CarroViewHolder(binding)
        }
        private val editarCarroLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                carregarCarrosDoUsuario()
            }
        }


        override fun onBindViewHolder(holder: CarroViewHolder, position: Int) {
            val carro = carros[position]
            holder.binding.txtPlaca.text = "${carro.placa}"
            holder.binding.txtMarcaModelo.text = "${carro.marca} ${carro.modelo}"
            holder.binding.txtDetalhes.text = "${carro.ano}•${carro.km} km"

            holder.itemView.setOnClickListener {
                val intent = Intent(this@CadastroCarro, EditarCarroActivity::class.java).apply {
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

        override fun getItemCount() = carros.size
    }
}
