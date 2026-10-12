package ir.hanzodev1375.ghostide.jgit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textview.MaterialTextView;
import ir.hanzodev1375.components.utils.GlassColors;
import ir.theme.M3Theme;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.SubmoduleInfo;

public class SubmoduleAdapter extends RecyclerView.Adapter<SubmoduleAdapter.ViewHolder> {
  private List<SubmoduleInfo> submodules = new ArrayList<>();

  public void submitList(List<SubmoduleInfo> list) {
    submodules = list != null ? list : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_submodule, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    SubmoduleInfo submodule = submodules.get(position);
    GlassColors.setBackgroundAlpha(holder.itemView, fallback(M3Theme.surface(), 0), 150);
    holder.tvPath.setText(submodule.getPath());
    holder.tvUrl.setText(
        submodule.getUrl() != null && !submodule.getUrl().isEmpty()
            ? submodule.getUrl()
            : holder.itemView.getContext().getString(R.string.submodule_no_url));
    holder.tvStatus.setText(statusLabel(holder.itemView, submodule));
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return submodules.size();
  }

  private static String statusLabel(View view, SubmoduleInfo submodule) {
    switch (submodule.getState()) {
      case INITIALIZED:
        return view.getContext().getString(R.string.submodule_state_initialized);
      case REV_CHECKED_OUT:
        return view.getContext().getString(R.string.submodule_state_rev_checked_out);
      case MISSING:
        return view.getContext().getString(R.string.submodule_state_missing);
      default:
        return view.getContext().getString(R.string.submodule_state_uninitialized);
    }
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    MaterialTextView tvPath, tvUrl, tvStatus;

    ViewHolder(View v) {
      super(v);
      tvPath = v.findViewById(R.id.tvSubmodulePath);
      tvUrl = v.findViewById(R.id.tvSubmoduleUrl);
      tvStatus = v.findViewById(R.id.tvSubmoduleStatus);
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
