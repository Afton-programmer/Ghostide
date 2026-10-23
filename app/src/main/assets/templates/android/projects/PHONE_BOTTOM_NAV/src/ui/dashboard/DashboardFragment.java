package ${PACKAGE}.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import ${PACKAGE}.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return FragmentDashboardBinding.inflate(inflater, container, false).getRoot();
    }
}
