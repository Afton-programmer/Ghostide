package ir.hanzodev1375.components.glsl;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import ir.hanzodev1375.components.childern.BackgroundChildFactory;
import ir.hanzodev1375.components.childern.BackgroundChildRegistry;
import ir.hanzodev1375.components.childern.IChild;

/**
 * Host factory that claims {@code glsl://<effect>} background paths and renders them with the
 * built-in {@link GlslEffects} shaders.
 *
 * <p>Registering it is enough to make every {@code ViewChilder} (Editor, FileManager, Terminal,
 * Settings, About, ...) able to show a live GPU background: a theme only has to store a
 * {@code glsl://liquid} (or silk/galaxy) path as its background media.
 */
public final class GlslBackgroundFactory implements BackgroundChildFactory {

  /** URI scheme claimed by this factory. */
  public static final String SCHEME = "glsl://";

  private static GlslBackgroundFactory instance;

  /** Registers the singleton factory. Safe to call more than once. */
  public static void register() {
    if (instance == null) {
      instance = new GlslBackgroundFactory();
    }
    BackgroundChildRegistry.register(instance);
  }

  private GlslBackgroundFactory() {}

  @Override
  public boolean supports(String path) {
    return path != null && path.startsWith(SCHEME);
  }

  @Nullable
  @Override
  public IChild create(
      Context context, String path, @Nullable LifecycleOwner owner, float blurSize) {
    if (context == null) {
      return null;
    }
    return new GlslBackgroundChild(context, path);
  }
}
