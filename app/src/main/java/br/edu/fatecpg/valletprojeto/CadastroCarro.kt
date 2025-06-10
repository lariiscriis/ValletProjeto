package br.edu.fatecpg.valletprojeto

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroCarroBinding
import br.edu.fatecpg.valletprojeto.databinding.FormCarroBinding
import br.edu.fatecpg.valletprojeto.databinding.ItemCardCarroBinding
import  br.edu.fatecpg.valletprojeto.Carro

class CadastroCarro : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroCarroBinding
    private val listaForms = mutableListOf<FormCarroBinding>()
    private val carrosCadastrados = mutableListOf<Carro>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCadastroCarroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adicionarNovoFormulario()

        binding.btnAdicionarCarro.setOnClickListener {
            adicionarNovoFormulario()
        }

        binding.btnFinalizar.setOnClickListener {
            Toast.makeText(this, "Cadastro finalizado com ${carrosCadastrados.size} carro(s)!", Toast.LENGTH_SHORT).show()
            // Aqui você pode fazer algo com a lista de carros, como enviar para outra tela
        }
    }

    private fun adicionarNovoFormulario() {
        val novoFormularioBinding = FormCarroBinding.inflate(layoutInflater, binding.layoutForms, false)
        binding.layoutForms.addView(novoFormularioBinding.root)
        listaForms.add(novoFormularioBinding)

        novoFormularioBinding.btnConfirmar.setOnClickListener {
            val placa = novoFormularioBinding.editPlaca.text.toString().trim()
            val marca = novoFormularioBinding.editMarca.text.toString().trim()
            val modelo = novoFormularioBinding.editModelo.text.toString().trim()

            if (placa.isNotEmpty() && marca.isNotEmpty() && modelo.isNotEmpty()) {
                val novoCarro = Carro(placa, marca, modelo)
                carrosCadastrados.add(novoCarro)

                mostrarCardCarro(novoCarro)
                binding.layoutForms.removeView(novoFormularioBinding.root)
                listaForms.remove(novoFormularioBinding)
                binding.btnFinalizar.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarCardCarro(carro: Carro) {
        val cardBinding = ItemCardCarroBinding.inflate(layoutInflater, binding.layoutCards, false)
        cardBinding.txtPlaca.text = "Placa: ${carro.placa}"
        cardBinding.txtMarca.text = "Marca: ${carro.marca}"
        cardBinding.txtModelo.text = "Modelo: ${carro.modelo}"
        binding.layoutCards.addView(cardBinding.root)
    }
}
