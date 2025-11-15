package br.edu.fatecpg.valletprojeto.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.ReservaActivity
import br.edu.fatecpg.valletprojeto.databinding.ItemVagaBinding // Importe o ViewBinding gerado
import br.edu.fatecpg.valletprojeto.model.Vaga

// 1. Remova 'listaVagas' do construtor e herde de ListAdapter
class VagasAdapter(
    private val isAdmin: Boolean,
    private val onEditClick: (Vaga) -> Unit,
    private val onDeleteClick: (Vaga) -> Unit
) : ListAdapter<Vaga, VagasAdapter.VagaViewHolder>(VagaDiffCallback) {

    // 2. O ViewHolder agora usa ViewBinding para acesso seguro às views
    inner class VagaViewHolder(private val binding: ItemVagaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(vaga: Vaga) {
            // Use 'binding' para acessar os componentes do layout
            binding.tvTitulo.text = "Vaga ${vaga.numero}"
            binding.tvLocalizacao.text = vaga.localizacao
            binding.tvPreco.text = "R$ %.2f/h".format(vaga.preco)

            if (isAdmin) {
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnExcluir.visibility = View.VISIBLE
                binding.btnEdit.setOnClickListener { onEditClick(vaga) }
                binding.btnExcluir.setOnClickListener { onDeleteClick(vaga) }
                binding.root.setOnClickListener(null) // Desativa o clique no item inteiro para o admin
            } else {
                binding.btnEdit.visibility = View.GONE
                binding.btnExcluir.visibility = View.GONE
                binding.root.setOnClickListener {
                    val context = itemView.context
                    val intent = Intent(context, ReservaActivity::class.java).apply {
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VagaViewHolder {
        // Infla o layout usando o ViewBinding
        val binding = ItemVagaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VagaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VagaViewHolder, position: Int) {
        // 3. Use o método getItem() do ListAdapter para pegar o objeto Vaga
        holder.bind(getItem(position))
    }

    // 4. O DiffUtil é obrigatório para o ListAdapter. Ele calcula as diferenças
    //    entre as listas de forma eficiente.
    companion object {
        private val VagaDiffCallback = object : DiffUtil.ItemCallback<Vaga>() {
            override fun areItemsTheSame(oldItem: Vaga, newItem: Vaga): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Vaga, newItem: Vaga): Boolean {
                return oldItem == newItem
            }
        }
    }
}
