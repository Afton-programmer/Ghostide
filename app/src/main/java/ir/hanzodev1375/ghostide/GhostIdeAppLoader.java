package ir.hanzodev1375.ghostide;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.downloader.PRDownloader;
import ir.hanzodev1375.components.store.notification.PluginCheckLifecycle;
import ir.hanzodev1375.components.store.notification.PluginNotifier;
import ir.hanzodev1375.components.glsl.GlslBackgroundFactory;
import ir.hanzodev1375.components.views.GhostToast;
import ir.hanzodev1375.ghostide.adapters.UiFeedbackHostImpl;
import ir.hanzodev1375.ghostide.activity.ErrorManagerActivity;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.ProotProcessLauncherImpl;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.ide.ui.api.FileIconContributor;
import ir.hanzodev1375.ghostide.ide.ui.api.IdeHostServices;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.materialfileicon.core.JsonFileIconHelper;
import ir.hanzodev1375.ghostide.plugin.BackgroundRendererBridge;
import ir.hanzodev1375.ghostide.plugin.api.ExtensionRegistration;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.hanzodev1375.ghostide.plugin.PluginManager;
import ir.hanzodev1375.ghostide.plugin.gpl.GplInstalledPlugins;
import ir.hanzodev1375.ghostide.plugin.gpl.GplPluginLoader;
import ir.hanzodev1375.ghostide.shizuku.ShizukuManager;
import ir.hanzodev1375.ghostide.snippets.UserSnippetCompletionProvider;
import ir.theme.M3Theme;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GhostIdeAppLoader extends Application {

  private static final String TAG = "GhostIdeAppLoader";
  private static Context mApplicationContext;
  private static GhostIdeAppLoader loader;
  private final StringBuilder softwareInfo = new StringBuilder();
  private PreferencesUtils setting;
  private ThemeUtils theme;

  public static Context getContext() {
    return mApplicationContext;
  }

  public static GhostIdeAppLoader getInstance() {
    return loader;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    // The app is always dark; colors come from the JSON/M3 theme, never from the system day mode.
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    loader = this;
    setting = new PreferencesUtils(this);
    mApplicationContext = getApplicationContext();
    ShizukuManager.registerListeners();
    M3Theme.init(this);
    var themeManager = new ThemeManager(this);
    theme = new ThemeUtils(themeManager);
    GhostToast.bindOfApp(this);
    PRDownloader.initialize(getApplicationContext());
    GlobalRegistry.services()
        .register(IdeHostServices.PROOT_PROCESS_LAUNCHER, new ProotProcessLauncherImpl(this));
    UiFeedbackHostImpl uiFeedbackHost = new UiFeedbackHostImpl();
    registerActivityLifecycleCallbacks(uiFeedbackHost);
    GlobalRegistry.services()
        .register(IdeHostServices.UI_FEEDBACK, uiFeedbackHost);
    JsonFileIconHelper.setExternalResolver(
        path -> {
          String selected = setting.getIconPack();
          if (selected == null || selected.isEmpty()) {
            return null;
          }
          for (ExtensionRegistration<FileIconContributor> registration :
              GlobalRegistry.extensions()
                  .registrations(PluginUiExtensionPoints.FILE_ICON_CONTRIBUTOR)) {
            if (!selected.equals(registration.ownerPluginId())) continue;
            try {
              String icon = registration.extension().getIcon(path);
              if (icon != null && !icon.trim().isEmpty()) return icon;
            } catch (Exception ignored) {
            }
          }
          return null;
        });
    BackgroundRendererBridge.install();
    GlslBackgroundFactory.register();
    // Initialize the plugin host at app start so the file manager can reach it on first launch.
    PluginManager.init(this);
    GplInstalledPlugins.loadAll(this, GplPluginLoader.getInstance(this));

    PluginNotifier.schedule(this);
    PluginNotifier.checkNow(this);
    PluginCheckLifecycle.init(this);

    IdeEditor.setDefaultUserSnippetProvider(new UserSnippetCompletionProvider(this));

    extractCursorZip();

    Thread.setDefaultUncaughtExceptionHandler(
        new Thread.UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread thread, Throwable throwable) {
            Intent intent = new Intent(getApplicationContext(), ErrorManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("error", Log.getStackTraceString(throwable));
            startActivity(intent);
            Process.killProcess(Process.myPid());
            System.exit(1);
          }
        });
  }

  public void restartApp() {
    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
    }
    Process.killProcess(Process.myPid());
  }

  public boolean isSdkS() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
  }

  public boolean isSdkQ() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
  }

  public String getVersion() {
    try {
      PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
      return info.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }

  public PreferencesUtils getSetting() {
    return setting;
  }

  public ThemeUtils getThemeUtils() {
    return theme;
  }

  private void extractCursorZip() {
    // Zip extraction is blocking I/O; editor settings read these files lazily,
    // so keep it off the main thread and log failures instead of ignoring them.
    new Thread(
            () -> {
              File cursorDir = new File("/storage/emulated/0/ghostide/cursor");
              File jsonMarker = new File(cursorDir, "cursor.json");
              if (cursorDir.exists() && jsonMarker.exists()) {
                return;
              }
              if (!cursorDir.exists() && !cursorDir.mkdirs()) {
                Log.w(TAG, "Cannot create cursor directory: " + cursorDir.getAbsolutePath());
                return;
              }
              try (InputStream is = getAssets().open("cursor.zip");
                  ZipInputStream zis = new ZipInputStream(is)) {
                String canonicalDir = cursorDir.getCanonicalPath() + File.separator;
                byte[] buffer = new byte[4096];
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                  if (entry.isDirectory()) continue;
                  File outFile = new File(cursorDir, entry.getName());
                  // Reject entries that would escape the target directory (zip path traversal).
                  if (!outFile.getCanonicalPath().startsWith(canonicalDir)) {
                    Log.w(TAG, "Skipped cursor.zip entry outside target dir: " + entry.getName());
                    continue;
                  }
                  try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                      fos.write(buffer, 0, len);
                    }
                  }
                  zis.closeEntry();
                }
              } catch (Exception e) {
                Log.w(TAG, "Failed to extract cursor.zip", e);
              }
            },
            "CursorZipExtractor")
        .start();
  }
}
