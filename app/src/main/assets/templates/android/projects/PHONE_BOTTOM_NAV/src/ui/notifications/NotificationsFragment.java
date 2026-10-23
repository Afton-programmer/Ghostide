package ${PACKAGE}.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import ${PACKAGE}.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return FragmentNotificationsBinding.inflate(inflater, container, false).getRoot();
    }
}
