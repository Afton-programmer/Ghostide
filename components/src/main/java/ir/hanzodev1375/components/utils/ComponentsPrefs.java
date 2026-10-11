package ir.hanzodev1375.components.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.StringJoiner;
import ir.hanzodev1375.components.searchdata.utils.GlobExcluder;

public class ComponentsPrefs {

  public static final String KEY_PARALLAX = "app_parallax";
  public static final String KEY_EXCLUDED_FILES_SEARCH = "search_excluded_files";
  public static final String KEY_GLASS_ENABLED = "app_glass_enabled";

  /** Devices above this much RAM get the glass look enabled by default. */
  public static final long GLASS_RAM_THRESHOLD_BYTES = 4L * 1024L * 1024L * 1024L;

  private final SharedPreferences prefs;
  private final Context appContext;

  public ComponentsPrefs(Context context) {
    appContext = context.getApplicationContext();
    prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
  }

  /**
   * Whether the liquid-glass look is on. When the user has never chosen, it defaults to on for
   * devices with more than 4 GB of RAM and off for weaker ones (glass is a heavy GPU effect), and
   * that decision is persisted so a low-RAM device can still opt in.
   */
  public boolean isGlassEnabled() {
    if (prefs.contains(KEY_GLASS_ENABLED)) {
      return prefs.getBoolean(KEY_GLASS_ENABLED, true);
    }
    boolean value = defaultGlassEnabled(appContext);
    prefs.edit().putBoolean(KEY_GLASS_ENABLED, value).apply();
    return value;
  }

  public void setGlassEnabled(boolean enabled) {
    prefs.edit().putBoolean(KEY_GLASS_ENABLED, enabled).apply();
  }

  /** Default glass state for a device, based only on its total RAM. */
  public static boolean defaultGlassEnabled(Context context) {
    if (context == null) {
      return true;
    }
    try {
      ActivityManager manager =
          (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      if (manager == null) {
        return true;
      }
      ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
      manager.getMemoryInfo(info);
      return info.totalMem > GLASS_RAM_THRESHOLD_BYTES;
    } catch (Throwable ignored) {
      return true;
    }
  }

  public boolean isGlassMaterialColor() {
    return prefs.getBoolean("m3glassColor", true);
  }

  public float getGlassTint() {
    return prefs.getFloat("m3glassTint", 0.6f);
  }

  public void setGlassTint(float value) {
    prefs.edit().putFloat("m3glassTint", value).apply();
  }

  public boolean isBlurMod() {
    return prefs.getBoolean("app_blurmod_allstate", true);
  }

  public boolean isParallaxEnabled() {
    return prefs.getBoolean(KEY_PARALLAX, true);
  }

  public void setParallaxEnabled(boolean enabled) {
    prefs.edit().putBoolean(KEY_PARALLAX, enabled).apply();
  }

  public boolean isShowBackground() {
    return prefs.getBoolean("filemanager_showbackgroundtheme", false);
  }

  public int getAnimationBatteryThreshold() {
    return prefs.getInt("pref_animation_battery_threshold", 20);
  }

  public String getExcludedFilesText() {
    return prefs.getString(KEY_EXCLUDED_FILES_SEARCH, defaultExcludedFilesText());
  }

  public void setExcludedFilesText(String text) {
    prefs.edit().putString(KEY_EXCLUDED_FILES_SEARCH, text).apply();
  }

  public void resetExcludedFiles() {
    prefs.edit().remove(KEY_EXCLUDED_FILES_SEARCH).apply();
  }

  public static String defaultExcludedFilesText() {
    StringJoiner joiner = new StringJoiner("\n");
    for (String glob : GlobExcluder.DEFAULT_EXCLUDED_FILES) joiner.add(glob);
    return joiner.toString();
  }
}