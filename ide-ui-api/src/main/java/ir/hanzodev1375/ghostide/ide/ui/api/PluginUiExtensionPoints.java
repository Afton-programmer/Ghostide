package ir.hanzodev1375.ghostide.ide.ui.api;

import ir.hanzodev1375.ghostide.plugin.api.ExtensionPoint;

/** Extension points for UI contributions. */
public final class PluginUiExtensionPoints {

  public static final ExtensionPoint<PluginScreen> PLUGIN_SCREEN =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.ui.pluginScreen", PluginScreen.class);

  /**
   * Panels that slide out inside a host screen (currently the editor). Kept separate from {@link
   * #PLUGIN_SCREEN} so the previous, whole-screen contribution API keeps working unchanged.
   */
  public static final ExtensionPoint<EditorPanel> EDITOR_PANEL =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.ui.editorPanel", EditorPanel.class);

  /**
   * Panels that slide out inside the file manager. Same contract as {@link #EDITOR_PANEL} but the
   * host screen is the file manager, so a plugin (e.g. git clone, archives, sync) can show its UI
   * without leaving the file list.
   */
  public static final ExtensionPoint<FilePanel> FILE_PANEL =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.ui.filePanel", FilePanel.class);

  /**
   * Handlers that intercept LSP command actions locally (e.g. code actions sent by a plugin
   * language server) and run them inside the editor, with the raw {@code IdeEditor} available.
   */
  public static final ExtensionPoint<EditorActionHandler> EDITOR_ACTION_HANDLER =
      new ExtensionPoint<>(
          "ir.hanzodev1375.ghostide.ui.editorActionHandler", EditorActionHandler.class);

  /**
   * Custom file icons contributed by plugins. Consulted (by descending priority) before the
   * built-in icon set everywhere file icons are shown: file manager, editor tabs, history,
   * bookmarks.
   */
  public static final ExtensionPoint<FileIconContributor> FILE_ICON_CONTRIBUTOR =
      new ExtensionPoint<>(
          "ir.hanzodev1375.ghostide.ui.fileIconContributor", FileIconContributor.class);

  /**
   * Toolbar actions shown persistently next to the IDE's own toolbar icons. Each {@link
   * PluginToolbarIcon} is added to the toolbar of the screen it targets ({@link
   * PluginIconState#Editor} in the editor, {@link PluginIconState#FileES} in the file manager), so
   * a plugin action is always one tap away instead of living only in the plugin popup.
   */
  public static final ExtensionPoint<PluginToolbarIcon> TOOLBAR_ICON =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.ui.toolbarIcon", PluginToolbarIcon.class);

  /**
   * Handlers that take over a single tap on a file or folder in the file manager, so a plugin can
   * react to its own formats (SVG, archives, custom project files, ...) and show its own UI instead
   * of the IDE's built-in viewer. Consulted in descending priority order; the first {@link
   * FileClickHandler} returning {@code true} from {@link FileClickHandler#supports} wins and the
   * built-in opening is skipped. A plugin that returns {@code false}, or that is not registered at
   * all, leaves the default behaviour untouched.
   */
  public static final ExtensionPoint<FileClickHandler> FILE_CLICK_HANDLER =
      new ExtensionPoint<>(
          "ir.hanzodev1375.ghostide.ui.fileClickHandler", FileClickHandler.class);

  /**
   * Custom screen background (wallpaper) renderers. Consulted by every {@code ViewChilder} before
   * the built-in image/gif/video detection, so a plugin can back the IDE wallpaper with a video,
   * a GLSL/shader scene, a live canvas animation or any custom view. A renderer claims the paths
   * or URI schemes it owns via {@link PluginBackgroundRenderer#supports}; consulted in descending
   * priority order and the first match wins.
   */
  public static final ExtensionPoint<PluginBackgroundRenderer> BACKGROUND_RENDERER =
      new ExtensionPoint<>(
          "ir.hanzodev1375.ghostide.ui.backgroundRenderer", PluginBackgroundRenderer.class);

  private PluginUiExtensionPoints() {}
}
