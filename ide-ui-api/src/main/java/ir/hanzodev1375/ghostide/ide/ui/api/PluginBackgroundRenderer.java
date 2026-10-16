package ir.hanzodev1375.ghostide.ide.ui.api;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * Plugin-facing API for contributing a custom screen background (wallpaper) renderer.
 *
 * <p>A plugin registers an implementation at {@link PluginUiExtensionPoints#BACKGROUND_RENDERER}
 * and claims any path/URI scheme it wants ({@code pet://cat}, {@code glsl://plasma.frag},
 * {@code myplayer://clip.mp4}, ...). Every {@code ViewChilder} in the IDE (editor, file manager,
 * terminal, settings, ...) resolves registered renderers before its built-in image/gif/video
 * detection, so the contribution becomes the active background wherever that path is loaded
 * through the theme's {@code widget.imagepath} or a direct {@code ViewChilder.load} call.
 *
 * <p>The returned view is added full-size as the bottom layer of the host screen; the editor and
 * all other content are rendered transparently on top of it. The view should keep itself updated
 * (animations, players) and must honour {@link #release} instead of relying on a lifecycle owner.
 */
public interface PluginBackgroundRenderer {

  /** Returns true when this renderer owns the given path/URI. */
  boolean supports(String path);

  /**
   * Builds the background view for a supported path.
   *
   * @param context  host activity context (itself a LifecycleOwner when lifecycle awareness is
   *                 needed)
   * @param path     path or URI passed to ViewChilder.load
   * @param blurSize blur hint requested by the theme, may be 0
   * @return the full-size background view, or null to decline and fall back to built-in handling
   */
  @Nullable
  View createView(Context context, String path, float blurSize);

  /**
   * Called when the background is replaced or the host screen is destroyed. Must stop players,
   * renderers and free all resources held by the view returned from {@link #createView}.
   */
  default void release(View backgroundView) {}
}
