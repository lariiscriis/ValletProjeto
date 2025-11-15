package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.databinding.ItemFavoriteParkingBinding
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FavoriteParkingAdapter(
    private val onFavoriteClicked: (Estacionamento) -> Unit,
    private val onItemClicked: (Estacionamento) -> Unit
) : ListAdapter<Estacionamento, FavoriteParkingAdapter.ViewHolder>(ParkingDiffCallback) {

    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val db = FirebaseFirestore.getInstance()

    inner class ViewHolder(val binding: ItemFavoriteParkingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(estacionamento: Estacionamento) {
            binding.tvParkingName.text = estacionamento.nome
            binding.tvParkingStatus.text = if (estacionamento.estaAberto()) "ABERTO" else "FECHADO"
            binding.tvParkingPrice.text = "R$%.2f/h".format(estacionamento.valorHora)
            Glide.with(binding.root.context)
                .load(estacionamento.fotoEstacionamentoUri)
                .placeholder(R.drawable.estacionamento_foto)
                .error(R.drawable.estacionamento_foto)
                .into(binding.ivParkingImage)

            binding.ivFavorite.setOnClickListener { onFavoriteClicked(estacionamento) }

            binding.root.setOnClickListener { onItemClicked(estacionamento) }
            binding.ivFavorite.setImageResource(R.drawable.btn_star_on)

            setupVagasListener(estacionamento)
        }

        private fun setupVagasListener(estacionamento: Estacionamento) {
            listeners[estacionamento.id]?.remove()
            val listener = db.collection("vaga")
                .whereEqualTo("estacionamentoId", estacionamento.id)
                .whereEqualTo("disponivel", true)
                .addSnapshotListener { snapshot, e ->
                    binding.tvTavInfo.text = "Vagas disponíveis: ${snapshot?.size() ?: "N/A"}"
                }
            listeners[estacionamento.id] = listener
        }
    }

    companion object {
        private val ParkingDiffCallback = object : DiffUtil.ItemCallback<Estacionamento>() {
            override fun areItemsTheSame(oldItem: Estacionamento, newItem: Estacionamento): Boolean {
                return oldItem.id == newItem.id
            }
            override fun areContentsTheSame(oldItem: Estacionamento, newItem: Estacionamento): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteParkingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}
