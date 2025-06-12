import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.valletprojeto.R
import br.edu.fatecpg.valletprojeto.adapter.SimpleParkingAdapter
import br.edu.fatecpg.valletprojeto.databinding.ActivityDashboardBaseBinding
import br.edu.fatecpg.valletprojeto.databinding.FragmentSpotsBinding

class SpotsFragment : Fragment() {

    private var _binding: FragmentSpotsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpotsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvOtherParkings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SimpleParkingAdapter()
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    LinearLayoutManager.VERTICAL
                )
            )
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}