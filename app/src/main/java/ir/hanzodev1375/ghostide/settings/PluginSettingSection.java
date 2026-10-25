package ir.hanzodev1375.ghostide.settings;

import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.activity.SettingActivity;
import ir.hanzodev1375.ghostide.models.SettingItem;
import ir.hanzodev1375.ghostide.plugin.api.CoreExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.api.ExtensionRegistration;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.hanzodev1375.ghostide.plugin.api.PluginSetting;
import ir.hanzodev1375.ghostide.plugin.api.PluginSettingsContributor;

/**
 * Settings contributed by plugins through {@link CoreExtensionPoints#PLUGIN_SETTINGS}. The section
 * flattens every contributor's rows into this screen's own list so plugin preferences sit next to
 * the built-in ones. Toggle rows become switches; action rows become plain clickable rows.
 */
public class PluginSettingSection extends SettingSection {

  /** Parallel to the built item list: the plugin setting behind each row, by row position. */
  private final List<PluginSetting> settings = new ArrayList<>();

  public PluginSettingSection(SettingActivity activity) {
    super(activity);
  }

  @Override
  protected List<SettingItem> buildItems() {
    settings.clear();
    List<SettingItem> items = new ArrayList<>();
    for (ExtensionRegistration<PluginSettingsContributor> registration :
        GlobalRegistry.extensions().registrations(CoreExtensionPoints.PLUGIN_SETTINGS)) {
      PluginSettingsContributor contributor = registration.extension();
      String owner = contributor.getDisplayName();
      if (owner == null || owner.trim().isEmpty()) {
        owner = registration.ownerPluginId();
      }
      List<PluginSetting> contributed;
      try {
        contributed = contributor.getSettings();
      } catch (Exception t) {
        continue;
      }
      if (contributed == null) continue;
      for (PluginSetting setting : contributed) {
        if (setting == null) continue;
        items.add(toSettingItem(setting, owner));
        settings.add(setting);
      }
    }
    return items;
  }

  private SettingItem toSettingItem(PluginSetting setting, String owner) {
    String description = setting.getDescription();
    if (owner != null && !owner.isEmpty()) {
      description =
          description == null || description.isEmpty() ? owner : owner + " — " + description;
    }
    if (setting instanceof PluginSetting.Toggle toggle) {
      return new SettingItem(
          toggle.getTitle(),
          description,
          toggle.value(),
          0,
          isChecked -> toggle.onChange().accept(isChecked));
    }
    PluginSetting.Action action = (PluginSetting.Action) setting;
    return new SettingItem(action.getTitle(), description, false, 0, null);
  }

  @Override
  public void onItemClick(int position) {
    if (position < 0 || position >= settings.size()) return;
    PluginSetting setting = settings.get(position);
    if (setting instanceof PluginSetting.Action action) {
      try {
        action.onClick().run();
      } catch (Exception ignored) {
        // A misbehaving plugin must not take down the settings screen.
      }
    }
  }
}
