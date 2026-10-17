package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static hook that lets host code contribute custom {@link BackgroundChildFactory} renderers to
 * every {@link ViewChilder} without {@code :components} depending on higher layers.
 *
 * <p>Two contribution paths exist:
 *
 * <ul>
 *   <li>{@link #register} for in-process factories (first registered wins, removed with {@link
 *       #unregister});
 *   <li>{@link #setResolver} for the app layer, which turns live plugin extension lookups into
 *       renderers (see the app's BackgroundRendererBridge).
 * </ul>
 *
 * Factories are asked first, in registration order; the resolver runs after them.
 */
public final class BackgroundChildRegistry {

  private static final String TAG = "BackgroundChildRegistry";

  /** Returns a renderer for custom paths, or null to fall back to the built-in types. */
  public interface Resolver {

    @Nullable
    IChild resolve(Context context, String path, @Nullable LifecycleOwner owner, float blurSize);
  }

  /** Notified when the active custom-background configuration changes. */
  public interface GenerationListener {

    void onGenerationChanged();
  }

  private static final CopyOnWriteArrayList<BackgroundChildFactory> FACTORIES =
      new CopyOnWriteArrayList<>();

  private static final CopyOnWriteArrayList<GenerationListener> LISTENERS =
      new CopyOnWriteArrayList<>();

  @Nullable private static volatile Resolver resolver;

  private static final AtomicInteger GENERATION = new AtomicInteger();

  private BackgroundChildRegistry() {}

  /**
   * Monotonic version of the custom-background configuration. A {@link ViewChilder} records it when
   * it loads content and reloads when it changes, so switching the active background plugin (or
   * installing/removing one) takes effect without recreating the view.
   */
  public static int generation() {
    return GENERATION.get();
  }

  /** Signals that custom background renderers changed and every view should re-resolve. */
  public static void bumpGeneration() {
    GENERATION.incrementAndGet();
    for (GenerationListener listener : LISTENERS) {
      try {
        listener.onGenerationChanged();
      } catch (RuntimeException e) {
        Log.e(TAG, "Background generation listener failed", e);
      }
    }
  }

  /** Registers a listener notified whenever {@link #bumpGeneration()} runs. */
  public static void addGenerationListener(GenerationListener listener) {
    if (listener != null && !LISTENERS.contains(listener)) {
      LISTENERS.add(listener);
    }
  }

  public static void removeGenerationListener(GenerationListener listener) {
    LISTENERS.remove(listener);
  }

  /** Registers a host factory. Re-registering the same instance is a no-op. */
  public static void register(BackgroundChildFactory factory) {
    if (factory != null && !FACTORIES.contains(factory)) {
      FACTORIES.add(factory);
    }
  }

  public static void unregister(BackgroundChildFactory factory) {
    FACTORIES.remove(factory);
  }

  /** Installs (or clears, with null) the app-level resolver consulted after the factories. */
  public static void setResolver(@Nullable Resolver newResolver) {
    resolver = newResolver;
  }

  @Nullable
  static IChild resolve(
      Context context, String path, @Nullable LifecycleOwner owner, float blurSize) {
    for (BackgroundChildFactory factory : FACTORIES) {
      IChild child = tryCreate(factory, context, path, owner, blurSize);
      if (child != null) return child;
    }
    Resolver r = resolver;
    if (r == null) return null;
    try {
      return r.resolve(context, path, owner, blurSize);
    } catch (RuntimeException e) {
      Log.e(TAG, "Custom background resolver failed for: " + path, e);
      return null;
    }
  }

  @Nullable
  private static IChild tryCreate(
      BackgroundChildFactory factory,
      Context context,
      String path,
      @Nullable LifecycleOwner owner,
      float blurSize) {
    boolean supports;
    try {
      supports = factory.supports(path);
    } catch (RuntimeException e) {
      Log.e(TAG, "Factory supports() failed for: " + path, e);
      return null;
    }
    if (!supports) return null;
    try {
      return factory.create(context, path, owner, blurSize);
    } catch (RuntimeException e) {
      Log.e(TAG, "Factory create() failed for: " + path, e);
      return null;
    }
  }
}
