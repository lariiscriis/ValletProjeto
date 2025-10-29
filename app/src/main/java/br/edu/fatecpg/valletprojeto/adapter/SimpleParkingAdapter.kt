package br.edu.fatecpg.valletprojeto.adapter

import android.widget.Toast
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.databinding.ItemParkingBinding
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import br.edu.fatecpg.valletprojeto.viewmodel.VagaViewModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SimpleParkingAdapter(
    private val onClick: (Estacionamento) -> Unit
) : RecyclerView.Adapter<SimpleParkingAdapter.ViewHolder>() {

    private val lista = mutableListOf<Estacionamento>()
    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val db = FirebaseFirestore.getInstance()
    private val favoritoRef = db.collection("favoritos")
    private val usuarioId = FirebaseAuth.getInstance().currentUser?.uid

    fun submitList(novaLista: List<Estacionamento>) {
        val idsAtuais = novaLista.map { it.id }.toSet()
        listeners.keys.filter { it !in idsAtuais }.forEach { id ->
            listeners[id]?.remove()
            listeners.remove(id)
        }

        lista.clear()
        lista.addAll(novaLista)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemParkingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(estacionamento: Estacionamento) {
            // Nome, status e preço
            binding.tvParkingName.text = estacionamento.nome
            binding.tvParkingStatus.text = if (estacionamento.estaAberto()) "ABERTO" else "FECHADO"
            binding.tvParkingPrice.text = "R$%.2f/h".format(estacionamento.valorHora)

            // Endereço
            binding.tvParkingAddress.text = estacionamento.endereco

            // Distância
            binding.tvParkingDistance.text = estacionamento.distanciaMetros?.let { "$it m" } ?: "Distância não disponível"

            // Foto
            Glide.with(binding.root.context)
                .load(estacionamento.fotoEstacionamentoUri)
                .placeholder(R.drawable.estacionamento_foto)
                .centerCrop() // ajusta escala corretamente
                .into(binding.ivParkingImage)

            // Favorito
            atualizarIconeFavorito(estacionamento.id)
            binding.ivFavorite.setOnClickListener {
                usuarioId?.let { toggleFavorito(estacionamento.id) }
            }

            // Botão vagas
            binding.btnViewSpots.setOnClickListener {
                val context = binding.root.context
                val vagaViewModel = VagaViewModel()
                if (estacionamento.estaAberto()) {
                    vagaViewModel.verificarSeTemVagas(estacionamento.id) { temVagas ->
                        if (temVagas) onClick(estacionamento)
                        else Toast.makeText(context, "Nenhuma vaga disponível.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "O estacionamento está fechado.", Toast.LENGTH_SHORT).show()
                }
            }

            iniciarListenerVagas(estacionamento)
        }


        private fun toggleFavorito(estacionamentoId: String) {
            usuarioId?.let { uid ->
                favoritoRef.whereEqualTo("usuarioId", uid)
                    .whereEqualTo("estacionamentoId", estacionamentoId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val context = binding.root.context
                        if (snapshot.isEmpty) {
                            favoritoRef.add(mapOf("usuarioId" to uid, "estacionamentoId" to estacionamentoId))
                                .addOnSuccessListener {
                                    binding.ivFavorite.setImageResource(R.drawable.btn_star_on)
                                    Toast.makeText(context, "Adicionado aos favoritos", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            snapshot.documents.first().reference.delete()
                                .addOnSuccessListener {
                                    binding.ivFavorite.setImageResource(R.drawable.btn_star_off)
                                    Toast.makeText(context, "Removido dos favoritos", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
            }
        }

        private fun atualizarIconeFavorito(estacionamentoId: String) {
            usuarioId?.let { uid ->
                favoritoRef.whereEqualTo("usuarioId", uid)
                    .whereEqualTo("estacionamentoId", estacionamentoId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        binding.ivFavorite.setImageResource(
                            if (snapshot.isEmpty) R.drawable.btn_star_off else R.drawable.btn_star_on
                        )
                    }
            }
        }
        private fun atualizarInfo(estacionamento: Estacionamento, vagasDisponiveis: Int? = null) {
            val distanciaTexto = estacionamento.distanciaMetros?.let {
                if (it >= 1000) "%.1f km".format(it / 1000.0) else "$it m"
            } ?: "Distância indisponível"

            val vagasTexto = vagasDisponiveis?.let { " • Vagas disponíveis: $it" } ?: ""

            binding.tvTavInfo.text = "$distanciaTexto$vagasTexto"
        }
        private fun iniciarListenerVagas(estacionamento: Estacionamento) {
            val estacionamentoId = estacionamento.id
            listeners[estacionamentoId]?.remove()

            val listener = db.collection("vaga")
                .whereEqualTo("estacionamentoId", estacionamentoId)
                .whereEqualTo("disponivel", true)
                .addSnapshotListener { snapshot, _ ->
                    val vagasDisponiveis = snapshot?.size() ?: 0
                    binding.tvTavInfo.text = "Vagas disponíveis: $vagasDisponiveis"
                }

            listeners[estacionamentoId] = listener
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParkingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = lista.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(lista[position])
}
