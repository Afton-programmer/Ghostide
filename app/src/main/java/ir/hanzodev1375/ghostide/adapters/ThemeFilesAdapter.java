package ir.hanzodev1375.ghostide.adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import ir.hanzodev1375.components.utils.RoundedCornersTransformation;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.themeengine.GthCanvasDrawable;
import ir.theme.GthDecoder;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThemeFilesAdapter extends RecyclerView.Adapter<ThemeFilesAdapter.ViewHolder> {

  public interface OnFileClickListener {
    void onFileClick(File file);
  }

  public interface OnFileLongClickListener {
    void onFileLongClick(File file, ViewHolder holder);
  }

  private static final ExecutorService DECODE_EXECUTOR =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "gth-decoder");
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
          });
  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  private final List<File> files;
  private final Map<String, GthDecoder.Result> dataCache = new ConcurrentHashMap<>();
  private final OnFileClickListener listener;
  private OnFileLongClickListener longClickListener;
  private String selectedPath = "";

  public ThemeFilesAdapter(List<File> files, OnFileClickListener listener) {
    this.files = files;
    this.listener = listener;
  }

  public void setOnFileLongClickListener(OnFileLongClickListener listener) {
    this.longClickListener = listener;
  }

  public void setSelectedPath(String selectedPath) {
    this.selectedPath = selectedPath != null ? selectedPath : "";
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme_grid, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    File file = files.get(position);
    String path = file.getAbsolutePath();
    holder.boundPath = path;

    String displayName = file.getName();
    if (displayName.toLowerCase().endsWith(".gth")) {
      displayName = displayName.substring(0, displayName.length() - 4);
    }
    holder.name.setText(displayName);

    boolean selected = path.equals(selectedPath);
    holder.checked.setVisibility(selected ? View.VISIBLE : View.GONE);

    holder.icon.setBackground(new GthCanvasDrawable(null, null, null));
    holder.icon.setColorFilter(null);
    Glide.with(holder.icon.getContext()).clear(holder.icon);
    holder.icon.setImageDrawable(null);

    GthDecoder.Result cached = dataCache.get(path);
    if (cached != null) {
      applyThemeData(holder, file, cached);
    } else {
      decodeAsync(holder, file);
    }

    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onFileClick(file);
        });
    holder.itemView.setOnLongClickListener(
        v -> {
          if (longClickListener != null) {
            longClickListener.onFileLongClick(file, holder);
            return true;
          }
          return false;
        });
  }

  @Override
  public int getItemCount() {
    return files.size();
  }

  public void refresh(List<File> newFiles) {
    files.clear();
    files.addAll(newFiles);
    dataCache.clear();
    notifyDataSetChanged();
  }

  private void decodeAsync(ViewHolder holder, File file) {
    String path = file.getAbsolutePath();
    DECODE_EXECUTOR.execute(
        () -> {
          GthDecoder.Result data = GthDecoder.decode(file);
          dataCache.put(path, data);
          MAIN_HANDLER.post(
              () -> {
                if (path.equals(holder.boundPath)) {
                  applyThemeData(holder, file, data);
                }
              });
        });
  }

  private void applyThemeData(ViewHolder holder, File file, GthDecoder.Result data) {
    if (!file.getAbsolutePath().equals(holder.boundPath)) return;

    holder.icon.setBackground(
        new GthCanvasDrawable(data.getPrimary(), data.getSecondary(), data.getTertiary()));
    holder.icon.setColorFilter(null);

    if (data.hasImage()) {
      Glide.with(holder.icon.getContext())
          .load(data.getImagePath())
          .transform(new RoundedCornersTransformation(26))
          .override(112, 122)
          .into(holder.icon);
    } else {
      Glide.with(holder.icon.getContext()).clear(holder.icon);
      holder.icon.setImageDrawable(null);
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    final ImageView icon;
    final ImageView checked;
    final TextView name;
    String boundPath = "";

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      icon = itemView.findViewById(R.id.theme_icon);
      checked = itemView.findViewById(R.id.theme_checked);
      name = itemView.findViewById(R.id.theme_name);
    }
  }
}
