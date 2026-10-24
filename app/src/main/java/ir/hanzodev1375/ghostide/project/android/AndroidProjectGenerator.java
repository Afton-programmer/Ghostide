package ir.hanzodev1375.ghostide.project.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import ir.hanzodev1375.ghostide.project.DslType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AndroidProjectGenerator {

  public interface OnGenerateListener {
    void onSuccess(String projectPath);

    void onError(String message);
  }

  private static final String ASSET_ROOT = "templates/android/";
  private static final String COMMON_DIR = ASSET_ROOT + "common/";
  private static final String PROJECTS_DIR = ASSET_ROOT + "projects/";

  private final Context context;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public AndroidProjectGenerator(Context context) {
    this.context = context.getApplicationContext();
  }

  public void generateAsync(AndroidProjectConfig config, OnGenerateListener listener) {
    new Thread(
            () -> {
              try {
                String projectPath = generate(config);
                mainHandler.post(() -> listener.onSuccess(projectPath));
              } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                mainHandler.post(() -> listener.onError(message));
              }
            })
        .start();
  }

  public String generate(AndroidProjectConfig config) throws IOException {
    validateConfig(config);

    File root = new File(config.getParentPath(), config.getProjectName());
    if (root.exists()) {
      throw new IOException(
          "A folder named \"" + config.getProjectName() + "\" already exists here.");
    }
    if (!root.mkdirs()) {
      throw new IOException("Failed to create project folder: " + root.getAbsolutePath());
    }

    boolean isKts = config.getDslType() == DslType.KOTLIN;
    boolean isKotlin = config.getLanguage() == AndroidProjectConfig.Language.KOTLIN;
    String pkg = config.getPackageName();
    String pkgPath = pkg.replace('.', '/');

    Map<String, String> tokens = baseTokens(config);

    File appDir = new File(root, "app");
    File gradleWrapperDir = new File(root, "gradle/wrapper");
    File srcMainPkg =
        new File(appDir, "src/main/" + (isKotlin ? "kotlin" : "java") + "/" + pkgPath);
    if (!srcMainPkg.mkdirs()) {
      throw new IOException("Failed to create source folder: " + srcMainPkg.getAbsolutePath());
    }
    if (!gradleWrapperDir.mkdirs()) {
      throw new IOException(
          "Failed to create wrapper folder: " + gradleWrapperDir.getAbsolutePath());
    }

    writeWrapperFiles(root, gradleWrapperDir);

    tokens.putAll(buildSnippets(config, isKts));
    copyText(
        COMMON_DIR + (isKts ? "settings.gradle.kts.tpl" : "settings.gradle.tpl"),
        new File(root, isKts ? "settings.gradle.kts" : "settings.gradle"),
        tokens);
    copyText(
        COMMON_DIR + (isKts ? "root.build.gradle.kts.tpl" : "root.build.gradle.tpl"),
        new File(root, isKts ? "build.gradle.kts" : "build.gradle"),
        tokens);
    copyText(
        COMMON_DIR + "libs.versions.toml", new File(root, "gradle/libs.versions.toml"), tokens);
    copyText(COMMON_DIR + "gitignore", new File(root, ".gitignore"), tokens);
    copyText(COMMON_DIR + "gradle.properties", new File(root, "gradle.properties"), tokens);
    copyText(COMMON_DIR + "generate-keystore.sh", new File(root, "generate-keystore.sh"), tokens);
    new File(root, "generate-keystore.sh").setExecutable(true, false);

    copyText(
        COMMON_DIR + (isKts ? "app.build.gradle.kts.tpl" : "app.build.gradle.tpl"),
        new File(appDir, isKts ? "build.gradle.kts" : "build.gradle"),
        tokens);
    copyText(COMMON_DIR + "app.gitignore", new File(appDir, ".gitignore"), tokens);
    copyText(COMMON_DIR + "proguard-rules.pro", new File(appDir, "proguard-rules.pro"), tokens);

    tokens.putAll(manifestSnippets(config));
    copyText(
        COMMON_DIR + "AndroidManifest.xml.tpl",
        new File(appDir, "src/main/AndroidManifest.xml"),
        tokens);

    copyTree(COMMON_DIR + "res", new File(appDir, "src/main/res"), tokens, null);

    String templateDir = PROJECTS_DIR + config.getTemplate().name() + "/";
    copyTree(templateDir + "res", new File(appDir, "src/main/res"), tokens, null);
    copyTree(templateDir + "src", srcMainPkg, tokens, isKotlin);

    return root.getAbsolutePath();
  }

  private Map<String, String> baseTokens(AndroidProjectConfig config) {
    Map<String, String> tokens = new HashMap<>();
    tokens.put("PACKAGE", config.getPackageName());
    tokens.put("PROJECT_NAME", config.getProjectName());
    tokens.put("PROJECT_NAME_ESCAPED", config.getProjectName().replace("\"", "\\\""));
    tokens.put("PROJECT_NAME_XML", escapeXml(config.getProjectName()));
    tokens.put("THEME_NAME", sanitizeThemeName(config.getProjectName()));
    tokens.put("MIN_SDK", String.valueOf(config.getMinSdk()));
    tokens.put("TARGET_SDK", String.valueOf(config.getTargetSdk()));
    tokens.put("COMPILE_SDK", String.valueOf(config.getCompileSdk()));
    return tokens;
  }

  private Map<String, String> buildSnippets(AndroidProjectConfig config, boolean isKts) {
    boolean isKotlin = config.getLanguage() == AndroidProjectConfig.Language.KOTLIN;
    boolean isCompose = config.getTemplate().isSupportsCompose();
    boolean isNavigation =
        config.getTemplate() == AndroidTemplate.PHONE_BASIC_VIEWS
            || config.getTemplate() == AndroidTemplate.PHONE_BOTTOM_NAV
            || config.getTemplate() == AndroidTemplate.PHONE_NAVIGATION_DRAWER;
    boolean isViews = !isCompose && config.getTemplate() != AndroidTemplate.PHONE_NO_ACTIVITY;

    Map<String, String> tokens = new HashMap<>();
    String nl = "\n";
    String kotlinPlugin =
        isKotlin
            ? (isKts
                ? nl + "    alias(libs.plugins.kotlin.android)"
                : nl + "    alias libs.plugins.kotlin.android")
            : "";
    String kotlinOptions =
        isKotlin
            ? nl
                + nl
                + "    kotlinOptions {"
                + nl
                + (isKts ? "        jvmTarget = \"17\"" : "        jvmTarget = '17'")
                + nl
                + "    }"
            : "";

    StringBuilder features = new StringBuilder();
    if (isCompose || isViews) {
      features.append(nl).append(nl).append("    buildFeatures {").append(nl);
      if (isViews)
        features
            .append(isKts ? "        viewBinding = true" : "        viewBinding true")
            .append(nl);
      if (isCompose)
        features.append(isKts ? "        compose = true" : "        compose true").append(nl);
      features.append("    }");
    }

    String composeOptions =
        isCompose
            ? nl
                + nl
                + "    composeOptions {"
                + nl
                + "        kotlinCompilerExtensionVersion = \"1.5.14\""
                + nl
                + "    }"
            : "";

    String prefix = isKts ? "    implementation(libs." : "    implementation libs.";
    String closing = isKts ? ")" : "";

    StringBuilder composeDeps = new StringBuilder();
    if (isCompose) {
      composeDeps
          .append(prefix)
          .append("androidx.lifecycle.runtime.ktx")
          .append(closing)
          .append(nl);
      composeDeps.append(prefix).append("androidx.activity.compose").append(closing).append(nl);
      composeDeps
          .append("    implementation platform(")
          .append(isKts ? "libs.androidx.compose.bom" : "libs.androidx.compose.bom")
          .append(")")
          .append(nl);
      composeDeps.append(prefix).append("androidx.ui").append(closing).append(nl);
      composeDeps.append(prefix).append("androidx.ui.graphics").append(closing).append(nl);
      composeDeps.append(prefix).append("androidx.ui.tooling.preview").append(closing).append(nl);
      composeDeps.append(prefix).append("androidx.material3").append(closing).append(nl);
      composeDeps
          .append("    debugImplementation")
          .append(isKts ? "(libs.androidx.ui.tooling)" : " libs.androidx.ui.tooling")
          .append(nl);
      composeDeps
          .append("    debugImplementation")
          .append(isKts ? "(libs.androidx.ui.test.manifest)" : " libs.androidx.ui.test.manifest")
          .append(nl);
    }

    String viewsDeps = isViews ? prefix + "androidx.constraintlayout" + closing + nl : "";

    StringBuilder navDeps = new StringBuilder();
    if (isNavigation) {
      navDeps.append(prefix).append("androidx.navigation.fragment.ktx").append(closing).append(nl);
      navDeps.append(prefix).append("androidx.navigation.ui.ktx").append(closing).append(nl);
    }

    StringBuilder categoryDeps = new StringBuilder();
    if (config.getCategory() == AndroidCategory.WEAR_OS) {
      categoryDeps
          .append(prefix)
          .append("androidx.wear.compose.material")
          .append(closing)
          .append(nl);
      categoryDeps
          .append(prefix)
          .append("androidx.wear.compose.foundation")
          .append(closing)
          .append(nl);
      categoryDeps.append(prefix).append("androidx.wear.watchface").append(closing).append(nl);
      categoryDeps.append(prefix).append("androidx.wear.tiles").append(closing).append(nl);
      categoryDeps.append(prefix).append("androidx.wear.protolayout").append(closing).append(nl);
    } else if (config.getCategory() == AndroidCategory.ANDROID_TV) {
      categoryDeps.append(prefix).append("androidx.leanback").append(closing).append(nl);
      categoryDeps.append(prefix).append("androidx.tv.foundation").append(closing).append(nl);
      categoryDeps.append(prefix).append("androidx.tv.material").append(closing).append(nl);
    } else if (config.getCategory() == AndroidCategory.AUTOMOTIVE) {
      categoryDeps.append(prefix).append("androidx.car.app").append(closing).append(nl);
      categoryDeps.append(prefix).append("androidx.media3.session").append(closing).append(nl);
    }

    tokens.put("KOTLIN_PLUGIN", kotlinPlugin);
    tokens.put("KOTLIN_OPTIONS", kotlinOptions);
    tokens.put("BUILD_FEATURES", features.toString());
    tokens.put("COMPOSE_OPTIONS", composeOptions);
    tokens.put("COMPOSE_DEPS", composeDeps.toString());
    tokens.put("VIEWS_DEPS", viewsDeps);
    tokens.put("NAV_DEPS", navDeps.toString());
    tokens.put("CATEGORY_DEPS", categoryDeps.toString());
    return tokens;
  }

  private Map<String, String> manifestSnippets(AndroidProjectConfig config) {
    AndroidTemplate template = config.getTemplate();
    AndroidCategory category = config.getCategory();
    StringBuilder features = new StringBuilder();
    if (category == AndroidCategory.WEAR_OS) {
      features.append("    <uses-feature android:name=\"android.hardware.type.watch\" />\n");
    } else if (category == AndroidCategory.ANDROID_TV) {
      features.append(
          "    <uses-feature android:name=\"android.software.leanback\" android:required=\"false\" />\n");
      features.append(
          "    <uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />\n");
    } else if (category == AndroidCategory.AUTOMOTIVE) {
      features.append(
          "    <uses-feature android:name=\"android.hardware.type.automotive\" android:required=\"true\" />\n");
    }

    StringBuilder component = new StringBuilder();
    if (template == AndroidTemplate.WEAR_WATCH_FACE) {
      component
          .append("        <service\n")
          .append("            android:name=\".MyWatchFaceService\"\n")
          .append("            android:exported=\"true\"\n")
          .append("            android:label=\"@string/app_name\"\n")
          .append("            android:permission=\"android.permission.BIND_WALLPAPER\">\n")
          .append("            <intent-filter>\n")
          .append(
              "                <action android:name=\"android.service.wallpaper.WallpaperService\" />\n")
          .append(
              "                <category android:name=\"com.google.android.wearable.watchface.category.WATCH_FACE\" />\n")
          .append("            </intent-filter>\n")
          .append("        </service>\n");
    } else if (template == AndroidTemplate.WEAR_TILE_SERVICE) {
      component
          .append("        <service\n")
          .append("            android:name=\".MyTileService\"\n")
          .append("            android:exported=\"true\"\n")
          .append("            android:label=\"@string/app_name\"\n")
          .append(
              "            android:permission=\"com.google.android.wearable.permission.BIND_TILE_PROVIDER\">\n")
          .append("            <intent-filter>\n")
          .append(
              "                <action android:name=\"androidx.wear.tiles.action.BIND_UPDATE_REQUESTER\" />\n")
          .append("            </intent-filter>\n")
          .append("        </service>\n");
    } else if (template == AndroidTemplate.AUTO_CAR_APP) {
      component
          .append("        <service\n")
          .append("            android:name=\".MyCarAppService\"\n")
          .append("            android:exported=\"true\">\n")
          .append("            <intent-filter>\n")
          .append("                <action android:name=\"androidx.car.app.CarAppService\" />\n")
          .append("                <category android:name=\"androidx.car.app.category.POI\" />\n")
          .append("            </intent-filter>\n")
          .append("        </service>\n")
          .append("        <meta-data\n")
          .append("            android:name=\"androidx.car.app.minCarApiLevel\"\n")
          .append("            android:value=\"1\" />\n");
    } else if (template == AndroidTemplate.AUTO_MEDIA_APP) {
      component
          .append("        <service\n")
          .append("            android:name=\".MyMediaPlaybackService\"\n")
          .append("            android:exported=\"true\">\n")
          .append("            <intent-filter>\n")
          .append(
              "                <action android:name=\"androidx.media3.session.MediaSessionService\" />\n")
          .append("            </intent-filter>\n")
          .append("        </service>\n");
    } else if (template != AndroidTemplate.PHONE_NO_ACTIVITY) {
      component
          .append("        <activity\n")
          .append("            android:name=\".MainActivity\"\n")
          .append("            android:exported=\"true\">\n")
          .append("            <intent-filter>\n")
          .append("                <action android:name=\"android.intent.action.MAIN\" />\n")
          .append(
              category == AndroidCategory.ANDROID_TV
                  ? "                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\" />\n"
                  : "                <category android:name=\"android.intent.category.LAUNCHER\" />\n")
          .append("            </intent-filter>\n")
          .append("        </activity>\n");
    }

    Map<String, String> tokens = new HashMap<>();
    tokens.put("CATEGORY_FEATURES", features.toString());
    tokens.put("COMPONENT_BLOCK", component.toString());
    return tokens;
  }

  private void validateConfig(AndroidProjectConfig config) throws IOException {
    if (config == null) throw new IOException("Configuration is null.");
    String name = config.getProjectName();
    if (name == null || name.trim().isEmpty()) {
      throw new IOException("Project name cannot be empty.");
    }
    if (!name.matches("[a-zA-Z0-9_ -]+")) {
      throw new IOException("Invalid project name. Use letters, numbers, spaces, or hyphens.");
    }
    String pkg = config.getPackageName();
    if (pkg == null || !isValidPackage(pkg)) {
      throw new IOException(
          "Invalid package name: \""
              + pkg
              + "\". Must have at least two segments (e.g. com.example.app).");
    }
  }

  private boolean isValidPackage(String pkg) {
    if (!pkg.contains(".")) return false;
    String[] parts = pkg.split("\\.", -1);
    for (String part : parts) {
      if (part.isEmpty() || !Character.isLetter(part.charAt(0))) return false;
      if (!part.matches("[a-zA-Z][a-zA-Z0-9_]*")) return false;
    }
    return true;
  }

  private void writeWrapperFiles(File root, File wrapperDir) throws IOException {
    copyTextOrFallback(
        "templates/android/wrapper/gradlew", new File(root, "gradlew"), getDefaultGradlewScript());
    new File(root, "gradlew").setExecutable(true, false);
    copyTextOrFallback(
        "templates/android/wrapper/gradlew.bat",
        new File(root, "gradlew.bat"),
        getDefaultGradlewBat());
    copyTextOrFallback(
        "templates/android/wrapper/gradle-wrapper.properties",
        new File(wrapperDir, "gradle-wrapper.properties"),
        getDefaultWrapperProperties());
    copyAssetBinary(
        "templates/android/wrapper/gradle-wrapper.jar", new File(wrapperDir, "gradle-wrapper.jar"));
  }

  private void copyTextOrFallback(String assetPath, File dest, String fallback) throws IOException {
    try (InputStream in = context.getAssets().open(assetPath)) {
      copyStream(in, dest);
    } catch (Exception e) {
      writeFile(dest, fallback);
    }
  }

  private void copyAssetBinary(String assetPath, File dest) {
    try (InputStream in = context.getAssets().open(assetPath)) {
      copyStream(in, dest);
    } catch (Exception ignored) {
    }
  }

  private void copyText(String assetPath, File dest, Map<String, String> tokens)
      throws IOException {
    String content;
    try (InputStream in = context.getAssets().open(assetPath)) {
      content = new String(readAll(in), StandardCharsets.UTF_8);
    }
    writeFile(dest, applyTokens(content, tokens));
  }

  private void copyTree(
      String assetDir, File destDir, Map<String, String> tokens, Boolean kotlinLanguage)
      throws IOException {
    String[] children;
    try {
      children = context.getAssets().list(assetDir);
    } catch (IOException e) {
      throw new IOException("Missing template asset: " + assetDir, e);
    }
    if (children == null || children.length == 0) {
      if (kotlinLanguage != null) {
        if (assetDir.endsWith(".java") && kotlinLanguage) return;
        if (assetDir.endsWith(".kt") && !kotlinLanguage) return;
      }
      String fileName = assetDir.substring(assetDir.lastIndexOf('/') + 1);
      if (fileName.endsWith(".tpl")) {
        fileName = fileName.substring(0, fileName.length() - 4);
      }
      String content;
      try (InputStream in = context.getAssets().open(assetDir)) {
        content = new String(readAll(in), StandardCharsets.UTF_8);
      } catch (IOException e) {
        return;
      }
      File dest = new File(destDir, fileName);
      if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
        throw new IOException("Failed to create folder: " + dest.getParentFile());
      }
      writeFile(dest, applyTokens(content, tokens));
      return;
    }
    if (!destDir.exists() && !destDir.mkdirs()) {
      throw new IOException("Failed to create folder: " + destDir);
    }
    for (String child : children) {
      copyTree(assetDir + "/" + child, destDir, tokens, kotlinLanguage);
    }
  }

  private String applyTokens(String content, Map<String, String> tokens) {
    String result = content;
    for (Map.Entry<String, String> entry : tokens.entrySet()) {
      result = result.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }

  private byte[] readAll(InputStream in) throws IOException {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int len;
    while ((len = in.read(buffer)) != -1) {
      out.write(buffer, 0, len);
    }
    return out.toByteArray();
  }

  private void copyStream(InputStream in, File dest) throws IOException {
    File parent = dest.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Failed to create folder: " + parent);
    }
    try (FileOutputStream out = new FileOutputStream(dest)) {
      byte[] buffer = new byte[8192];
      int len;
      while ((len = in.read(buffer)) != -1) {
        out.write(buffer, 0, len);
      }
    }
  }

  private void writeFile(File file, String content) throws IOException {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Failed to create folder: " + parent);
    }
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(content.getBytes(StandardCharsets.UTF_8));
    }
  }

  private String sanitizeThemeName(String name) {
    if (name == null || name.isEmpty()) return "MyApplication";
    String s = name.replaceAll("[^a-zA-Z0-9]", "");
    return s.isEmpty() ? "MyApplication" : s;
  }

  private String escapeXml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "\\'");
  }

  private String getDefaultWrapperProperties() {
    return "distributionBase=GRADLE_USER_HOME\n"
        + "distributionPath=wrapper/dists\n"
        + "distributionUrl=https\\://services.gradle.org/distributions/gradle-9.0.0-bin.zip\n"
        + "zipStoreBase=GRADLE_USER_HOME\n"
        + "zipStorePath=wrapper/dists\n";
  }

  private String getDefaultGradlewScript() {
    return "#!/bin/sh\n" + "exec gradle \"$@\"\n";
  }

  private String getDefaultGradlewBat() {
    return "@rem Gradle startup script for Windows\r\n" + "@gradle %*\r\n";
  }
}
