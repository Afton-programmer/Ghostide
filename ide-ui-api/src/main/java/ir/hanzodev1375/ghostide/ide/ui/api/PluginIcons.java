package ir.hanzodev1375.ghostide.ide.ui.api;

import android.graphics.drawable.Drawable;
import android.view.View;

import ir.hanzodev1375.ghostide.plugin.api.Disposable;
import ir.hanzodev1375.ghostide.plugin.api.PluginContext;

/**
 * Convenience one-liner for turning a plugin action into a persistent toolbar icon.
 *
 * <p>This is the "keep the plugin at hand" helper: instead of the action living only in the plugin
 * popup, it shows up live beside the IDE's own toolbar icons on the requested screen.
 *
 * <pre>{@code
 * Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
 * PluginIcons.setPluginShowState(
 *     context,
 *     PluginIconState.Editor,
 *     "Format",
 *     v -> formatCurrentFile(),
 *     pluginContext.getDrawable(R.drawable.ic_format));
 * }</pre>
 *
 * <p>The returned registration is already tracked by {@link PluginContext#registerDisposable}, so
 * unload cleanup happens automatically; keep the {@code Disposable} only if you need to remove the
 * icon earlier.
 */
public final class PluginIcons {

  private PluginIcons() {}

  /** Registers one toolbar icon with an auto-generated unique id. */
  public static Disposable setPluginShowState(
      PluginContext context,
      PluginIconState state,
      String title,
      View.OnClickListener onClick,
      Drawable icon) {
    return setPluginShowState(context, state, null, title, onClick, icon);
  }

  /**
   * Registers a toolbar icon on the screen matching {@code state}. {@code itemId} distinguishes
   * several icons contributed by the same plugin and must be unique per icon; pass {@code null} to
   * get an auto-generated id.
   */
  public static Disposable setPluginShowState(
      PluginContext context,
      PluginIconState state,
      String itemId,
      String title,
      View.OnClickListener onClick,
      Drawable icon) {
    String pluginId = context.getDescriptor().getId();
    String resolvedId =
        itemId == null || itemId.isBlank() ? pluginId + "#" + state.name() : pluginId + "/" + itemId;
    PluginToolbarIcon toolbarIcon =
        new PluginToolbarIcon() {
          @Override
          public String getId() {
            return resolvedId;
          }

          @Override
          public String getTitle() {
            return title;
          }

          @Override
          public PluginIconState getState() {
            return state;
          }

          @Override
          public Drawable getIcon() {
            return icon;
          }

          @Override
          public void onClick(View anchor) {
            if (onClick != null) {
              onClick.onClick(anchor);
            }
          }
        };
    Disposable registration =
        context.getExtensions().register(PluginUiExtensionPoints.TOOLBAR_ICON, toolbarIcon);
    context.registerDisposable(registration);
    return registration;
  }

  /**
   * Variant that takes a plugin resource id ({@code R.drawable.x}). The drawable is resolved with
   * the plugin's own scoped context, so it works for both resource and asset backed icons.
   */
  public static Disposable setPluginShowState(
      PluginContext context,
      PluginIconState state,
      String itemId,
      String title,
      View.OnClickListener onClick,
      int iconRes) {
    android.content.Context pluginContext =
        context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
    return setPluginShowState(
        context, state, itemId, title, onClick, pluginContext.getDrawable(iconRes));
  }
}