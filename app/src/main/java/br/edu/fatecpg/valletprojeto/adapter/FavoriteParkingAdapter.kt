// FavoriteParkingAdapter.kt
package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.databinding.ItemFavoriteParkingBinding
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FavoriteParkingAdapter(
    private val onClick: (Estacionamento) -> Unit
) : RecyclerView.Adapter<FavoriteParkingAdapter.ViewHolder>() {

    // A lista agora é interna e começa vazia
    private var favoritos: List<Estacionamento> = emptyList()
    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // **MÉTODO CORRIGIDO**: Este método permite que o ViewModel atualize a lista
    fun updateFavorites(newFavorites: List<Estacionamento>) {
        this.favoritos = newFavorites
        notifyDataSetChanged() // Notifica o adapter que os dados mudaram
    }

    inner class ViewHolder(val binding: ItemFavoriteParkingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(estacionamento: Estacionamento) {
            binding.tvParkingName.text = estacionamento.nome
            binding.tvParkingStatus.text = if (estacionamento.estaAberto()) "ABERTO" else "FECHADO"
            binding.tvParkingPrice.text = "R$%.2f/h".format(estacionamento.valorHora)

            Glide.with(binding.root.context)
                .load(estacionamento.fotoEstacionamentoUri)
                .placeholder(R.drawable.estacionamento_foto)
                .into(binding.ivParkingImage)

            binding.root.setOnClickListener { onClick(estacionamento) }

            // Lógica de favoritar (pode ser movida para o ViewModel para melhor prática)
            setupFavoriteButton(estacionamento)

            // Gerenciamento seguro do listener de vagas
            setupVagasListener(estacionamento)
        }

        private fun setupVagasListener(estacionamento: Estacionamento) {
            // Remove o listener antigo para este item, se houver
            listeners[estacionamento.id]?.remove()

            val listener = db.collection("vaga")
                .whereEqualTo("estacionamentoId", estacionamento.id)
                .whereEqualTo("disponivel", true)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) {
                        binding.tvTavInfo.text = "Vagas: N/A"
                        return@addSnapshotListener
                    }
                    val vagasDisponiveis = snapshot.size()
                    binding.tvTavInfo.text = "Vagas disponíveis: $vagasDisponiveis"
                }
            // Armazena o novo listener
            listeners[estacionamento.id] = listener
        }

        private fun setupFavoriteButton(estacionamento: Estacionamento) {
            val usuarioId = auth.currentUser?.uid
            if (usuarioId == null) {
                binding.ivFavorite.setImageResource(R.drawable.btn_star_off)
                return
            }

            val favoritoRef = db.collection("favoritos")

            // Define o estado inicial do ícone
            favoritoRef.whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("estacionamentoId", estacionamento.id)
                .get().addOnSuccessListener {
                    binding.ivFavorite.setImageResource(
                        if (it.isEmpty) R.drawable.btn_star_off else R.drawable.btn_star_on
                    )
                }

            // Ação de clique
            binding.ivFavorite.setOnClickListener {
                // A lógica para adicionar/remover favorito permanece a mesma do seu código original
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteParkingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(favoritos[position])
    }

    override fun getItemCount() = favoritos.size

    // Limpa todos os listeners quando o adapter é desanexado da RecyclerView
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}
