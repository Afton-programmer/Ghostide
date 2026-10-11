package ir.hanzodev1375.ghostide.ide.ui.api;

/**
 * Opens a plugin {@link Panel} the same way the plugin popup does (side sheet, dialog, bottom
 * sheet, floating window, ...) according to the panel's {@link PluginStateMod}. Lets a plugin
 * action such as a toolbar icon run its own panel without routing through the popup.
 */
public interface PanelHostService {

  /** Shows {@code panel} on the current host screen; no-op if the host has no way to render it. */
  void showPanel(Panel panel);
}