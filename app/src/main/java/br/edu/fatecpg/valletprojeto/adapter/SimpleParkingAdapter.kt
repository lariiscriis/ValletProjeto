package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R

class SimpleParkingAdapter : RecyclerView.Adapter<SimpleParkingAdapter.ParkingViewHolder>() {

    private val itemCount = 3 // Número de cards estáticos

    class ParkingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking, parent, false)
        return ParkingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        // Não faz nada, apenas exibe o layout como está no XML
    }

    override fun getItemCount() = itemCount
}