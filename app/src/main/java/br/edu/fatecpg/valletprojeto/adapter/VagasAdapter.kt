package br.edu.fatecpg.valletprojeto.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.ReservaActivity
import br.edu.fatecpg.valletprojeto.model.Vaga

class VagasAdapter(
    private val listaVagas: List<Vaga>,
    private val isAdmin: Boolean,
    private val onEditClick: (Vaga) -> Unit,
    private val onDeleteClick: (Vaga) -> Unit
) : RecyclerView.Adapter<VagasAdapter.VagaViewHolder>() {

    inner class VagaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val localizacao: TextView = itemView.findViewById(R.id.tvLocalizacao)
        val preco: TextView = itemView.findViewById(R.id.tvSalario)
        val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        val btnExcluir: Button = itemView.findViewById(R.id.btn_excluir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VagaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vaga, parent, false)
        return VagaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VagaViewHolder, position: Int) {
        val vaga = listaVagas[position]

        holder.titulo.text = vaga.numero
        holder.localizacao.text = vaga.localizacao
        holder.preco.text = "R$ ${vaga.preco}"

        if (isAdmin) {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnExcluir.visibility = View.VISIBLE

            holder.btnEdit.setOnClickListener { onEditClick(vaga) }
            holder.btnExcluir.setOnClickListener { onDeleteClick(vaga) }
        } else {
            holder.btnEdit.visibility = View.GONE
            holder.btnExcluir.visibility = View.GONE

            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context,ReservaActivity::class.java).apply {
                    putExtra("vagaId", vaga.id)
                    putExtra("numero", vaga.numero)
                    putExtra("preco", vaga.preco)
                    putExtra("tipo", vaga.tipo)
                    putExtra("estacionamentoId", vaga.estacionamentoId)
                }
                context.startActivity(intent)
            }
        }
    }


    override fun getItemCount(): Int = listaVagas.size
}
