package br.edu.fatecpg.valletprojeto

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
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
import br.edu.fatecpg.valletprojeto.databinding.DialogCadastroCarroBinding
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

        binding.fabAddCar.setOnClickListener {
            showAddCarDialog()
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
        binding.rvCarList.apply {
            layoutManager = LinearLayoutManager(this@CarroActivity)
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
            val apelido = dialogBinding.editApelido.text.toString().trim()
            val ano = dialogBinding.editAno.text.toString().trim()
            val km = dialogBinding.editKM.text.toString().trim()

            if (placa.isEmpty() || marca.isEmpty() || modelo.isEmpty() || ano.isEmpty() || km.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos obrigatórios (*)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipoSelecionado = when (dialogBinding.rgVehicleType.checkedRadioButtonId) {
                R.id.rb_moto -> "moto"
                else -> "carro"
            }

            val novoCarro = Carro(
                placa = placa,
                marca = marca,
                modelo = modelo,
                apelido = apelido,
                ano = ano,
                km = km,
                tipo = tipoSelecionado
            )

            viewModel.cadastrarCarro(
                novoCarro,
                onSuccess = {
                    carregarCarrosDoUsuario()
                    dialog.dismiss()
                    Toast.makeText(this, "Veículo salvo!", Toast.LENGTH_SHORT).show()
                },
                onFailure = { erro ->
                    Toast.makeText(this, "Erro ao salvar: $erro", Toast.LENGTH_LONG).show()
                }
            )
        }

        Log.d("DialogTest", "Tentando mostrar o dialog")
        dialog.show()
        Log.d("DialogTest", "Dialog mostrado")
    }

    inner class CarrosAdapter(private val carros: List<Carro>) :
        RecyclerView.Adapter<CarrosAdapter.CarroViewHolder>() {

        inner class CarroViewHolder(val binding: ItemCardCarroBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(
            parent: android.view.ViewGroup,
            viewType: Int
        ): CarroViewHolder {
            val binding = ItemCardCarroBinding.inflate(layoutInflater, parent, false)
            return CarroViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CarroViewHolder, position: Int) {
            val carro = carros[position]

            if (carro.tipo == "moto") {
                holder.binding.ivCarIcon.setImageResource(R.drawable.ic_moto)
            } else {
                holder.binding.ivCarIcon.setImageResource(R.drawable.ic_vehicle)
            }

            holder.binding.tvCarModel.text = "${carro.marca} ${carro.modelo}"
            holder.binding.tvCarPlate.text = carro.placa

            holder.binding.toggleFavoriteCar.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Toast.makeText(
                        holder.itemView.context,
                        "${carro.placa} definido como principal!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(this@CarroActivity, EditarCarroActivity::class.java).apply {
                    putExtra("carroId", carro.id)
                    putExtra("placa", carro.placa)
                    putExtra("marca", carro.marca)
                    putExtra("modelo", carro.modelo)
                    putExtra("ano", carro.ano)
                    putExtra("km", carro.km)
                    putExtra("tipo", carro.tipo) // Envia o tipo para a tela de edição também
                }
                editarCarroLauncher.launch(intent)
            }
        }
        override fun getItemCount(): Int = carros.size
    }
}
