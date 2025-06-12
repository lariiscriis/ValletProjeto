package br.edu.fatecpg.valletprojeto

import ParkingSpotAdapter
import android.R
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.dao.ParkingSpot
import br.edu.fatecpg.valletprojeto.databinding.ActivityPaginaDeReservaVagasBinding


class PaginaDeReservaVagas : AppCompatActivity() {
    private lateinit var binding: ActivityPaginaDeReservaVagasBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaginaDeReservaVagasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rvParkingSpots = binding.rvParkingSpots
        rvParkingSpots.layoutManager = GridLayoutManager(this, 3)

        // Criar lista de vagas
        val spots = mutableListOf(
            ParkingSpot("634", "Available"),
            ParkingSpot("636", "Available"),
            ParkingSpot("637", "Available"),
            ParkingSpot("640", "Available"),
            ParkingSpot("641", "Available"),
            ParkingSpot("643", "Available"),
            ParkingSpot("645", "Available")
        )

        val adapter = ParkingSpotAdapter(spots)
        rvParkingSpots.adapter = adapter

        binding.btnContinue.setOnClickListener {
        }

    }
}