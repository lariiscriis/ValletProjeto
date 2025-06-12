import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.dao.ParkingSpot

class ParkingSpotAdapter(private val spots: List<ParkingSpot>) :
    RecyclerView.Adapter<ParkingSpotAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSpotNumber: TextView = itemView.findViewById(R.id.tvSpotNumber)
        val tvSpotStatus: TextView = itemView.findViewById(R.id.tvSpotStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vaga, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val spot = spots[position]
        holder.tvSpotNumber.text = spot.number
        holder.tvSpotStatus.text = spot.status

        // Muda a cor se não estiver disponível
        if(spot.status != "Available") {
            holder.tvSpotStatus.setTextColor(Color.parseColor("#F44336"))
        } else {
            holder.tvSpotStatus.setTextColor(Color.parseColor("#4CAF50"))
        }

        holder.itemView.setOnClickListener {
        }
    }

    override fun getItemCount() = spots.size
}