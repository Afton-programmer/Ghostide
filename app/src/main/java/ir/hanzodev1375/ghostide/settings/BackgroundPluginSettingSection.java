package ir.hanzodev1375.ghostide.settings;

import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.activity.SettingActivity;
import ir.hanzodev1375.ghostide.plugin.BackgroundPluginManager;
import ir.hanzodev1375.ghostide.models.SettingItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Pick which installed background plugin (renderer) applies; "Default" uses the theme background.
 * The section exposes no rows when no background plugin is installed, so its card is hidden.
 */
public class BackgroundPluginSettingSection extends SettingSection {

  private static final int POS_PLUGIN = 0;

  public BackgroundPluginSettingSection(SettingActivity activity) {
    super(activity);
  }

  @Override
  protected List<SettingItem> buildItems() {
    List<SettingItem> items = new ArrayList<>();
    if (BackgroundPluginManager.listInstalled(activity).isEmpty()) {
      return items;
    }
    items.add(textItem(R.string.background_plugin_title, currentPluginDescription()));
    return items;
  }

  @Override
  public void onItemClick(int position) {
    if (position == POS_PLUGIN) showPluginDialog();
  }

  private String currentPluginDescription() {
    String selected = BackgroundPluginManager.selected(activity);
    if (selected == null || selected.isEmpty()) {
      return getString(R.string.background_plugin_default);
    }
    for (BackgroundPluginManager.BackgroundPluginEntry entry :
        BackgroundPluginManager.listInstalled(activity)) {
      if (entry.id().equals(selected)) {
        if (entry.version() == null || entry.version().isEmpty()) {
          return entry.name();
        }
        return entry.name() + "  •  v" + entry.version();
      }
    }
    return getString(R.string.background_plugin_default);
  }

  private void showPluginDialog() {
    List<BackgroundPluginManager.BackgroundPluginEntry> plugins =
        BackgroundPluginManager.listInstalled(activity);
    String selected = BackgroundPluginManager.selected(activity);
    String[] names = new String[plugins.size() + 1];
    names[0] = getString(R.string.background_plugin_default);
    int checked = 0;
    for (int i = 0; i < plugins.size(); i++) {
      BackgroundPluginManager.BackgroundPluginEntry plugin = plugins.get(i);
      String version = plugin.version() == null ? "" : plugin.version();
      names[i + 1] = version.isEmpty() ? plugin.name() : plugin.name() + "  •  v" + version;
      if (selected != null && selected.equals(plugin.id())) checked = i + 1;
    }
    showSingleChoiceDialog(
        R.string.background_plugin_title,
        names,
        checked,
        which -> {
          String id = which == 0 ? "" : plugins.get(which - 1).id();
          BackgroundPluginManager.select(activity, id);
          updateDescriptionAt(POS_PLUGIN, currentPluginDescription());
          activity.refreshBackgroundUi();
        });
  }
}
