package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.model.VagaOcupada
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class OccupationAdapter(
    private var lista: List<VagaOcupada>
) : RecyclerView.Adapter<OccupationAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvVagaNumero: TextView = itemView.findViewById(R.id.tv_vaga_numero)
        val tvReservaStatus: TextView = itemView.findViewById(R.id.tv_reserva_status)
        val ivMotoristaFoto: ImageView = itemView.findViewById(R.id.iv_motorista_foto)
        val tvMotoristaNome: TextView = itemView.findViewById(R.id.tv_motorista_nome)
        val tvMotoristaEmail: TextView = itemView.findViewById(R.id.tv_motorista_email)
        val tvMotoristaTelefone: TextView = itemView.findViewById(R.id.tv_motorista_telefone)
        val tvCarroInfo: TextView = itemView.findViewById(R.id.tv_carro_info)
        val tvReservaDetalhes: TextView = itemView.findViewById(R.id.tv_reserva_detalhes)
        val tvAlertaReserva: TextView = itemView.findViewById(R.id.tv_alerta_reserva)
        val tvVagaLocalizacao: TextView = itemView.findViewById(R.id.tvVagaLocalizacao)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_occupation, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vaga = lista[position]

        holder.tvVagaNumero.text = "Vaga: ${vaga.numeroVaga}"

        val agora = Date()
        val fim = vaga.horaFimDate
        if (fim.after(agora)) {
            holder.tvReservaStatus.text = "Em uso"
            holder.tvReservaStatus.setTextColor(holder.itemView.context.getColor(R.color.verdeescuro))
            holder.tvAlertaReserva.visibility = View.GONE
        } else {
            holder.tvReservaStatus.text = "Vencida"
            holder.tvReservaStatus.setTextColor(android.graphics.Color.parseColor("#FF0000"))
            holder.tvAlertaReserva.visibility = View.VISIBLE
            holder.tvAlertaReserva.text = "⚠️ Tempo de reserva expirado!"
        }

        // Dados do motorista
        holder.tvMotoristaNome.text = vaga.motoristaNome
        holder.tvMotoristaEmail.text = vaga.motoristaEmail
        holder.tvMotoristaTelefone.text = vaga.motoristaTelefone


        // veiculo
        holder.tvCarroInfo.text = "Veículo: ${vaga.carroModelo} - ${vaga.carroPlaca}"

        // Formatar horários
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val inicioFmt = vaga.horaInicioDate?.let { formato.format(it) } ?: "-"
        val fimFmt = vaga.horaFimDate?.let { formato.format(it) } ?: "-"
        holder.tvReservaDetalhes.text = "Reserva: $inicioFmt → $fimFmt"

        val preferencialTxt = if (vaga.preferencial) "Preferencial" else "Comum"
        holder.tvVagaLocalizacao.text = "Localização: ${vaga.localizacao} • $preferencialTxt"

        if (!vaga.motoristaFoto.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(vaga.motoristaFoto)
                .placeholder(R.drawable.ic_default_profile)
                .into(holder.ivMotoristaFoto)
        } else {
            holder.ivMotoristaFoto.setImageResource(R.drawable.ic_default_profile)
        }
    }


    fun atualizarLista(novaLista: List<VagaOcupada>) {
        lista = novaLista
        notifyDataSetChanged()
    }
}
