package ir.hanzodev1375.ghostide.plugin.api;

import java.util.List;

/**
 * Lets a plugin add its own preferences to the host settings screen instead of drawing a separate
 * settings view. Register an implementation at {@link CoreExtensionPoints#PLUGIN_SETTINGS}; the
 * host renders every {@link PluginSetting} returned by {@link #getSettings()} and tags each row
 * with {@link #getDisplayName()} so the user can tell which plugin it belongs to.
 *
 * <p>The list is read fresh every time the settings screen opens, so a contributor may compute rows
 * lazily from its current state:
 *
 * <pre>{@code
 * public final class MySettings implements PluginSettingsContributor {
 *   private final PluginStorage storage;
 *
 *   @Override public String getId() { return "com.example.myplugin.settings"; }
 *   @Override public String getDisplayName() { return "My Plugin"; }
 *
 *   @Override public List<PluginSetting> getSettings() {
 *     return List.of(
 *         PluginSetting.toggle("wrap", "Word wrap", storage.getBoolean("wrap", true),
 *             value -> storage.putBoolean("wrap", value)),
 *         PluginSetting.action("reset", "Reset cache", this::clearCache));
 *   }
 * }
 * }</pre>
 *
 * <p>Implementations may be called on any thread; do not block for long inside {@link
 * #getSettings()} and never touch UI from it. Callbacks on {@link PluginSetting} rows run on the UI
 * thread.
 */
public interface PluginSettingsContributor extends ConfigurableExtension {

  /** Rows to show for this plugin, in display order. Return an empty list for nothing. */
  List<PluginSetting> getSettings();
}
