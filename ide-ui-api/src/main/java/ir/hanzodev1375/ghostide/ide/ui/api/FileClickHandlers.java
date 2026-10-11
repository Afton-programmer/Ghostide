package ir.hanzodev1375.ghostide.ide.ui.api;

import android.content.Context;
import android.view.View;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;

/**
 * Dispatches a file-manager tap to the registered {@link FileClickHandler}s.
 *
 * <p>Handlers are consulted in descending priority order (the same order as any extension point);
 * the first one whose {@link FileClickHandler#supports supports} returns {@code true} gets the tap
 * and the host's built-in handling is skipped.
 */
public final class FileClickHandlers {

  private FileClickHandlers() {}

  /**
   * Offers a tap to the registered handlers.
   *
   * @return {@code true} when a handler claimed and handled the tap, meaning the caller must skip
   *     its built-in file opening; {@code false} to continue normally
   */
  public static boolean dispatch(
      Context context, String path, String name, boolean isDirectory, View anchor) {
    if (path == null || path.isEmpty()) {
      return false;
    }
    for (FileClickHandler handler :
        GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.FILE_CLICK_HANDLER)) {
      boolean claimed;
      try {
        claimed = handler.supports(path, name, isDirectory);
      } catch (Throwable ignored) {
        continue;
      }
      if (!claimed) {
        continue;
      }
      try {
        handler.onFileClick(new FileClickContext(context, path, name, isDirectory, anchor));
        return true;
      } catch (Throwable ignored) {
        // A broken handler must never swallow the tap: let the next one, or the built-in
        // flow, take over.
      }
    }
    return false;
  }
}
