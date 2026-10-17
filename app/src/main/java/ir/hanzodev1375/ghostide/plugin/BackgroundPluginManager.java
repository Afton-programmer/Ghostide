package ir.hanzodev1375.ghostide.plugin;

import android.content.Context;
import ir.hanzodev1375.components.childern.BackgroundChildRegistry;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginBackgroundRenderer;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.api.ExtensionRegistration;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.hanzodev1375.ghostide.plugin.api.PluginDescriptor;
import ir.hanzodev1375.ghostide.plugin.api.PluginIds;
import ir.hanzodev1375.ghostide.plugin.gpl.GplPluginLoader;
import ir.hanzodev1375.ghostide.plugin.gpl.LoadedGplPlugin;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers the background renderer plugins currently installed.
 *
 * <p>A "background plugin" is simply a loaded plugin that registered at {@link
 * PluginUiExtensionPoints#BACKGROUND_RENDERER}. Each owner plugin appears once, even when it
 * registers several renderers. The selected plugin id is stored in {@link PreferencesUtils}; an
 * empty id means "use the theme's own background image". Mirrors {@link
 * ir.hanzodev1375.ghostide.iconpack.IconPackManager}: only the selected plugin's renderer is
 * consulted by {@link BackgroundRendererBridge}, so installing further plugins never changes the
 * active background.
 */
public final class BackgroundPluginManager {

  private BackgroundPluginManager() {}

  /** Display identity of one installed background plugin. */
  public record BackgroundPluginEntry(String id, String name, String version) {}

  /**
   * Installed background plugins, in registration (priority) order. Only plugins are listed;
   * host-owned registrations are ignored.
   */
  public static List<BackgroundPluginEntry> listInstalled(Context context) {
    List<ExtensionRegistration<PluginBackgroundRenderer>> registrations =
        GlobalRegistry.extensions().registrations(PluginUiExtensionPoints.BACKGROUND_RENDERER);
    Map<String, BackgroundPluginEntry> plugins = new LinkedHashMap<>();
    for (ExtensionRegistration<PluginBackgroundRenderer> reg : registrations) {
      String owner = reg.ownerPluginId();
      if (owner == null || owner.isEmpty() || PluginIds.CORE.equals(owner)) continue;
      if (plugins.containsKey(owner)) continue;
      LoadedGplPlugin plugin = GplPluginLoader.getInstance(context).getLoaded(owner);
      PluginDescriptor descriptor = plugin == null ? null : plugin.getDescriptor();
      plugins.put(
          owner,
          new BackgroundPluginEntry(
              owner,
              descriptor == null ? owner : descriptor.getName(),
              descriptor == null ? "" : descriptor.getVersion()));
    }
    return new ArrayList<>(plugins.values());
  }

  /** The selected background plugin id, or {@code ""} for the theme's own background. */
  public static String selected(Context context) {
    return new PreferencesUtils(context.getApplicationContext()).getBackgroundPlugin();
  }

  /** Persists the selected background plugin id ({@code ""} clears it) and invalidates the cache. */
  public static void select(Context context, String id) {
    new PreferencesUtils(context.getApplicationContext())
        .setBackgroundPlugin(id == null ? "" : id);
    BackgroundChildRegistry.bumpGeneration();
  }
}
