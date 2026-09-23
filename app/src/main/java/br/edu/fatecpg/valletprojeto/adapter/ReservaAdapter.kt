package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.model.Reserva
import java.text.SimpleDateFormat
import java.util.Locale

class ReservaAdapter : ListAdapter<Reserva, ReservaAdapter.ReservaViewHolder>(ReservaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reserva, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReservaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEstacionamentoNome: TextView = itemView.findViewById(R.id.tvEstacionamentoNome)
        private val tvPeriodo: TextView = itemView.findViewById(R.id.tvPeriodo)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(reserva: Reserva) {
            tvEstacionamentoNome.text = reserva.estacionamentoNome

            val inicioStr = reserva.inicioReserva?.toDate()?.let { sdf.format(it) } ?: "N/A"
            val fimStr = reserva.fimReserva?.toDate()?.let { sdf.format(it) } ?: "N/A"
            tvPeriodo.text = "Início: $inicioStr - Fim: $fimStr"

            val statusFormatado = reserva.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            tvStatus.text = "Status: $statusFormatado"
        }
    }
}

class ReservaDiffCallback : DiffUtil.ItemCallback<Reserva>() {
    override fun areItemsTheSame(oldItem: Reserva, newItem: Reserva): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Reserva, newItem: Reserva): Boolean {
        return oldItem == newItem
    }
}