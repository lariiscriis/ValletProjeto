package br.edu.fatecpg.valletprojeto

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroCarroBinding
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroEstacionamentoBinding

class CadastroEstacionamento : AppCompatActivity() {
    private lateinit var binding: ActivityCadastroEstacionamentoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCadastroEstacionamentoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}