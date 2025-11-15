// OccupationAdapter.kt
package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.databinding.ItemOccupationBinding // 1. Importe o View Binding
import br.edu.fatecpg.valletprojeto.model.VagaOcupada
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

// 2. Mude para ListAdapter para eficiência
class OccupationAdapter : ListAdapter<VagaOcupada, OccupationAdapter.ViewHolder>(VagaOcupadaDiffCallback) {

    // 3. O ViewHolder agora recebe o Binding, não a View
    inner class ViewHolder(private val binding: ItemOccupationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vaga: VagaOcupada) {
            // 4. Use 'binding' para acessar as views de forma segura
            binding.tvVagaNumero.text = "Vaga: ${vaga.numeroVaga}"

            val agora = Date()
            if (vaga.horaFimDate.after(agora)) {
                binding.tvReservaStatus.text = "Em uso"
                binding.tvReservaStatus.setTextColor(itemView.context.getColor(R.color.verdeescuro))
                binding.tvAlertaReserva.visibility = View.GONE
            } else {
                binding.tvReservaStatus.text = "Vencida"
                binding.tvReservaStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                binding.tvAlertaReserva.visibility = View.VISIBLE
                binding.tvAlertaReserva.text = "⚠️ Tempo de reserva expirado!"
            }

            binding.tvMotoristaNome.text = vaga.motoristaNome
            binding.tvMotoristaEmail.text = vaga.motoristaEmail
            binding.tvMotoristaTelefone.text = vaga.motoristaTelefone
            binding.tvCarroInfo.text = "Veículo: ${vaga.carroModelo} - ${vaga.carroPlaca}"

            val formato = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val inicioFmt = formato.format(vaga.horaInicioDate)
            val fimFmt = formato.format(vaga.horaFimDate)
            binding.tvReservaDetalhes.text = "Reserva: $inicioFmt → $fimFmt"

            val preferencialTxt = if (vaga.preferencial) "Preferencial" else "Comum"
            binding.tvVagaLocalizacao.text = "Local: ${vaga.localizacao} • $preferencialTxt"

            Glide.with(itemView.context)
                .load(vaga.motoristaFoto)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(binding.ivMotoristaFoto)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 5. Infla o layout usando o View Binding
        val binding = ItemOccupationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 6. Adicione o DiffUtil para o ListAdapter
    companion object {
        private val VagaOcupadaDiffCallback = object : DiffUtil.ItemCallback<VagaOcupada>() {
            override fun areItemsTheSame(oldItem: VagaOcupada, newItem: VagaOcupada): Boolean {
                // Supondo que a combinação de placa e vaga seja única para uma reserva ativa
                return oldItem.carroPlaca == newItem.carroPlaca && oldItem.numeroVaga == newItem.numeroVaga
            }
            override fun areContentsTheSame(oldItem: VagaOcupada, newItem: VagaOcupada): Boolean {
                return oldItem == newItem
            }
        }
    }
}
