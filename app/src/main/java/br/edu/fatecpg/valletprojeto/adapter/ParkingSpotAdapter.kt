package br.edu.fatecpg.valletprojeto.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
//import br.edu.fatecpg.valletprojeto.dao.ParkingSpot
import br.edu.fatecpg.valletprojeto.model.Vaga

class ParkingSpotAdapter(
    private val spots: List<Vaga>,
    private val onItemClick: (Vaga) -> Unit
) : RecyclerView.Adapter<ParkingSpotAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSpotNumber: TextView = itemView.findViewById(R.id.tvSpotNumber)
        val tvSpotStatus: TextView = itemView.findViewById(R.id.tvSpotStatus)

        fun bind(spot: Vaga) {
            tvSpotNumber.text = spot.numero
            tvSpotStatus.text = if (spot.disponivel) "Disponível" else "Reservada"

            // Muda a cor com base no status
            tvSpotStatus.setTextColor(
                if (spot.disponivel != true) Color.parseColor("#F44336")
                else Color.parseColor("#4CAF50")
            )

            itemView.setOnClickListener {
                onItemClick(spot)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pagina_reservas_vagas, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(spots[position])
    }

    override fun getItemCount() = spots.size
}
