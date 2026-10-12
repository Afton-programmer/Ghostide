package ir.hanzodev1375.ghostide.materialfileicon.core;

import android.content.Context;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.util.LruCache;
import android.widget.ImageView;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileIconHelper {

  private static final float DEFAULT_ICON_SIZE = 24f;
  private static final int ICON_CACHE_SIZE = 64;
  private static final LruCache<String, Picture> ICON_CACHE = new LruCache<>(ICON_CACHE_SIZE);

  private String filePath;
  private String mimeType;

  private boolean isDynamicFolderEnabled;
  private boolean isEnvironmentEnabled;

  private JsonFileIconHelper jsonHelper;

  public FileIconHelper(String filePath) {
    this(filePath, "");
  }

  public FileIconHelper(String filePath, String mimeType) {
    this.filePath = filePath == null ? "" : filePath;
    this.mimeType = mimeType == null ? "" : mimeType;
    rebuild();
  }

  private void rebuild() {
    jsonHelper = new JsonFileIconHelper(filePath);
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath == null ? "" : filePath;
    rebuild();
  }

  public String getFilePath() {
    return filePath;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getFileIcon() {
    if (isDirectory() && !isDynamicFolderEnabled)
      return jsonHelper.getIconUri(false);
    return jsonHelper.getIconUri();
  }

  public String getFileIconName() {
    return jsonHelper.getIconName(isDirectory() && isDynamicFolderEnabled);
  }

  public void setDynamicFolderEnabled(boolean isDynamicFolderEnabled) {
    this.isDynamicFolderEnabled = isDynamicFolderEnabled;
  }

  public boolean isDynamicFolderEnabled() {
    return isDynamicFolderEnabled;
  }

  public void setEnvironmentEnabled(boolean isEnvironmentEnabled) {
    this.isEnvironmentEnabled = isEnvironmentEnabled;
  }

  public boolean isEnvironmentEnabled() {
    return isEnvironmentEnabled;
  }

  public void bindIcon(ImageView imageView) {
    JsonFileIconHelper.load(imageView.getContext());
    jsonHelper.bindIcon(imageView);
  }

  public Drawable getIconDrawable(Context context) {
    if (context == null) return null;
    JsonFileIconHelper.load(context);
    String assetPath = jsonHelper.getIconPath();
    Picture picture = ICON_CACHE.get(assetPath);
    if (picture == null) {
      try (InputStream is = context.getAssets().open(assetPath)) {
        SVG svg = SVG.getFromInputStream(is);
        float width = svg.getDocumentWidth();
        float height = svg.getDocumentHeight();
        if (width <= 0f) width = DEFAULT_ICON_SIZE;
        if (height <= 0f) height = DEFAULT_ICON_SIZE;
        picture = svg.renderToPicture((int) Math.ceil(width), (int) Math.ceil(height));
        ICON_CACHE.put(assetPath, picture);
      } catch (IOException | SVGParseException e) {
        return null;
      }
    }
    return new PictureDrawable(picture);
  }

  private boolean isDirectory() {
    File f = new File(filePath);
    return f.exists() && f.isDirectory();
  }
}
