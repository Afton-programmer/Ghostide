package ir.hanzodev1375.ghostide.plugin.api;

/** Extension points at the {@code :plugin-api} layer, usable by Android-free plugins. */
public final class CoreExtensionPoints {

  /** Commands contributed by plugins for the host to list and invoke (e.g. a command palette). */
  public static final ExtensionPoint<PluginCommand> PLUGIN_COMMAND =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.core.pluginCommand", PluginCommand.class);

  /**
   * Preferences contributed by plugins, rendered inside the host settings screen. Each contributor
   * declares a group of rows; see {@link PluginSettingsContributor} and {@link PluginSetting}.
   */
  public static final ExtensionPoint<PluginSettingsContributor> PLUGIN_SETTINGS =
      new ExtensionPoint<>(
          "ir.hanzodev1375.ghostide.core.pluginSettings", PluginSettingsContributor.class);

  private CoreExtensionPoints() {}
}
