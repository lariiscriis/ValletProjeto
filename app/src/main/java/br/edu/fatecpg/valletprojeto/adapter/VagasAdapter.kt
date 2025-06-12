package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.model.Vaga

class VagasAdapter(
    private val vagas: List<Vaga>,
    private val onEditClick: (Vaga) -> Unit,
    private val onDeleteClick: (Vaga) -> Unit
) : RecyclerView.Adapter<VagasAdapter.VagaViewHolder>() {

    inner class VagaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvLocalizacao: TextView = itemView.findViewById(R.id.tvLocalizacao)
        private val tvSalario: TextView = itemView.findViewById(R.id.tvSalario)
        private val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
        private val btnExcluir: Button = itemView.findViewById(R.id.btnExcluir)

        fun bind(vaga: Vaga) {
            tvTitulo.text = vaga.numero
            tvLocalizacao.text = vaga.localizacao
            tvSalario.text = vaga.preco.toString()

            btnEditar.setOnClickListener { onEditClick(vaga) }
            btnExcluir.setOnClickListener { onDeleteClick(vaga) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VagaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vaga, parent, false)
        return VagaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VagaViewHolder, position: Int) {
        holder.bind(vagas[position])
    }

    override fun getItemCount() = vagas.size

}