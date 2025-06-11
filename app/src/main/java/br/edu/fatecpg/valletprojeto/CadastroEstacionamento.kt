package br.edu.fatecpg.valletprojeto

import Estacionamento
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroEstacionamentoBinding
import br.edu.fatecpg.valletprojeto.viewmodel.EstacionamentoViewModel

class CadastroEstacionamento : AppCompatActivity() {
    private lateinit var binding: ActivityCadastroEstacionamentoBinding
    private val vm: EstacionamentoViewModel by viewModels()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityCadastroEstacionamentoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCadastrar.setOnClickListener {
            val est = Estacionamento(
                nome = binding.edtNomeEstacionamento.text.toString().trim(),
                cnpj = binding.edtCnpj.text.toString().trim(),
                telefone = binding.edtTelefone.text.toString().trim(),
                endereco = binding.edtEndereco.text.toString().trim(),
                cep = binding.edtCep.text.toString().trim(),
                quantidadeVagasTotal = binding.edtVagasTotal.text.toString().toIntOrNull() ?: 0,
                tiposVagasComum = true,
                tiposVagasIdosoPcd = false,
                quantidadeVagasComum = null,
                quantidadeVagasIdosoPcd = null,
                possuiCobertura = false,
                numeroPavimentos = null,
                valorHora = binding.edtValorHora.text.toString().toDoubleOrNull() ?: 0.0,
                valorDiario = 0.0,
                horarioFuncionamento = formatarHora(binding.edtAbertura.text.toString()) + "-" +
                    formatarHora(binding.edtFechamento.text.toString()),
                tempoMaxReservaHoras = 2,
                toleranciaReservaMinutos = 0,
                fotoEstacionamentoUri = null
            )
            vm.cadastrar(est,
                onSuccess = {
                    Toast.makeText(this, "Estacionamento cadastrado!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { msg ->
                    Toast.makeText(this, "Erro: $msg", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    private fun formatarHora(horaTexto: String): String {
        val hora = horaTexto.padStart(4, '0')
        val horas = hora.substring(0, 2)
        val minutos = hora.substring(2, 4)
        return "$horas:$minutos"
    }
}
