package ir.hanzodev1375.ghostide.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import ir.hanzodev1375.ghostide.glide.xml.VectorModel;
import ir.hanzodev1375.ghostide.materialfileicon.core.FileIconHelper;

import java.io.File;

public class Icon {

  private static final String[] RASTER = {
    ".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp", ".avif", ".ico", ".heic", ".heif",
    ".tiff", ".tif", ".mp4", ".webm", ".mkv", ".mov", ".3gp"
  };

  public void bind(String path, ImageView icon) {
    if (path == null) return;
    Context context = icon.getContext();
    File file = new File(path);
    String lower = path.toLowerCase();

    if (lower.endsWith(".svg")) {
      icon.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
      Glide.with(context)
          .as(PictureDrawable.class)
          .load(file)
          .error(defaultIcon(context, path))
          .into(icon);
      return;
    }

    if (lower.endsWith(".xml")) {
      Glide.with(context)
          .as(Drawable.class)
          .load(new VectorModel(file, context))
          .error(defaultIcon(context, path))
          .into(icon);
      return;
    }

    if (lower.endsWith(".apk")) {
      Glide.with(context)
          .as(Drawable.class)
          .load(path)
          .error(defaultIcon(context, path))
          .into(icon);
      return;
    }

    if (lower.endsWith(".mp3")) {
      Glide.with(context)
          .asBitmap()
          .load(path)
          .error(defaultIcon(context, path))
          .into(icon);
      return;
    }

    for (String ext : RASTER) {
      if (lower.endsWith(ext)) {
        Glide.with(context).load(file).error(defaultIcon(context, path)).into(icon);
        return;
      }
    }
  }

  private Drawable defaultIcon(Context context, String path) {
    return new FileIconHelper(path).getIconDrawable(context);
  }
}
