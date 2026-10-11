package ir.hanzodev1375.ghostide.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.recyclerview.widget.RecyclerView;
import ir.theme.M3Theme;
import ir.hanzodev1375.ghostide.interfaces.OnItemClickListener;
import ir.hanzodev1375.ghostide.models.ToolbarModel;
import java.util.List;

public class ToolbarListAdapter extends RecyclerView.Adapter<ToolbarListAdapter.VH> {

  private List<ToolbarModel> listModel;
  private OnItemClickListener<ToolbarModel> clickListener;
  private Context context;

  public ToolbarListAdapter(
      List<ToolbarModel> listModel,
      OnItemClickListener<ToolbarModel> clickListener,
      Context context) {
    this.listModel = listModel;
    this.clickListener = clickListener;
    this.context = context;
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    Context ctx = parent.getContext();
    float density = ctx.getResources().getDisplayMetrics().density;

    int sizePx = (int) (40 * density + 0.5f);
    int marginPx = (int) (2 * density + 0.5f);
    LinearLayout container = new LinearLayout(ctx);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setGravity(Gravity.CENTER);

    RecyclerView.LayoutParams lp =
        new RecyclerView.LayoutParams(sizePx, ViewGroup.LayoutParams.MATCH_PARENT);
    lp.setMargins(marginPx, 0, marginPx, 0);
    container.setLayoutParams(lp);

    ImageButton imageButton = new ImageButton(ctx);
    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(sizePx, sizePx);
    imageButton.setLayoutParams(btnLp);
    imageButton.setBackground(null);
    imageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    imageButton.setAdjustViewBounds(false);
    int paddingPx = (int) (8 * density + 0.5f);
    imageButton.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

    container.addView(imageButton);

    return new VH(container, imageButton);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    ToolbarModel model = listModel.get(position);
    holder.bind(model, clickListener);
  }

  @Override
  public int getItemCount() {
    return listModel == null ? 0 : listModel.size();
  }

  class VH extends RecyclerView.ViewHolder {
    private final ImageButton icon;

    public VH(@NonNull View itemView, ImageButton icon) {
      super(itemView);
      this.icon = icon;
    }

    public void bind(ToolbarModel model, OnItemClickListener<ToolbarModel> clickListener) {
      if (model.getDrawable() != null) {
        icon.setImageDrawable(model.getDrawable());
      } else {
        icon.setImageResource(model.getIcon());
      }

      M3Theme.imageB(icon);

      icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      float density = icon.getContext().getResources().getDisplayMetrics().density;
      int paddingPx = (int) (8 * density + 0.5f);
      icon.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

      // 🔥 تگ روی آیکون (برای کلیک‌ها)
      icon.setTag(model.getTag());
      // 🔥 تگ روی کانتینر (برای اینکه findToolbarItem توی FileManagerActivity پیداش کنه و بج رو بچسبونه)
      itemView.setTag(model.getTag());

      icon.setOnClickListener(
          v -> {
            if (clickListener != null) {
              clickListener.onClick(v, model, getBindingAdapterPosition());
            }
          });

      if (model.isShowVisblityItem()) {
        icon.setEnabled(true);
        icon.setAlpha(1f);
      } else {
        icon.setEnabled(false);
        icon.setAlpha(0.5f);
      }
      icon.setVisibility(model.isVisible() ? View.VISIBLE : View.GONE);
      icon.setOnLongClickListener(
          v -> {
            TooltipCompat.setTooltipText(v, model.getTag());
            return false;
          });
    }
  }
}