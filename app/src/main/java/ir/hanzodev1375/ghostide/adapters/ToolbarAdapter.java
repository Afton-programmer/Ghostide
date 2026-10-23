package ir.hanzodev1375.ghostide.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.components.views.GhostToast;
import ir.theme.M3Theme;
import java.util.List;

public class ToolbarAdapter extends RecyclerView.Adapter<ToolbarAdapter.Holder> {

  private List<Integer> icons;
  private List<Integer> tooltips;
  private OnClick click;

  public interface OnClick {
    void click(View v, int pos);
  }

  public ToolbarAdapter(List<Integer> icons, OnClick click) {
    this(icons, null, click);
  }

  public ToolbarAdapter(List<Integer> icons, List<Integer> tooltips, OnClick click) {
    this.icons = icons;
    this.tooltips = tooltips;
    this.click = click;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ImageView image = new ImageView(parent.getContext());
    RecyclerView.LayoutParams lp =
        new RecyclerView.LayoutParams(
            dp(parent.getContext(), 64), ViewGroup.LayoutParams.MATCH_PARENT);
    image.setLayoutParams(lp);

    image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    image.setColorFilter(
        fallback(M3Theme.onSurface(), 0),
        PorterDuff.Mode.SRC_IN);

    return new Holder(image);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    holder.image.setImageResource(icons.get(position));
    holder.image.setOnClickListener(
        c -> {
          click.click(c, position);
        });
    holder.image.setOnLongClickListener(
        v -> {
          if (tooltips != null && position < tooltips.size() && tooltips.get(position) != null) {
            GhostToast.makeText(holder.image.getContext(), holder.image.getContext().getString(tooltips.get(position)), GhostToast.LENGTH_SHORT).show();
          }
          return true;
        });
  }

  @Override
  public int getItemCount() {
    return icons == null ? 0 : icons.size();
  }

  public static class Holder extends RecyclerView.ViewHolder {

    ImageView image;

    public Holder(@NonNull View itemView) {
      super(itemView);
      image = (ImageView) itemView;
    }
  }

  private static int dp(Context c, int value) {
    return (int) (value * c.getResources().getDisplayMetrics().density);
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
