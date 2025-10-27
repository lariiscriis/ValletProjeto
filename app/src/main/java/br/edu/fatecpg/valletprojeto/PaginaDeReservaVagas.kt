package br.edu.fatecpg.valletprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import br.edu.fatecpg.valletprojeto.adapter.ParkingSpotAdapter
import br.edu.fatecpg.valletprojeto.dao.ParkingSpotDao
import br.edu.fatecpg.valletprojeto.databinding.ActivityPaginaDeReservaVagasBinding

class PaginaDeReservaVagas : AppCompatActivity() {

    private lateinit var binding: ActivityPaginaDeReservaVagasBinding
    private lateinit var parkingSpotDao: ParkingSpotDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaginaDeReservaVagasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        parkingSpotDao = ParkingSpotDao()

        // Configura o RecyclerView com layout manager logo no início
        binding.rvParkingSpots.layoutManager = GridLayoutManager(this, 3)

        // Busca as vagas do Firestore de forma assíncrona
        parkingSpotDao.listarTodasAsVagas(
            onSuccess = { vagas ->
                runOnUiThread {
                    val adapter = ParkingSpotAdapter(vagas) { vagaSelecionada ->
                        // *** COLOQUE AQUI ***
                        // Quando a vaga for clicada, cria o Intent e abre a ReservaActivity passando os dados da vaga
                        val intent = Intent(this, ReservaActivity::class.java).apply {
                            putExtra("VAGA_ID", vagaSelecionada.id)
//                            putExtra("ESTACIONAMENTO_ID", vagaSelecionada.estacionamentoId)
                        }
                        startActivity(intent)

                        Toast.makeText(this, "Vaga selecionada: ${vagaSelecionada.numero}", Toast.LENGTH_SHORT).show()
                    }
                    binding.rvParkingSpots.adapter = adapter
                }
            },
            onError = { erro ->
                runOnUiThread {
                    Toast.makeText(this, "Erro ao carregar vagas: $erro", Toast.LENGTH_LONG).show()
                }
            }
        )
        binding.btnContinue.setOnClickListener {
            // ação futura
        }
    }
}
