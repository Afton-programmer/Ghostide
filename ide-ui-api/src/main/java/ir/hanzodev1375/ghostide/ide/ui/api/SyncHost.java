package ir.hanzodev1375.ghostide.ide.ui.api;

/**
 * Published as a service by the host under {@link IdeHostServices#SYNC_HOST}. A plugin that needs to
 * install/refresh a project's dependencies calls {@link #begin(CharSequence)} to drive the host's
 * Android-Studio-style sync bar instead of letting the work run in the visible terminal.
 *
 * <pre>{@code
 * SyncHost sync = context.getServices().require(IdeHostServices.SYNC_HOST);
 * SyncSession session = sync.begin("Gradle sync");
 * session.setProgress(-1);                       // indeterminate, install starting
 * // ... run your package manager, updating progress as it goes ...
 * session.setProgress(100);
 * session.succeed();                             // bar disappears, language servers reload
 * }</pre>
 *
 * <p>{@link #begin(CharSequence)} must be called on the UI thread. Only one session can be active on
 * an editor at a time; starting a new one while another is running cancels the previous session.
 */
public interface SyncHost {

  /**
   * Starts a sync session and shows the sync bar under the editor tabs.
   *
   * @param title headline shown while the sync runs (e.g. "Gradle sync")
   * @return a handle to report progress and finish the sync
   */
  SyncSession begin(CharSequence title);
}
