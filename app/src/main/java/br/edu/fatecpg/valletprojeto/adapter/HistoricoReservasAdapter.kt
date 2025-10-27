package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.databinding.ItemReservaHistoricoRecenteBinding
import br.edu.fatecpg.valletprojeto.model.ReservaHistorico

class HistoricoReservasAdapter(
    private val listaReservas: List<ReservaHistorico>
) : RecyclerView.Adapter<HistoricoReservasAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReservaHistoricoRecenteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReservaHistoricoRecenteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reserva = listaReservas[position]
        holder.binding.tvHistorySpot.text = reserva.vaga
        holder.binding.tvHistoryDate.text = reserva.data
        holder.binding.tvHistoryTime.text = reserva.horario
    }

    override fun getItemCount() = listaReservas.size
}