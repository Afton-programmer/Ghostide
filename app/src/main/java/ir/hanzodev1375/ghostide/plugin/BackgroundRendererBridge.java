package ir.hanzodev1375.ghostide.plugin;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import ir.hanzodev1375.components.childern.BackgroundChildRegistry;
import ir.hanzodev1375.components.childern.IChild;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginBackgroundRenderer;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.api.ExtensionRegistration;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;

/**
 * Bridges the plugin extension {@link PluginUiExtensionPoints#BACKGROUND_RENDERER} into the
 * component-level {@link BackgroundChildRegistry}, so plugin-contributed wallpaper renderers are
 * resolved by every {@code ViewChilder} without {@code :components} depending on the plugin APIs.
 *
 * <p>The resolver queries the live extension registry on every request; plugins loaded or
 * unloaded at runtime are therefore honoured immediately.
 */
public final class BackgroundRendererBridge {

  private static final String TAG = "BackgroundRendererBridge";

  private static final BackgroundChildRegistry.Resolver RESOLVER =
      (context, path, owner, blurSize) -> {
        if (path == null) return null;
        for (ExtensionRegistration<PluginBackgroundRenderer> registration :
            GlobalRegistry.extensions()
                .registrations(PluginUiExtensionPoints.BACKGROUND_RENDERER)) {
          PluginBackgroundRenderer renderer = registration.extension();
          boolean supports;
          try {
            supports = renderer.supports(path);
          } catch (RuntimeException e) {
            Log.e(TAG, "supports() failed for plugin " + registration.ownerPluginId(), e);
            continue;
          }
          if (!supports) continue;
          View view = renderer.createView(context, path, blurSize);
          if (view == null) continue;
          return new RendererChild(view, path, renderer);
        }
        return null;
      };

  private BackgroundRendererBridge() {}

  /** Installs the resolver. Safe to call more than once. */
  public static void install() {
    BackgroundChildRegistry.setResolver(RESOLVER);
  }

  private static final class RendererChild implements IChild {

    private final View view;
    private final String path;
    private final PluginBackgroundRenderer renderer;

    RendererChild(View view, String path, PluginBackgroundRenderer renderer) {
      this.view = view;
      this.path = path;
      this.renderer = renderer;
    }

    @Override
    public View view() {
      return view;
    }

    @Override
    public String pathTheme() {
      return path;
    }

    @Override
    public void release() {
      try {
        renderer.release(view);
      } catch (RuntimeException e) {
        Log.e(TAG, "release() failed for background: " + path, e);
      }
    }
  }
}
