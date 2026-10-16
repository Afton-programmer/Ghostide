package ir.hanzodev1375.ghostide.ide.ui.api;

/**
 * A running "project sync" shown by the host inside the editor, mirroring Android Studio's Gradle
 * sync bar. The host shows the {@code layout_sync} bar under the editor tabs so dependency installs
 * (Gradle, npm, pip, ...) are surfaced as a sync instead of being buried in a terminal the user has
 * to open; use this API instead of {@code CodeRunnerHost.runShell} for that work.
 *
 * <p>Obtained from {@link SyncHost#begin(CharSequence)}. Reporting is safe from any thread. After
 * {@link #succeed()}, {@link #fail(CharSequence)} or {@link #cancel()} the session is finished and
 * the host hiding the bar also reloads the language servers so newly installed libraries become
 * visible to completion/navigation.
 */
public interface SyncSession {

  /** Replaces the headline shown on the sync bar. */
  void setTitle(CharSequence title);

  /** Sets the small status line / current step (e.g. "Downloading junit-4.13.2.jar"). */
  void setMessage(CharSequence message);

  /**
   * Sets determinate progress, {@code 0..100}. Pass a negative value to switch the bar back to an
   * indeterminate spinner when the total amount of work is unknown.
   */
  void setProgress(int percent);

  /** Marks the sync as successful: hides the bar and reloads the language servers. */
  void succeed();

  /** Marks the sync as failed with {@code reason}: hides the bar and reloads the language servers. */
  void fail(CharSequence reason);

  /** Aborts the session (no LSP reload); the bar is hidden. */
  void cancel();

  /** Whether this session has not finished/cancelled yet. */
  boolean isActive();

  /**
   * Turns the sync bar into a confirmation prompt: {@code message} is shown with the two buttons and
   * the listener is notified on the UI thread. Nothing is considered "running" until the plugin
   * reacts to {@link ActionListener#onConfirm()} — typically by calling {@link #setProgress(int)} to
   * switch the bar back to the running state.
   */
  void prompt(
      CharSequence message, CharSequence confirmLabel, CharSequence cancelLabel, ActionListener listener);

  /** Receives the user's answer to {@link #prompt}. */
  interface ActionListener {
    void onConfirm();

    void onCancel();
  }
}
