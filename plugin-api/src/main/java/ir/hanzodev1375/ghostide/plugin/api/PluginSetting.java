package ir.hanzodev1375.ghostide.plugin.api;

import java.util.function.Consumer;

/**
 * One settings row a plugin contributes to the host settings screen instead of drawing its own
 * preferences UI. Two kinds are supported:
 *
 * <ul>
 *   <li>{@link Toggle} — a labelled switch bound to a boolean; the plugin is told on every change
 *       so it can persist the value (e.g. through {@link CoreServices#PLUGIN_STORAGE}).
 *   <li>{@link Action} — a labelled row that simply runs code when tapped. Use this for anything
 *       richer (a choice, a slider, opening a dialog) and let the plugin show its own UI from the
 *       callback.
 * </ul>
 *
 * <p>Build rows with the static factories so plugin code stays short:
 *
 * <pre>{@code
 * PluginSetting.toggle("wrap", "Word wrap", "Wrap long lines", storage.getBoolean("wrap", true),
 *     value -> storage.putBoolean("wrap", value));
 * PluginSetting.action("reset", "Reset cache", () -> clearCache());
 * }</pre>
 *
 * <p>This type is deliberately free of Android types; the host renders it with its own widgets.
 */
public sealed interface PluginSetting permits PluginSetting.Toggle, PluginSetting.Action {

  /**
   * Stable id of this row within its {@link PluginSettingsContributor}, unique across the plugin.
   */
  String getId();

  /** Short user-facing label shown on the settings row. */
  String getTitle();

  /** Optional one-line explanation; empty string when there is nothing useful to say. */
  default String getDescription() {
    return "";
  }

  static Toggle toggle(
      String id, String title, String description, boolean value, Consumer<Boolean> onChange) {
    return new Toggle(id, title, description, value, onChange);
  }

  static Toggle toggle(String id, String title, boolean value, Consumer<Boolean> onChange) {
    return new Toggle(id, title, "", value, onChange);
  }

  static Action action(String id, String title, String description, Runnable onClick) {
    return new Action(id, title, description, onClick);
  }

  static Action action(String id, String title, Runnable onClick) {
    return new Action(id, title, "", onClick);
  }

  /** A switch row. {@code value} is the current state; {@code onChange} fires on every toggle. */
  record Toggle(
      String id, String title, String description, boolean value, Consumer<Boolean> onChange)
      implements PluginSetting {

    public Toggle {
      requireText(id, "id");
      requireText(title, "title");
      if (description == null) description = "";
      if (onChange == null) {
        throw new IllegalArgumentException("Plugin setting onChange must not be null");
      }
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  /** A plain clickable row. {@code onClick} runs on the UI thread when the user taps it. */
  record Action(String id, String title, String description, Runnable onClick)
      implements PluginSetting {

    public Action {
      requireText(id, "id");
      requireText(title, "title");
      if (description == null) description = "";
      if (onClick == null) {
        throw new IllegalArgumentException("Plugin setting onClick must not be null");
      }
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Plugin setting " + field + " must not be blank");
    }
  }
}
