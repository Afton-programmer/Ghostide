package ir.hanzodev1375.ghostide.ide.ui.api;

import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * A toolbar action contributed by a plugin and shown persistently next to the IDE's own toolbar
 * icons, instead of living only inside the plugin popup.
 *
 * <p>Register an implementation at {@link PluginUiExtensionPoints#TOOLBAR_ICON}. The host appends it
 * to the toolbar of the screen matching {@link #getState()}: the editor for {@link
 * PluginIconState#Editor} or the file manager for {@link PluginIconState#FileES}.
 *
 * <p>The icon is a fully-built {@link Drawable}; create it from the plugin's own scoped context so
 * both resource ({@code R.drawable.x}) and asset based icons work:
 *
 * <pre>{@code
 * Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
 * Drawable icon = pluginContext.getDrawable(R.drawable.ic_my_action);
 * }</pre>
 *
 * <p>{@link #onClick(View)} is called on the UI thread with the toolbar item view as anchor.
 */
public interface PluginToolbarIcon {

  /** Stable, unique id used by the host to route clicks back to this icon. */
  String getId();

  /** Toolbar tooltip shown on long press. */
  String getTitle();

  PluginIconState getState();

  Drawable getIcon();

  void onClick(View anchor);
}