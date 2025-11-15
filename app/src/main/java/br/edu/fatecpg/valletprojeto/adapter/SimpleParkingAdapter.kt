package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.databinding.ItemParkingBinding
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// --- MUDANÇA 1: Herdar de ListAdapter ---
// Trocamos RecyclerView.Adapter por ListAdapter.
// Isso nos dá o método submitList() e animações automáticas de graça.
class SimpleParkingAdapter(
    private val onClick: (Estacionamento) -> Unit
) : ListAdapter<Estacionamento, SimpleParkingAdapter.ViewHolder>(ParkingDiffCallback) {

    // Mapa para manter os listeners de vagas, evitando leaks.
    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val db = FirebaseFirestore.getInstance()

    // --- MUDANÇA 2: ViewHolder otimizado ---
    // A lógica de listeners e favoritos foi simplificada.
    inner class ViewHolder(private val binding: ItemParkingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val favoritoRef = db.collection("favoritos")
        private val usuarioId = FirebaseAuth.getInstance().currentUser?.uid

        fun bind(estacionamento: Estacionamento) {
            // --- Sua lógica de UI original, mantida e organizada ---
            binding.tvParkingName.text = estacionamento.nome
            binding.tvParkingStatus.text = if (estacionamento.estaAberto()) "ABERTO" else "FECHADO"
            binding.tvParkingPrice.text = "R$%.2f/h".format(estacionamento.valorHora)
            binding.tvParkingAddress.text = estacionamento.endereco

            // Formatação de distância melhorada
            binding.tvParkingDistance.text = formatarDistancia(estacionamento.distanciaMetros)

            Glide.with(binding.root.context)
                .load(estacionamento.fotoEstacionamentoUri)
                .placeholder(R.drawable.estacionamento_foto)
                .centerCrop()
                .into(binding.ivParkingImage)

            // Listeners de clique
            binding.root.setOnClickListener { onClick(estacionamento) }
            binding.ivFavorite.setOnClickListener { toggleFavorito(estacionamento.id) }

            // O clique no botão de vagas agora é tratado pelo clique no item inteiro.
            // Se precisar de lógica específica, pode ser adicionada aqui.

            // Atualiza o ícone de favorito e inicia o listener de vagas
            atualizarIconeFavorito(estacionamento.id)
            iniciarListenerVagas(estacionamento)
        }

        private fun formatarDistancia(metros: Int?): String {
            return metros?.let {
                if (it >= 1000) "%.1f km".format(it / 1000.0) else "$it m"
            } ?: "N/A"
        }

        private fun toggleFavorito(estacionamentoId: String) {
            val uid = usuarioId ?: return // Não faz nada se o usuário não estiver logado
            val context = binding.root.context

            favoritoRef.whereEqualTo("usuarioId", uid)
                .whereEqualTo("estacionamentoId", estacionamentoId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        // Adiciona favorito
                        favoritoRef.add(mapOf("usuarioId" to uid, "estacionamentoId" to estacionamentoId))
                            .addOnSuccessListener {
                                binding.ivFavorite.setImageResource(R.drawable.btn_star_on)
                                Toast.makeText(context, "Adicionado aos favoritos", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Remove favorito
                        snapshot.documents.first().reference.delete()
                            .addOnSuccessListener {
                                binding.ivFavorite.setImageResource(R.drawable.btn_star_off)
                                Toast.makeText(context, "Removido dos favoritos", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }

        private fun atualizarIconeFavorito(estacionamentoId: String) {
            val uid = usuarioId ?: return
            favoritoRef.whereEqualTo("usuarioId", uid)
                .whereEqualTo("estacionamentoId", estacionamentoId)
                .get()
                .addOnSuccessListener { snapshot ->
                    binding.ivFavorite.setImageResource(
                        if (snapshot.isEmpty) R.drawable.btn_star_off else R.drawable.btn_star_on
                    )
                }
        }

        private fun iniciarListenerVagas(estacionamento: Estacionamento) {
            val estacionamentoId = estacionamento.id
            // Remove o listener antigo para este ID, se houver, para evitar duplicação
            listeners[estacionamentoId]?.remove()

            val listener = db.collection("vaga")
                .whereEqualTo("estacionamentoId", estacionamentoId)
                .whereEqualTo("disponivel", true)
                .addSnapshotListener { snapshot, _ ->
                    val vagasDisponiveis = snapshot?.size() ?: 0
                    // Atualiza o texto de vagas
                    binding.tvTavInfo.text = "Vagas disponíveis: $vagasDisponiveis"
                }
            // Armazena o novo listener
            listeners[estacionamentoId] = listener
        }
    }

    object ParkingDiffCallback : DiffUtil.ItemCallback<Estacionamento>() {
        override fun areItemsTheSame(oldItem: Estacionamento, newItem: Estacionamento): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Estacionamento, newItem: Estacionamento): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParkingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
