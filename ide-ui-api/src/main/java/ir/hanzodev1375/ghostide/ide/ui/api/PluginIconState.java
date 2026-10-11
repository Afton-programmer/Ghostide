package ir.hanzodev1375.ghostide.ide.ui.api;

/**
 * Which host screen a {@link PluginToolbarIcon} should appear in.
 *
 * <p>{@link #Editor} shows the icon in the editor's toolbar, {@link #FileES} in the file manager
 * (file system) toolbar. The host reads the extension point and appends every matching icon to its
 * toolbar, so a plugin's action is always one tap away instead of being buried in the plugin popup.
 */
public enum PluginIconState {
  Editor,
  FileES
}