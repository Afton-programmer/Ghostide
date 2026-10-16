package ir.hanzodev1375.components.childern;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

/**
 * Host-side factory for custom background renderers. Consulted by {@link ViewChilder} before the
 * built-in image/gif/video detection, so a registered factory can claim any path or URI scheme
 * (e.g. {@code pet://...}, {@code glsl://...}, a custom video container, a live canvas scene).
 */
public interface BackgroundChildFactory {

  /** Returns true when this factory is responsible for the given path/URI. */
  boolean supports(String path);

  /**
   * Creates the renderer for a supported path.
   *
   * @param context  hosting context
   * @param path     path or URI passed to {@link ViewChilder#load}
   * @param owner    lifecycle owner of the host screen, may be null
   * @param blurSize blur hint in the same units the built-in children use
   */
  @Nullable
  IChild create(Context context, String path, @Nullable LifecycleOwner owner, float blurSize);
}
