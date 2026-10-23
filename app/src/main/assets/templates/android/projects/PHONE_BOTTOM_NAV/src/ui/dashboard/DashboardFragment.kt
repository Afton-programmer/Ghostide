package ${PACKAGE}.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ${PACKAGE}.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentDashboardBinding.inflate(inflater, container, false).root
    }
}
