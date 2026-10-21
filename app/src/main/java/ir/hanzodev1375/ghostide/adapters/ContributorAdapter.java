package ir.hanzodev1375.ghostide.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.models.Contributor;
import java.util.ArrayList;
import java.util.List;

/** Shows GitHub contributors as circular avatars with their usernames. */
public class ContributorAdapter extends RecyclerView.Adapter<ContributorAdapter.ViewHolder> {

  private final List<Contributor> items = new ArrayList<>();

  public void setItems(List<Contributor> contributors) {
    items.clear();
    if (contributors != null) {
      items.addAll(contributors);
    }
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_contributor, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Contributor contributor = items.get(position);
    holder.name.setText(contributor.getUsername());
    holder.avatar.setContentDescription(contributor.getUsername());
    Glide.with(holder.avatar)
        .load(contributor.getAvatarUrl())
        .circleCrop()
        .placeholder(R.drawable.outline_person)
        .error(R.drawable.outline_person)
        .into(holder.avatar);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    private final ImageView avatar;
    private final TextView name;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      avatar = itemView.findViewById(R.id.contributor_avatar);
      name = itemView.findViewById(R.id.contributor_name);
    }
  }
}
