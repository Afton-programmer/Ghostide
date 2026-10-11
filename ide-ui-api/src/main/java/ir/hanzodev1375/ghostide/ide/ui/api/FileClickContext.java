package ir.hanzodev1375.ghostide.ide.ui.api;

import android.content.Context;
import android.view.View;
import java.io.File;

/**
 * Everything a {@link FileClickHandler} needs about a tap it claimed in the file manager: the
 * tapped path, the host {@link Context} to show UI from, and the row {@link View} to anchor or
 * transition from.
 */
public final class FileClickContext {

  private final Context context;
  private final String path;
  private final String name;
  private final boolean directory;
  private final View anchor;

  public FileClickContext(
      Context context, String path, String name, boolean directory, View anchor) {
    this.context = context;
    this.path = path;
    this.name = name;
    this.directory = directory;
    this.anchor = anchor;
  }

  /** Host Activity context; use it to show dialogs or start activities. */
  public Context context() {
    return context;
  }

  /** Absolute path of the tapped file or directory. */
  public String path() {
    return path;
  }

  /** Display name of the tapped entry. */
  public String name() {
    return name;
  }

  /** The tapped path as a {@link File}. */
  public File file() {
    return new File(path);
  }

  /** {@code true} when the tapped entry is a directory. */
  public boolean isDirectory() {
    return directory;
  }

  /**
   * The tapped row view, usable as a shared-element source or popup anchor; may be {@code null}
   * when the host has no view for the tap.
   */
  public View anchor() {
    return anchor;
  }
}
