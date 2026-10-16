package ir.hanzodev1375.ghostide.adapters;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ir.hanzodev1375.ghostide.activity.EditorActivity;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.AndroidClasspathResolver;
import ir.hanzodev1375.ghostide.ide.ui.api.SyncHost;
import ir.hanzodev1375.ghostide.ide.ui.api.SyncSession;

/** Wraps one {@link EditorActivity}'s sync bar; registered alongside its lifecycle. */
public final class SyncHostAdapter implements SyncHost {

  private static final String TAG = "SyncHostAdapter";

  private final EditorActivity activity;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final Object lock = new Object();

  private long generation;

  public SyncHostAdapter(EditorActivity activity) {
    this.activity = activity;
  }

  @Override
  public SyncSession begin(CharSequence title) {
    final long gen;
    synchronized (lock) {
      gen = ++generation;
    }
    Session session = new Session(gen, title);
    mainHandler.post(
        () -> {
          synchronized (lock) {
            if (gen != generation) return;
          }
          activity.showSyncBar(session.title);
        });
    return session;
  }

  /**
   * Drops the cached classpath and reconnects the current file's language server so it picks up the
   * libraries the sync just installed. The editor reference is taken on the UI thread; the actual
   * disconnect/connect work runs on a background thread.
   */
  private void reloadLanguageServers() {
    IdeEditor editor = activity.getEditor();
    if (editor == null) return;
    Thread worker =
        new Thread(
            () -> {
              try {
                AndroidClasspathResolver.clearClasspathCache(activity.getApplicationContext());
                editor.disconnectLsp();
                editor.ensureLspConnected();
              } catch (Exception e) {
                Log.w(TAG, "LSP reload after sync failed", e);
              }
            },
            "ghostide-sync-lsp");
    worker.start();
  }

  private boolean isCurrent(long gen) {
    synchronized (lock) {
      return gen == generation;
    }
  }

  private final class Session implements SyncSession {

    private final long gen;
    private volatile boolean active = true;
    private CharSequence title;

    Session(long gen, CharSequence title) {
      this.gen = gen;
      this.title = title;
    }

    @Override
    public void setTitle(CharSequence title) {
      this.title = title;
      if (!isCurrent(gen)) return;
      mainHandler.post(
          () -> {
            if (isCurrent(gen)) activity.setSyncBarTitle(title);
          });
    }

    @Override
    public void setMessage(CharSequence message) {
      if (!isCurrent(gen)) return;
      mainHandler.post(
          () -> {
            if (isCurrent(gen)) activity.setSyncBarMessage(message);
          });
    }

    @Override
    public void setProgress(int percent) {
      if (!isCurrent(gen)) return;
      mainHandler.post(
          () -> {
            if (isCurrent(gen)) activity.setSyncBarProgress(percent);
          });
    }

    @Override
    public void succeed() {
      finish(true);
    }

    @Override
    public void fail(CharSequence reason) {
      if (!markInactive()) return;
      final CharSequence message = reason;
      mainHandler.post(
          () -> {
            if (!isCurrent(gen)) return;
            activity.showSyncBarError(title, message);
            reloadLanguageServers();
          });
    }

    @Override
    public void cancel() {
      finish(false);
    }

    @Override
    public boolean isActive() {
      return active;
    }

    @Override
    public void prompt(
        CharSequence message, CharSequence confirmLabel, CharSequence cancelLabel, ActionListener listener) {
      if (!isCurrent(gen)) return;
      mainHandler.post(
          () -> {
            if (!isCurrent(gen)) return;
            activity.showSyncBarPrompt(
                title,
                message,
                confirmLabel,
                cancelLabel,
                () -> {
                  if (isCurrent(gen)) listener.onConfirm();
                },
                () -> {
                  if (isCurrent(gen)) listener.onCancel();
                });
          });
    }

    private boolean markInactive() {
      synchronized (this) {
        if (!active) return false;
        active = false;
        return true;
      }
    }

    private void finish(boolean reloadLsp) {
      if (!markInactive()) return;
      mainHandler.post(
          () -> {
            if (!isCurrent(gen)) return;
            activity.hideSyncBar();
            if (reloadLsp) reloadLanguageServers();
          });
    }
  }
}
