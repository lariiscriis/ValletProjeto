package br.edu.fatecpg.valletprojeto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.databinding.ItemFavoriteParkingBinding
import br.edu.fatecpg.valletprojeto.model.Estacionamento
import br.edu.fatecpg.valletprojeto.viewmodel.VagaViewModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavoriteParkingAdapter(
    private val favoritos: List<Estacionamento>,
    private val onClick: (Estacionamento) -> Unit
) : RecyclerView.Adapter<FavoriteParkingAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFavoriteParkingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val favoritoRef = FirebaseFirestore.getInstance().collection("favoritos")
        private val usuarioId = FirebaseAuth.getInstance().currentUser?.uid

        fun bind(estacionamento: Estacionamento) {
            val aberto = verificarSeEstaAberto(
                estacionamento.horarioAbertura ?: "",
                estacionamento.horarioFechamento ?: ""
            )

            val db = FirebaseFirestore.getInstance()
            var vagasListener: ListenerRegistration? = null

            fun iniciarListenerVagas() {
                vagasListener?.remove()

                vagasListener = db.collection("vaga")
                    .whereEqualTo("estacionamentoId", estacionamento.id)
                    .whereEqualTo("disponivel", true)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            binding.tvTavInfo.text = "Vagas disponíveis: -"
                            return@addSnapshotListener
                        }

                        val vagasDisponiveis = snapshot?.size() ?: 0
                        binding.tvTavInfo.text = "Vagas disponíveis: $vagasDisponiveis"
                        estacionamento.vagasDisponiveis = vagasDisponiveis
                    }
            }

            iniciarListenerVagas()

            binding.tvParkingName.text = estacionamento.nome
            binding.tvParkingStatus.text =
                if (estacionamento.estaAberto()) "ABERTO" else "FECHADO"
            binding.tvParkingPrice.text = "R$%.2f/h".format(estacionamento.valorHora)

            atualizarIconeFavorito(estacionamento.id)

            binding.ivFavorite.setOnClickListener {
                if (usuarioId == null) return@setOnClickListener

                favoritoRef.whereEqualTo("usuarioId", usuarioId)
                    .whereEqualTo("estacionamentoId", estacionamento.id)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            // Adiciona favorito
                            favoritoRef.add(
                                mapOf(
                                    "usuarioId" to usuarioId,
                                    "estacionamentoId" to estacionamento.id
                                )
                            ).addOnSuccessListener {
                                binding.ivFavorite.setImageResource(R.drawable.btn_star_on)
                                Toast.makeText(
                                    binding.root.context,
                                    "Adicionado aos favoritos",
                                    Toast.LENGTH_SHORT
                                ).show()
                                iniciarListenerVagas()
                            }
                        } else {
                            snapshot.documents.first().reference.delete()
                                .addOnSuccessListener {
                                    binding.ivFavorite.setImageResource(R.drawable.btn_star_off)
                                    Toast.makeText(
                                        binding.root.context,
                                        "Removido dos favoritos",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    iniciarListenerVagas()
                                }
                        }
                    }
            }

            Glide.with(binding.root.context)
                .load(estacionamento.fotoEstacionamentoUri)
                .placeholder(R.drawable.estacionamento_foto)
                .into(binding.ivParkingImage)

            binding.btnViewSpots.setOnClickListener {
                val context = binding.root.context
                val vagaViewModel = VagaViewModel()
                if (aberto) {
                    vagaViewModel.verificarSeTemVagas(estacionamento.id) { temVagas ->
                        if (temVagas) onClick(estacionamento)
                        else Toast.makeText(context, "Nenhuma vaga disponível.", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(context, "O estacionamento está fechado.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }



        private fun atualizarIconeFavorito(estacionamentoId: String) {
            if (usuarioId == null) return
            favoritoRef.whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("estacionamentoId", estacionamentoId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        binding.ivFavorite.setImageResource(R.drawable.btn_star_off)
                    } else {
                        binding.ivFavorite.setImageResource(R.drawable.btn_star_on)
                    }
                }
        }

        private fun verificarSeEstaAberto(horaAbertura: String, horaFechamento: String): Boolean {
            return try {
                if (horaAbertura.isBlank() || horaFechamento.isBlank()) return true
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val agora = sdf.parse(sdf.format(Date()))
                val abertura = sdf.parse(horaAbertura)
                val fechamento = sdf.parse(horaFechamento)
                if (abertura != null && fechamento != null && agora != null) {
                    if (fechamento.before(abertura)) {
                        agora.after(abertura) || agora.before(fechamento)
                    } else {
                        agora.after(abertura) && agora.before(fechamento)
                    }
                } else false
            } catch (e: Exception) {
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteParkingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(favoritos[position])
    }

    override fun getItemCount() = favoritos.size
}
