package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.fatecpg.valletprojeto.databinding.ActivityCadastroCarroBinding
import br.edu.fatecpg.valletprojeto.databinding.ActivityIntroCadastroCarroBinding
import com.bumptech.glide.Glide

class IntroCadastroCarro : AppCompatActivity() {
    private lateinit var binding: ActivityIntroCadastroCarroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityIntroCadastroCarroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Glide.with(this)
            .asGif()
            .load(R.drawable.car_intro_animado)
            .into(binding.gifCarro)


        binding.btnIrParaCadastro.setOnClickListener {
            val intent = Intent(this, CadastroCarro::class.java)
            startActivity(intent)
            }
        }
    }
