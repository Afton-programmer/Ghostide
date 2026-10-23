package ir.hanzodev1375.ghostide.project.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import ir.hanzodev1375.ghostide.project.DslType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AndroidProjectGenerator {

    public interface OnGenerateListener {
        void onSuccess(String projectPath);
        void onError(String message);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AndroidProjectGenerator(Context context) {
        this.context = context.getApplicationContext();
    }

    public void generateAsync(AndroidProjectConfig config, OnGenerateListener listener) {
        new Thread(() -> {
            try {
                String projectPath = generate(config);
                mainHandler.post(() -> listener.onSuccess(projectPath));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError(e.getMessage() != null ? e.getMessage() : e.toString()));
            }
        }).start();
    }

    public String generate(AndroidProjectConfig config) throws IOException {
        validateConfig(config);

        File root = new File(config.getParentPath(), config.getProjectName());
        if (root.exists()) {
            throw new IOException("A folder named \"" + config.getProjectName() + "\" already exists here.");
        }
        if (!root.mkdirs()) {
            throw new IOException("Failed to create project folder: " + root.getAbsolutePath());
        }

        boolean isKts = config.getDslType() == DslType.KOTLIN;
        boolean isKotlin = config.getLanguage() == AndroidProjectConfig.Language.KOTLIN;
        String pkg = config.getPackageName();
        String pkgPath = pkg.replace('.', '/');

        // 1. Create base project directories
        File gradleWrapperDir = new File(root, "gradle/wrapper");
        gradleWrapperDir.mkdirs();

        File appDir = new File(root, "app");
        appDir.mkdirs();

        String srcLangDir = isKotlin ? "kotlin" : "java";
        File srcMainPkg = new File(appDir, "src/main/" + srcLangDir + "/" + pkgPath);
        srcMainPkg.mkdirs();

        File resValues = new File(appDir, "src/main/res/values");
        File resValuesNight = new File(appDir, "src/main/res/values-night");
        File resLayout = new File(appDir, "src/main/res/layout");
        File resDrawable = new File(appDir, "src/main/res/drawable");
        File resMipmap = new File(appDir, "src/main/res/mipmap-anydpi-v26");
        resValues.mkdirs();
        resValuesNight.mkdirs();
        resLayout.mkdirs();
        resDrawable.mkdirs();
        resMipmap.mkdirs();

        // 2. Gradle Wrapper
        writeWrapperFiles(root, gradleWrapperDir);

        // 3. Root files
        writeRootBuildFiles(root, config, isKts);
        writeVersionCatalog(new File(root, "gradle/libs.versions.toml"));
        writeRootGitignore(new File(root, ".gitignore"));
        writeGradleProperties(new File(root, "gradle.properties"));
        writeKeystoreHelper(root);

        // 4. App module files
        writeAppBuildGradle(appDir, config, isKts, isKotlin);
        writeAppGitignore(new File(appDir, ".gitignore"));
        writeAppProguard(new File(appDir, "proguard-rules.pro"));
        writeAndroidManifest(appDir, config);

        // 5. Common resources
        writeCommonResources(appDir, config);

        // 6. Template specific code and resources
        writeTemplateArtifacts(appDir, srcMainPkg, config, isKotlin);

        return root.getAbsolutePath();
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
            throw new IOException("Invalid package name: \"" + pkg + "\". Must have at least two segments (e.g. com.example.app).");
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
        copyAssetOrFallback("templates/android/wrapper/gradlew", new File(root, "gradlew"), getDefaultGradlewScript());
        File gradlewFile = new File(root, "gradlew");
        gradlewFile.setExecutable(true, false);

        copyAssetOrFallback("templates/android/wrapper/gradlew.bat", new File(root, "gradlew.bat"), getDefaultGradlewBat());
        copyAssetOrFallback("templates/android/wrapper/gradle-wrapper.properties", new File(wrapperDir, "gradle-wrapper.properties"), getDefaultWrapperProperties());
        copyAssetBinary("templates/android/wrapper/gradle-wrapper.jar", new File(wrapperDir, "gradle-wrapper.jar"));
    }

    private void copyAssetOrFallback(String assetPath, File dest, String fallback) throws IOException {
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

    private void copyStream(InputStream in, File dest) throws IOException {
        try (FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    private void writeFile(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeRootBuildFiles(File root, AndroidProjectConfig config, boolean isKts) throws IOException {
        if (isKts) {
            writeFile(new File(root, "settings.gradle.kts"),
                    "pluginManagement {\n"
                            + "    repositories {\n"
                            + "        google {\n"
                            + "            content {\n"
                            + "                includeGroupByRegex(\"com\\\\.android.*\")\n"
                            + "                includeGroupByRegex(\"com\\\\.google.*\")\n"
                            + "                includeGroupByRegex(\"androidx.*\")\n"
                            + "            }\n"
                            + "        }\n"
                            + "        mavenCentral()\n"
                            + "        gradlePluginPortal()\n"
                            + "    }\n"
                            + "}\n"
                            + "dependencyResolutionManagement {\n"
                            + "    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n"
                            + "    repositories {\n"
                            + "        google()\n"
                            + "        mavenCentral()\n"
                            + "    }\n"
                            + "}\n\n"
                            + "rootProject.name = \"" + config.getProjectName().replace("\"", "\\\"") + "\"\n"
                            + "include(\":app\")\n");

            writeFile(new File(root, "build.gradle.kts"),
                    "// Top-level build file where you can add configuration options common to all sub-projects/modules.\n"
                            + "plugins {\n"
                            + "    alias(libs.plugins.android.application) apply false\n"
                            + "    alias(libs.plugins.kotlin.android) apply false\n"
                            + "}\n");
        } else {
            writeFile(new File(root, "settings.gradle"),
                    "pluginManagement {\n"
                            + "    repositories {\n"
                            + "        google {\n"
                            + "            content {\n"
                            + "                includeGroupByRegex(\"com\\\\.android.*\")\n"
                            + "                includeGroupByRegex(\"com\\\\.google.*\")\n"
                            + "                includeGroupByRegex(\"androidx.*\")\n"
                            + "            }\n"
                            + "        }\n"
                            + "        mavenCentral()\n"
                            + "        gradlePluginPortal()\n"
                            + "    }\n"
                            + "}\n"
                            + "dependencyResolutionManagement {\n"
                            + "    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n"
                            + "    repositories {\n"
                            + "        google()\n"
                            + "        mavenCentral()\n"
                            + "    }\n"
                            + "}\n\n"
                            + "rootProject.name = \"" + config.getProjectName().replace("\"", "\\\"") + "\"\n"
                            + "include ':app'\n");

            writeFile(new File(root, "build.gradle"),
                    "// Top-level build file where you can add configuration options common to all sub-projects/modules.\n"
                            + "plugins {\n"
                            + "    alias libs.plugins.android.application apply false\n"
                            + "    alias libs.plugins.kotlin.android apply false\n"
                            + "}\n");
        }
    }

    private void writeVersionCatalog(File toml) throws IOException {
        String content = "[versions]\n"
                + "agp = \"8.5.2\"\n"
                + "kotlin = \"1.9.24\"\n"
                + "coreKtx = \"1.13.1\"\n"
                + "junit = \"4.13.2\"\n"
                + "junitVersion = \"1.2.1\"\n"
                + "espressoCore = \"3.6.1\"\n"
                + "appcompat = \"1.7.0\"\n"
                + "material = \"1.12.0\"\n"
                + "activity = \"1.9.2\"\n"
                + "constraintlayout = \"2.1.4\"\n"
                + "navigation = \"2.8.0\"\n"
                + "lifecycleRuntimeKtx = \"2.8.5\"\n"
                + "activityCompose = \"1.9.2\"\n"
                + "composeBom = \"2024.09.00\"\n"
                + "wearCompose = \"1.4.0\"\n"
                + "wearWatchface = \"1.2.1\"\n"
                + "wearTiles = \"1.4.0\"\n"
                + "wearProtoLayout = \"1.2.0\"\n"
                + "leanback = \"1.0.0\"\n"
                + "tvFoundation = \"1.0.0-alpha10\"\n"
                + "tvMaterial = \"1.0.0-alpha10\"\n"
                + "carApp = \"1.4.0\"\n"
                + "media3 = \"1.4.1\"\n\n"
                + "[libraries]\n"
                + "androidx-core-ktx = { group = \"androidx.core\", name = \"core-ktx\", version.ref = \"coreKtx\" }\n"
                + "junit = { group = \"junit\", name = \"junit\", version.ref = \"junit\" }\n"
                + "androidx-junit = { group = \"androidx.test.ext\", name = \"junit\", version.ref = \"junitVersion\" }\n"
                + "androidx-espresso-core = { group = \"androidx.test.espresso\", name = \"espresso-core\", version.ref = \"espressoCore\" }\n"
                + "androidx-appcompat = { group = \"androidx.appcompat\", name = \"appcompat\", version.ref = \"appcompat\" }\n"
                + "material = { group = \"com.google.android.material\", name = \"material\", version.ref = \"material\" }\n"
                + "androidx-activity = { group = \"androidx.activity\", name = \"activity\", version.ref = \"activity\" }\n"
                + "androidx-constraintlayout = { group = \"androidx.constraintlayout\", name = \"constraintlayout\", version.ref = \"constraintlayout\" }\n"
                + "androidx-navigation-fragment-ktx = { group = \"androidx.navigation\", name = \"navigation-fragment-ktx\", version.ref = \"navigation\" }\n"
                + "androidx-navigation-ui-ktx = { group = \"androidx.navigation\", name = \"navigation-ui-ktx\", version.ref = \"navigation\" }\n"
                + "androidx-lifecycle-runtime-ktx = { group = \"androidx.lifecycle\", name = \"lifecycle-runtime-ktx\", version.ref = \"lifecycleRuntimeKtx\" }\n"
                + "androidx-activity-compose = { group = \"androidx.activity\", name = \"activity-compose\", version.ref = \"activityCompose\" }\n"
                + "androidx-compose-bom = { group = \"androidx.compose\", name = \"compose-bom\", version.ref = \"composeBom\" }\n"
                + "androidx-ui = { group = \"androidx.compose.ui\", name = \"ui\" }\n"
                + "androidx-ui-graphics = { group = \"androidx.compose.ui\", name = \"ui-graphics\" }\n"
                + "androidx-ui-tooling = { group = \"androidx.compose.ui\", name = \"ui-tooling\" }\n"
                + "androidx-ui-tooling-preview = { group = \"androidx.compose.ui\", name = \"ui-tooling-preview\" }\n"
                + "androidx-ui-test-manifest = { group = \"androidx.compose.ui\", name = \"ui-test-manifest\" }\n"
                + "androidx-ui-test-junit4 = { group = \"androidx.compose.ui\", name = \"ui-test-junit4\" }\n"
                + "androidx-material3 = { group = \"androidx.compose.material3\", name = \"material3\" }\n"
                + "androidx-wear-compose-material = { group = \"androidx.wear.compose\", name = \"compose-material\", version.ref = \"wearCompose\" }\n"
                + "androidx-wear-compose-foundation = { group = \"androidx.wear.compose\", name = \"compose-foundation\", version.ref = \"wearCompose\" }\n"
                + "androidx-wear-watchface = { group = \"androidx.wear.watchface\", name = \"watchface\", version.ref = \"wearWatchface\" }\n"
                + "androidx-wear-tiles = { group = \"androidx.wear.tiles\", name = \"tiles\", version.ref = \"wearTiles\" }\n"
                + "androidx-wear-protolayout = { group = \"androidx.wear.protolayout\", name = \"protolayout\", version.ref = \"wearProtoLayout\" }\n"
                + "androidx-leanback = { group = \"androidx.leanback\", name = \"leanback\", version.ref = \"leanback\" }\n"
                + "androidx-tv-foundation = { group = \"androidx.tv\", name = \"tv-foundation\", version.ref = \"tvFoundation\" }\n"
                + "androidx-tv-material = { group = \"androidx.tv\", name = \"tv-material\", version.ref = \"tvMaterial\" }\n"
                + "androidx-car-app = { group = \"androidx.car.app\", name = \"app\", version.ref = \"carApp\" }\n"
                + "androidx-media3-session = { group = \"androidx.media3\", name = \"media3-session\", version.ref = \"media3\" }\n\n"
                + "[plugins]\n"
                + "android-application = { id = \"com.android.application\", version.ref = \"agp\" }\n"
                + "kotlin-android = { id = \"org.jetbrains.kotlin.android\", version.ref = \"kotlin\" }\n";
        writeFile(toml, content);
    }

    private void writeRootGitignore(File file) throws IOException {
        String content = "*.iml\n.gradle\n/local.properties\n/.idea/\n.DS_Store\n/build\n/captures\n.externalNativeBuild\n.cxx\n";
        writeFile(file, content);
    }

    private void writeGradleProperties(File file) throws IOException {
        String content = "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\n"
                + "android.useAndroidX=true\n"
                + "android.nonTransitiveRClass=true\n"
                + "kotlin.code.style=official\n";
        writeFile(file, content);
    }

    private void writeKeystoreHelper(File root) throws IOException {
        File script = new File(root, "generate-keystore.sh");
        String content = "#!/bin/sh\n"
                + "# Ghostide Release Keystore Generator\n"
                + "echo \"Generating production release keystore (release.keystore)...\"\n"
                + "keytool -genkey -v -keystore release.keystore -alias androidreleasekey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname \"CN=Android App, OU=Mobile, O=Ghostide, L=City, S=State, C=US\"\n"
                + "if [ $? -eq 0 ]; then\n"
                + "  echo \"✅ Release keystore created successfully at release.keystore\"\n"
                + "  echo \"Alias: androidreleasekey | StorePass: android | KeyPass: android\"\n"
                + "else\n"
                + "  echo \"❌ Failed to generate keystore. Ensure keytool is installed.\"\n"
                + "fi\n";
        writeFile(script, content);
        script.setExecutable(true, false);
    }

    private void writeAppGitignore(File file) throws IOException {
        writeFile(file, "/build\n*.apk\n*.aab\n");
    }

    private void writeAppProguard(File file) throws IOException {
        writeFile(file, "# Add project specific ProGuard rules here.\n");
    }

    private void writeAppBuildGradle(File appDir, AndroidProjectConfig config, boolean isKts, boolean isKotlin) throws IOException {
        String fileName = isKts ? "build.gradle.kts" : "build.gradle";
        File file = new File(appDir, fileName);

        boolean isCompose = config.getTemplate().isSupportsCompose();
        boolean isNavigation = config.getTemplate() == AndroidTemplate.PHONE_BASIC_VIEWS
                || config.getTemplate() == AndroidTemplate.PHONE_BOTTOM_NAV
                || config.getTemplate() == AndroidTemplate.PHONE_NAVIGATION_DRAWER;
        boolean isViews = !isCompose && config.getTemplate() != AndroidTemplate.PHONE_NO_ACTIVITY;

        StringBuilder sb = new StringBuilder();
        if (isKts) {
            sb.append("plugins {\n");
            sb.append("    alias(libs.plugins.android.application)\n");
            if (isKotlin) {
                sb.append("    alias(libs.plugins.kotlin.android)\n");
            }
            sb.append("}\n\n");

            sb.append("android {\n");
            sb.append("    namespace = \"").append(config.getPackageName()).append("\"\n");
            sb.append("    compileSdk = ").append(config.getCompileSdk()).append("\n\n");

            sb.append("    defaultConfig {\n");
            sb.append("        applicationId = \"").append(config.getPackageName()).append("\"\n");
            sb.append("        minSdk = ").append(config.getMinSdk()).append("\n");
            sb.append("        targetSdk = ").append(config.getTargetSdk()).append("\n");
            sb.append("        versionCode = 1\n");
            sb.append("        versionName = \"1.0\"\n\n");
            sb.append("        testInstrumentationRunner = \"androidx.test.runner.AndroidJUnitRunner\"\n");
            sb.append("    }\n\n");

            sb.append("    signingConfigs {\n");
            sb.append("        create(\"release\") {\n");
            sb.append("            // Optional release signing config:\n");
            sb.append("            // storeFile = file(\"../release.keystore\")\n");
            sb.append("            // storePassword = \"android\"\n");
            sb.append("            // keyAlias = \"androidreleasekey\"\n");
            sb.append("            // keyPassword = \"android\"\n");
            sb.append("        }\n");
            sb.append("    }\n\n");

            sb.append("    buildTypes {\n");
            sb.append("        release {\n");
            sb.append("            isMinifyEnabled = false\n");
            sb.append("            proguardFiles(\n");
            sb.append("                getDefaultProguardFile(\"proguard-android-optimize.txt\"),\n");
            sb.append("                \"proguard-rules.pro\"\n");
            sb.append("            )\n");
            sb.append("        }\n");
            sb.append("    }\n");

            sb.append("    compileOptions {\n");
            sb.append("        sourceCompatibility = JavaVersion.VERSION_17\n");
            sb.append("        targetCompatibility = JavaVersion.VERSION_17\n");
            sb.append("    }\n");

            if (isKotlin) {
                sb.append("    kotlinOptions {\n");
                sb.append("        jvmTarget = \"17\"\n");
                sb.append("    }\n");
            }

            if (isCompose || isViews) {
                sb.append("    buildFeatures {\n");
                if (isViews) sb.append("        viewBinding = true\n");
                if (isCompose) sb.append("        compose = true\n");
                sb.append("    }\n");
            }

            if (isCompose) {
                sb.append("    composeOptions {\n");
                sb.append("        kotlinCompilerExtensionVersion = \"1.5.14\"\n");
                sb.append("    }\n");
            }
            sb.append("}\n\n");

            sb.append("dependencies {\n");
            sb.append("    implementation(libs.androidx.core.ktx)\n");
            sb.append("    implementation(libs.androidx.appcompat)\n");
            sb.append("    implementation(libs.material)\n");
            sb.append("    implementation(libs.androidx.activity)\n");

            if (isCompose) {
                sb.append("    implementation(libs.androidx.lifecycle.runtime.ktx)\n");
                sb.append("    implementation(libs.androidx.activity.compose)\n");
                sb.append("    implementation(platform(libs.androidx.compose.bom))\n");
                sb.append("    implementation(libs.androidx.ui)\n");
                sb.append("    implementation(libs.androidx.ui.graphics)\n");
                sb.append("    implementation(libs.androidx.ui.tooling.preview)\n");
                sb.append("    implementation(libs.androidx.material3)\n");
                sb.append("    debugImplementation(libs.androidx.ui.tooling)\n");
                sb.append("    debugImplementation(libs.androidx.ui.test.manifest)\n");
            }

            if (isViews) {
                sb.append("    implementation(libs.androidx.constraintlayout)\n");
            }
            if (isNavigation) {
                sb.append("    implementation(libs.androidx.navigation.fragment.ktx)\n");
                sb.append("    implementation(libs.androidx.navigation.ui.ktx)\n");
            }
            if (config.getCategory() == AndroidCategory.WEAR_OS) {
                sb.append("    implementation(libs.androidx.wear.compose.material)\n");
                sb.append("    implementation(libs.androidx.wear.compose.foundation)\n");
                sb.append("    implementation(libs.androidx.wear.watchface)\n");
                sb.append("    implementation(libs.androidx.wear.tiles)\n");
                sb.append("    implementation(libs.androidx.wear.protolayout)\n");
            } else if (config.getCategory() == AndroidCategory.ANDROID_TV) {
                sb.append("    implementation(libs.androidx.leanback)\n");
                sb.append("    implementation(libs.androidx.tv.foundation)\n");
                sb.append("    implementation(libs.androidx.tv.material)\n");
            } else if (config.getCategory() == AndroidCategory.AUTOMOTIVE) {
                sb.append("    implementation(libs.androidx.car.app)\n");
                sb.append("    implementation(libs.androidx.media3.session)\n");
            }

            sb.append("    testImplementation(libs.junit)\n");
            sb.append("    androidTestImplementation(libs.androidx.junit)\n");
            sb.append("    androidTestImplementation(libs.androidx.espresso.core)\n");
            sb.append("}\n");
        } else {
            // Groovy DSL
            sb.append("plugins {\n");
            sb.append("    alias libs.plugins.android.application\n");
            if (isKotlin) {
                sb.append("    alias libs.plugins.kotlin.android\n");
            }
            sb.append("}\n\n");

            sb.append("android {\n");
            sb.append("    namespace '").append(config.getPackageName()).append("'\n");
            sb.append("    compileSdk ").append(config.getCompileSdk()).append("\n\n");

            sb.append("    defaultConfig {\n");
            sb.append("        applicationId \"").append(config.getPackageName()).append("\"\n");
            sb.append("        minSdk ").append(config.getMinSdk()).append("\n");
            sb.append("        targetSdk ").append(config.getTargetSdk()).append("\n");
            sb.append("        versionCode 1\n");
            sb.append("        versionName \"1.0\"\n\n");
            sb.append("        testInstrumentationRunner \"androidx.test.runner.AndroidJUnitRunner\"\n");
            sb.append("    }\n\n");

            sb.append("    signingConfigs {\n");
            sb.append("        release {\n");
            sb.append("            // Optional release signing config:\n");
            sb.append("            // storeFile file('../release.keystore')\n");
            sb.append("            // storePassword 'android'\n");
            sb.append("            // keyAlias 'androidreleasekey'\n");
            sb.append("            // keyPassword 'android'\n");
            sb.append("        }\n");
            sb.append("    }\n\n");

            sb.append("    buildTypes {\n");
            sb.append("        release {\n");
            sb.append("            minifyEnabled false\n");
            sb.append("            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'\n");
            sb.append("        }\n");
            sb.append("    }\n");

            sb.append("    compileOptions {\n");
            sb.append("        sourceCompatibility JavaVersion.VERSION_17\n");
            sb.append("        targetCompatibility JavaVersion.VERSION_17\n");
            sb.append("    }\n");

            if (isKotlin) {
                sb.append("    kotlinOptions {\n");
                sb.append("        jvmTarget = '17'\n");
                sb.append("    }\n");
            }

            if (isCompose || isViews) {
                sb.append("    buildFeatures {\n");
                if (isViews) sb.append("        viewBinding true\n");
                if (isCompose) sb.append("        compose true\n");
                sb.append("    }\n");
            }

            if (isCompose) {
                sb.append("    composeOptions {\n");
                sb.append("        kotlinCompilerExtensionVersion = '1.5.14'\n");
                sb.append("    }\n");
            }
            sb.append("}\n\n");

            sb.append("dependencies {\n");
            sb.append("    implementation libs.androidx.core.ktx\n");
            sb.append("    implementation libs.androidx.appcompat\n");
            sb.append("    implementation libs.material\n");
            sb.append("    implementation libs.androidx.activity\n");

            if (isCompose) {
                sb.append("    implementation libs.androidx.lifecycle.runtime.ktx\n");
                sb.append("    implementation libs.androidx.activity.compose\n");
                sb.append("    implementation platform(libs.androidx.compose.bom)\n");
                sb.append("    implementation libs.androidx.ui\n");
                sb.append("    implementation libs.androidx.ui.graphics\n");
                sb.append("    implementation libs.androidx.ui.tooling.preview\n");
                sb.append("    implementation libs.androidx.material3\n");
                sb.append("    debugImplementation libs.androidx.ui.tooling\n");
                sb.append("    debugImplementation libs.androidx.ui.test.manifest\n");
            }

            if (isViews) {
                sb.append("    implementation libs.androidx.constraintlayout\n");
            }
            if (isNavigation) {
                sb.append("    implementation libs.androidx.navigation.fragment.ktx\n");
                sb.append("    implementation libs.androidx.navigation.ui.ktx\n");
            }
            if (config.getCategory() == AndroidCategory.WEAR_OS) {
                sb.append("    implementation libs.androidx.wear.compose.material\n");
                sb.append("    implementation libs.androidx.wear.compose.foundation\n");
                sb.append("    implementation libs.androidx.wear.watchface\n");
                sb.append("    implementation libs.androidx.wear.tiles\n");
                sb.append("    implementation libs.androidx.wear.protolayout\n");
            } else if (config.getCategory() == AndroidCategory.ANDROID_TV) {
                sb.append("    implementation libs.androidx.leanback\n");
                sb.append("    implementation libs.androidx.tv.foundation\n");
                sb.append("    implementation libs.androidx.tv.material\n");
            } else if (config.getCategory() == AndroidCategory.AUTOMOTIVE) {
                sb.append("    implementation libs.androidx.car.app\n");
                sb.append("    implementation libs.androidx.media3.session\n");
            }

            sb.append("    testImplementation libs.junit\n");
            sb.append("    androidTestImplementation libs.androidx.junit\n");
            sb.append("    androidTestImplementation libs.androidx.espresso.core\n");
            sb.append("}\n");
        }

        writeFile(file, sb.toString());
    }

    private void writeAndroidManifest(File appDir, AndroidProjectConfig config) throws IOException {
        File manifest = new File(appDir, "src/main/AndroidManifest.xml");
        AndroidTemplate t = config.getTemplate();
        AndroidCategory cat = config.getCategory();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n\n");

        if (cat == AndroidCategory.WEAR_OS) {
            sb.append("    <uses-feature android:name=\"android.hardware.type.watch\" />\n\n");
        } else if (cat == AndroidCategory.ANDROID_TV) {
            sb.append("    <uses-feature android:name=\"android.software.leanback\" android:required=\"false\" />\n");
            sb.append("    <uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />\n\n");
        } else if (cat == AndroidCategory.AUTOMOTIVE) {
            sb.append("    <uses-feature android:name=\"android.hardware.type.automotive\" android:required=\"true\" />\n\n");
        }

        sb.append("    <application\n");
        sb.append("        android:allowBackup=\"true\"\n");
        sb.append("        android:icon=\"@mipmap/ic_launcher\"\n");
        sb.append("        android:label=\"@string/app_name\"\n");
        sb.append("        android:roundIcon=\"@mipmap/ic_launcher_round\"\n");
        sb.append("        android:supportsRtl=\"true\"\n");
        sb.append("        android:theme=\"@style/Theme.").append(sanitizeThemeName(config.getProjectName())).append("\">\n\n");

        if (t == AndroidTemplate.WEAR_WATCH_FACE) {
            sb.append("        <service\n");
            sb.append("            android:name=\".MyWatchFaceService\"\n");
            sb.append("            android:exported=\"true\"\n");
            sb.append("            android:label=\"@string/app_name\"\n");
            sb.append("            android:permission=\"android.permission.BIND_WALLPAPER\">\n");
            sb.append("            <intent-filter>\n");
            sb.append("                <action android:name=\"android.service.wallpaper.WallpaperService\" />\n");
            sb.append("                <category android:name=\"com.google.android.wearable.watchface.category.WATCH_FACE\" />\n");
            sb.append("            </intent-filter>\n");
            sb.append("        </service>\n");
        } else if (t == AndroidTemplate.WEAR_TILE_SERVICE) {
            sb.append("        <service\n");
            sb.append("            android:name=\".MyTileService\"\n");
            sb.append("            android:exported=\"true\"\n");
            sb.append("            android:label=\"@string/app_name\"\n");
            sb.append("            android:permission=\"com.google.android.wearable.permission.BIND_TILE_PROVIDER\">\n");
            sb.append("            <intent-filter>\n");
            sb.append("                <action android:name=\"androidx.wear.tiles.action.BIND_UPDATE_REQUESTER\" />\n");
            sb.append("            </intent-filter>\n");
            sb.append("        </service>\n");
        } else if (t == AndroidTemplate.AUTO_CAR_APP) {
            sb.append("        <service\n");
            sb.append("            android:name=\".MyCarAppService\"\n");
            sb.append("            android:exported=\"true\">\n");
            sb.append("            <intent-filter>\n");
            sb.append("                <action android:name=\"androidx.car.app.CarAppService\" />\n");
            sb.append("                <category android:name=\"androidx.car.app.category.POI\" />\n");
            sb.append("            </intent-filter>\n");
            sb.append("        </service>\n");
            sb.append("        <meta-data\n");
            sb.append("            android:name=\"androidx.car.app.minCarApiLevel\"\n");
            sb.append("            android:value=\"1\" />\n");
        } else if (t == AndroidTemplate.AUTO_MEDIA_APP) {
            sb.append("        <service\n");
            sb.append("            android:name=\".MyMediaPlaybackService\"\n");
            sb.append("            android:exported=\"true\">\n");
            sb.append("            <intent-filter>\n");
            sb.append("                <action android:name=\"androidx.media3.session.MediaSessionService\" />\n");
            sb.append("            </intent-filter>\n");
            sb.append("        </service>\n");
        } else if (t != AndroidTemplate.PHONE_NO_ACTIVITY) {
            sb.append("        <activity\n");
            sb.append("            android:name=\".MainActivity\"\n");
            sb.append("            android:exported=\"true\">\n");
            sb.append("            <intent-filter>\n");
            sb.append("                <action android:name=\"android.intent.action.MAIN\" />\n");
            if (cat == AndroidCategory.ANDROID_TV) {
                sb.append("                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\" />\n");
            } else {
                sb.append("                <category android:name=\"android.intent.category.LAUNCHER\" />\n");
            }
            sb.append("            </intent-filter>\n");
            sb.append("        </activity>\n");
        }

        sb.append("    </application>\n");
        sb.append("</manifest>\n");

        writeFile(manifest, sb.toString());
    }

    private void writeCommonResources(File appDir, AndroidProjectConfig config) throws IOException {
        String themeName = "Theme." + sanitizeThemeName(config.getProjectName());

        // strings.xml
        writeFile(new File(appDir, "src/main/res/values/strings.xml"),
                "<resources>\n"
                        + "    <string name=\"app_name\">" + escapeXml(config.getProjectName()) + "</string>\n"
                        + "    <string name=\"hello_world\">Hello " + escapeXml(config.getProjectName()) + "!</string>\n"
                        + "    <string name=\"action_settings\">Settings</string>\n"
                        + "    <string name=\"title_home\">Home</string>\n"
                        + "    <string name=\"title_dashboard\">Dashboard</string>\n"
                        + "    <string name=\"title_notifications\">Notifications</string>\n"
                        + "</resources>\n");

        // colors.xml
        writeFile(new File(appDir, "src/main/res/values/colors.xml"),
                "<resources>\n"
                        + "    <color name=\"purple_200\">#FFBB86FC</color>\n"
                        + "    <color name=\"purple_500\">#FF6200EE</color>\n"
                        + "    <color name=\"purple_700\">#FF3700B3</color>\n"
                        + "    <color name=\"teal_200\">#FF03DAC5</color>\n"
                        + "    <color name=\"teal_700\">#FF018786</color>\n"
                        + "    <color name=\"black\">#FF000000</color>\n"
                        + "    <color name=\"white\">#FFFFFFFF</color>\n"
                        + "</resources>\n");

        // themes.xml
        writeFile(new File(appDir, "src/main/res/values/themes.xml"),
                "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <style name=\"" + themeName + "\" parent=\"Theme.Material3.DayNight.NoActionBar\">\n"
                        + "        <item name=\"colorPrimary\">@color/purple_500</item>\n"
                        + "        <item name=\"colorSecondary\">@color/teal_200</item>\n"
                        + "    </style>\n"
                        + "</resources>\n");

        // values-night/themes.xml
        writeFile(new File(appDir, "src/main/res/values-night/themes.xml"),
                "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <style name=\"" + themeName + "\" parent=\"Theme.Material3.DayNight.NoActionBar\">\n"
                        + "        <item name=\"colorPrimary\">@color/purple_200</item>\n"
                        + "        <item name=\"colorSecondary\">@color/teal_200</item>\n"
                        + "    </style>\n"
                        + "</resources>\n");

        // Launcher icons
        writeFile(new File(appDir, "src/main/res/drawable/ic_launcher_background.xml"),
                "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:width=\"108dp\"\n"
                        + "    android:height=\"108dp\"\n"
                        + "    android:viewportWidth=\"108\"\n"
                        + "    android:viewportHeight=\"108\">\n"
                        + "    <path\n"
                        + "        android:fillColor=\"#3DDC84\"\n"
                        + "        android:pathData=\"M0,0h108v108h-108z\"/>\n"
                        + "</vector>\n");

        writeFile(new File(appDir, "src/main/res/drawable/ic_launcher_foreground.xml"),
                "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:width=\"108dp\"\n"
                        + "    android:height=\"108dp\"\n"
                        + "    android:viewportWidth=\"108\"\n"
                        + "    android:viewportHeight=\"108\">\n"
                        + "    <path\n"
                        + "        android:fillColor=\"#FFFFFF\"\n"
                        + "        android:pathData=\"M31,64c0,1.1 0.9,2 2,2h4v8c0,1.66 1.34,3 3,3s3,-1.34 3,-3v-8h22v8c0,1.66 1.34,3 3,3s3,-1.34 3,-3v-8h4c1.1,0 2,-0.9 2,-2V42H31v22zM24,42c-1.66,0 -3,1.34 -3,3v14c0,1.66 1.34,3 3,3s3,-1.34 3,-3V45c0,-1.66 -1.34,-3 -3,-3zm60,0c-1.66,0 -3,1.34 -3,3v14c0,1.66 1.34,3 3,3s3,-1.34 3,-3V45c0,-1.66 -1.34,-3 -3,-3zM69.07,26.31l3.52,-3.52c0.39,-0.39 0.39,-1.02 0,-1.41 -0.39,-0.39 -1.02,-0.39 -1.41,0l-4.01,4.01C63.78,24.08 59.99,23.5 54,23.5c-5.99,0 -9.78,0.58 -13.17,1.89l-4.01,-4.01c-0.39,-0.39 -1.02,-0.39 -1.41,0 -0.39,0.39 -0.39,1.02 0,1.41l3.52,3.52C34.42,28.87 31,34.85 31,40h46c0,-5.15 -3.42,-11.13 -7.93,-13.69zM45,34h-2v-2h2v2zm18,0h-2v-2h2v2z\"/>\n"
                        + "</vector>\n");

        writeFile(new File(appDir, "src/main/res/mipmap-anydpi-v26/ic_launcher.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <background android:drawable=\"@drawable/ic_launcher_background\" />\n"
                        + "    <foreground android:drawable=\"@drawable/ic_launcher_foreground\" />\n"
                        + "</adaptive-icon>\n");

        writeFile(new File(appDir, "src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <background android:drawable=\"@drawable/ic_launcher_background\" />\n"
                        + "    <foreground android:drawable=\"@drawable/ic_launcher_foreground\" />\n"
                        + "</adaptive-icon>\n");
    }

    private void writeTemplateArtifacts(File appDir, File srcPkg, AndroidProjectConfig config, boolean isKotlin) throws IOException {
        AndroidTemplate t = config.getTemplate();
        String pkg = config.getPackageName();
        String ext = isKotlin ? ".kt" : ".java";

        switch (t) {
            case PHONE_EMPTY_COMPOSE:
                writeComposeTemplate(srcPkg, config);
                break;

            case PHONE_EMPTY_VIEWS:
                writeEmptyViewsTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case PHONE_BASIC_VIEWS:
                writeBasicViewsTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case PHONE_BOTTOM_NAV:
                writeBottomNavTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case PHONE_NAVIGATION_DRAWER:
                writeNavigationDrawerTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case PHONE_TABBED:
                writeTabbedTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case PHONE_FULLSCREEN:
                writeFullscreenTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case WEAR_BLANK_VIEWS:
                writeWearViewsTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case WEAR_COMPOSE:
                writeWearComposeTemplate(srcPkg, config);
                break;

            case WEAR_WATCH_FACE:
                writeWearWatchFaceTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case WEAR_TILE_SERVICE:
                writeWearTileTemplate(srcPkg, pkg, isKotlin);
                break;

            case TV_LEANBACK:
                writeTvLeanbackTemplate(appDir, srcPkg, pkg, isKotlin);
                break;

            case TV_COMPOSE:
                writeTvComposeTemplate(srcPkg, config);
                break;

            case AUTO_CAR_APP:
                writeAutoCarAppTemplate(srcPkg, pkg, isKotlin);
                break;

            case AUTO_MEDIA_APP:
                writeAutoMediaTemplate(srcPkg, pkg, isKotlin);
                break;

            case PHONE_NO_ACTIVITY:
            default:
                break;
        }
    }

    private void writeEmptyViewsTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        // Layout
        writeFile(new File(appDir, "src/main/res/layout/activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<androidx.constraintlayout.widget.ConstraintLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:id=\"@+id/main\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n\n"
                        + "    <TextView\n"
                        + "        android:id=\"@+id/tvMessage\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello_world\"\n"
                        + "        android:textAppearance=\"?textAppearanceHeadlineMedium\"\n"
                        + "        app:layout_constraintBottom_toBottomOf=\"parent\"\n"
                        + "        app:layout_constraintEnd_toEndOf=\"parent\"\n"
                        + "        app:layout_constraintStart_toStartOf=\"parent\"\n"
                        + "        app:layout_constraintTop_toTopOf=\"parent\" />\n\n"
                        + "</androidx.constraintlayout.widget.ConstraintLayout>\n");

        if (isKotlin) {
            writeFile(new File(srcPkg, "MainActivity.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import androidx.appcompat.app.AppCompatActivity\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding\n\n"
                            + "class MainActivity : AppCompatActivity() {\n\n"
                            + "    private lateinit var binding: ActivityMainBinding\n\n"
                            + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                            + "        super.onCreate(savedInstanceState)\n"
                            + "        binding = ActivityMainBinding.inflate(layoutInflater)\n"
                            + "        setContentView(binding.root)\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MainActivity.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import androidx.appcompat.app.AppCompatActivity;\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding;\n\n"
                            + "public class MainActivity extends AppCompatActivity {\n\n"
                            + "    private ActivityMainBinding binding;\n\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        binding = ActivityMainBinding.inflate(getLayoutInflater());\n"
                            + "        setContentView(binding.getRoot());\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeComposeTemplate(File srcPkg, AndroidProjectConfig config) throws IOException {
        String pkg = config.getPackageName();
        File uiTheme = new File(srcPkg, "ui/theme");
        uiTheme.mkdirs();

        // Color.kt
        writeFile(new File(uiTheme, "Color.kt"),
                "package " + pkg + ".ui.theme\n\n"
                        + "import androidx.compose.ui.graphics.Color\n\n"
                        + "val Purple80 = Color(0xFFD0BCFF)\n"
                        + "val PurpleGrey80 = Color(0xFFCCC2DC)\n"
                        + "val Pink80 = Color(0xFFEFB8C8)\n\n"
                        + "val Purple40 = Color(0xFF6650a4)\n"
                        + "val PurpleGrey40 = Color(0xFF625b71)\n"
                        + "val Pink40 = Color(0xFF7D5260)\n");

        // Type.kt
        writeFile(new File(uiTheme, "Type.kt"),
                "package " + pkg + ".ui.theme\n\n"
                        + "import androidx.compose.material3.Typography\n"
                        + "import androidx.compose.ui.text.TextStyle\n"
                        + "import androidx.compose.ui.text.font.FontFamily\n"
                        + "import androidx.compose.ui.text.font.FontWeight\n"
                        + "import androidx.compose.ui.unit.sp\n\n"
                        + "val Typography = Typography(\n"
                        + "    bodyLarge = TextStyle(\n"
                        + "        fontFamily = FontFamily.Default,\n"
                        + "        fontWeight = FontWeight.Normal,\n"
                        + "        fontSize = 16.sp,\n"
                        + "        lineHeight = 24.sp,\n"
                        + "        letterSpacing = 0.5.sp\n"
                        + "    )\n"
                        + ")\n");

        // Theme.kt
        String appNameSanitized = sanitizeThemeName(config.getProjectName());
        writeFile(new File(uiTheme, "Theme.kt"),
                "package " + pkg + ".ui.theme\n\n"
                        + "import android.app.Activity\n"
                        + "import android.os.Build\n"
                        + "import androidx.compose.foundation.isSystemInDarkTheme\n"
                        + "import androidx.compose.material3.MaterialTheme\n"
                        + "import androidx.compose.material3.darkColorScheme\n"
                        + "import androidx.compose.material3.dynamicDarkColorScheme\n"
                        + "import androidx.compose.material3.dynamicLightColorScheme\n"
                        + "import androidx.compose.material3.lightColorScheme\n"
                        + "import androidx.compose.runtime.Composable\n"
                        + "import androidx.compose.ui.platform.LocalContext\n\n"
                        + "private val DarkColorScheme = darkColorScheme(\n"
                        + "    primary = Purple80,\n"
                        + "    secondary = PurpleGrey80,\n"
                        + "    tertiary = Pink80\n"
                        + ")\n\n"
                        + "private val LightColorScheme = lightColorScheme(\n"
                        + "    primary = Purple40,\n"
                        + "    secondary = PurpleGrey40,\n"
                        + "    tertiary = Pink40\n"
                        + ")\n\n"
                        + "@Composable\n"
                        + "fun " + appNameSanitized + "Theme(\n"
                        + "    darkTheme: Boolean = isSystemInDarkTheme(),\n"
                        + "    dynamicColor: Boolean = true,\n"
                        + "    content: @Composable () -> Unit\n"
                        + ") {\n"
                        + "    val colorScheme = when {\n"
                        + "        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {\n"
                        + "            val context = LocalContext.current\n"
                        + "            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)\n"
                        + "        }\n"
                        + "        darkTheme -> DarkColorScheme\n"
                        + "        else -> LightColorScheme\n"
                        + "    }\n\n"
                        + "    MaterialTheme(\n"
                        + "        colorScheme = colorScheme,\n"
                        + "        typography = Typography,\n"
                        + "        content = content\n"
                        + "    )\n"
                        + "}\n");

        // MainActivity.kt
        writeFile(new File(srcPkg, "MainActivity.kt"),
                "package " + pkg + "\n\n"
                        + "import android.os.Bundle\n"
                        + "import androidx.activity.ComponentActivity\n"
                        + "import androidx.activity.compose.setContent\n"
                        + "import androidx.compose.foundation.layout.fillMaxSize\n"
                        + "import androidx.compose.foundation.layout.padding\n"
                        + "import androidx.compose.material3.MaterialTheme\n"
                        + "import androidx.compose.material3.Scaffold\n"
                        + "import androidx.compose.material3.Surface\n"
                        + "import androidx.compose.material3.Text\n"
                        + "import androidx.compose.runtime.Composable\n"
                        + "import androidx.compose.ui.Modifier\n"
                        + "import androidx.compose.ui.tooling.preview.Preview\n"
                        + "import " + pkg + ".ui.theme." + appNameSanitized + "Theme\n\n"
                        + "class MainActivity : ComponentActivity() {\n"
                        + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                        + "        super.onCreate(savedInstanceState)\n"
                        + "        setContent {\n"
                        + "            " + appNameSanitized + "Theme {\n"
                        + "                Scaffold(modifier = Modifier.fillMaxSize()) {\n"
                        + "                    Greeting(\n"
                        + "                        name = \"Android\",\n"
                        + "                        modifier = Modifier.padding(it)\n"
                        + "                    )\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n\n"
                        + "@Composable\n"
                        + "fun Greeting(name: String, modifier: Modifier = Modifier) {\n"
                        + "    Text(\n"
                        + "        text = \"Hello $name!\",\n"
                        + "        modifier = modifier\n"
                        + "    )\n"
                        + "}\n\n"
                        + "@Preview(showBackground = true)\n"
                        + "@Composable\n"
                        + "fun GreetingPreview() {\n"
                        + "    " + appNameSanitized + "Theme {\n"
                        + "        Greeting(\"Android\")\n"
                        + "    }\n"
                        + "}\n");
    }

    private void writeBasicViewsTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        File navDir = new File(appDir, "src/main/res/navigation");
        navDir.mkdirs();

        // nav_graph.xml
        writeFile(new File(navDir, "nav_graph.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<navigation xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:id=\"@+id/nav_graph\"\n"
                        + "    app:startDestination=\"@+id/FirstFragment\">\n\n"
                        + "    <fragment\n"
                        + "        android:id=\"@+id/FirstFragment\"\n"
                        + "        android:name=\"" + pkg + ".FirstFragment\"\n"
                        + "        android:label=\"First Fragment\" />\n"
                        + "</navigation>\n");

        // Layouts
        writeFile(new File(appDir, "src/main/res/layout/activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n\n"
                        + "    <com.google.android.material.appbar.AppBarLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\">\n"
                        + "        <com.google.android.material.appbar.MaterialToolbar\n"
                        + "            android:id=\"@+id/toolbar\"\n"
                        + "            android:layout_width=\"match_parent\"\n"
                        + "            android:layout_height=\"?attr/actionBarSize\"\n"
                        + "            app:title=\"@string/app_name\" />\n"
                        + "    </com.google.android.material.appbar.AppBarLayout>\n\n"
                        + "    <androidx.fragment.app.FragmentContainerView\n"
                        + "        android:id=\"@+id/nav_host_fragment_content_main\"\n"
                        + "        android:name=\"androidx.navigation.fragment.NavHostFragment\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        app:defaultNavHost=\"true\"\n"
                        + "        app:layout_behavior=\"@string/appbar_scrolling_view_behavior\"\n"
                        + "        app:navGraph=\"@navigation/nav_graph\" />\n\n"
                        + "    <com.google.android.material.floatingactionbutton.FloatingActionButton\n"
                        + "        android:id=\"@+id/fab\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_gravity=\"bottom|end\"\n"
                        + "        android:layout_margin=\"16dp\"\n"
                        + "        android:contentDescription=\"FAB\"\n"
                        + "        app:srcCompat=\"@android:drawable/ic_dialog_email\" />\n"
                        + "</androidx.coordinatorlayout.widget.CoordinatorLayout>\n");

        writeFile(new File(appDir, "src/main/res/layout/fragment_first.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<androidx.constraintlayout.widget.ConstraintLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"Hello First Fragment\"\n"
                        + "        android:textAppearance=\"?textAppearanceHeadlineSmall\"\n"
                        + "        app:layout_constraintBottom_toBottomOf=\"parent\"\n"
                        + "        app:layout_constraintEnd_toEndOf=\"parent\"\n"
                        + "        app:layout_constraintStart_toStartOf=\"parent\"\n"
                        + "        app:layout_constraintTop_toTopOf=\"parent\" />\n"
                        + "</androidx.constraintlayout.widget.ConstraintLayout>\n");

        // Code
        if (isKotlin) {
            writeFile(new File(srcPkg, "MainActivity.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import androidx.appcompat.app.AppCompatActivity\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding\n\n"
                            + "class MainActivity : AppCompatActivity() {\n"
                            + "    private lateinit var binding: ActivityMainBinding\n\n"
                            + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                            + "        super.onCreate(savedInstanceState)\n"
                            + "        binding = ActivityMainBinding.inflate(layoutInflater)\n"
                            + "        setContentView(binding.root)\n"
                            + "        setSupportActionBar(binding.toolbar)\n"
                            + "    }\n"
                            + "}\n");

            writeFile(new File(srcPkg, "FirstFragment.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import android.view.LayoutInflater\n"
                            + "import android.view.View\n"
                            + "import android.view.ViewGroup\n"
                            + "import androidx.fragment.app.Fragment\n"
                            + "import " + pkg + ".databinding.FragmentFirstBinding\n\n"
                            + "class FirstFragment : Fragment() {\n"
                            + "    private var _binding: FragmentFirstBinding? = null\n"
                            + "    private val binding get() = _binding!!\n\n"
                            + "    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {\n"
                            + "        _binding = FragmentFirstBinding.inflate(inflater, container, false)\n"
                            + "        return binding.root\n"
                            + "    }\n\n"
                            + "    override fun onDestroyView() {\n"
                            + "        super.onDestroyView()\n"
                            + "        _binding = null\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MainActivity.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import androidx.appcompat.app.AppCompatActivity;\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding;\n\n"
                            + "public class MainActivity extends AppCompatActivity {\n"
                            + "    private ActivityMainBinding binding;\n\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        binding = ActivityMainBinding.inflate(getLayoutInflater());\n"
                            + "        setContentView(binding.getRoot());\n"
                            + "        setSupportActionBar(binding.toolbar);\n"
                            + "    }\n"
                            + "}\n");

            writeFile(new File(srcPkg, "FirstFragment.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import android.view.LayoutInflater;\n"
                            + "import android.view.View;\n"
                            + "import android.view.ViewGroup;\n"
                            + "import androidx.annotation.NonNull;\n"
                            + "import androidx.fragment.app.Fragment;\n"
                            + "import " + pkg + ".databinding.FragmentFirstBinding;\n\n"
                            + "public class FirstFragment extends Fragment {\n"
                            + "    private FragmentFirstBinding binding;\n\n"
                            + "    @Override\n"
                            + "    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {\n"
                            + "        binding = FragmentFirstBinding.inflate(inflater, container, false);\n"
                            + "        return binding.getRoot();\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeBottomNavTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        File menuDir = new File(appDir, "src/main/res/menu");
        File navDir = new File(appDir, "src/main/res/navigation");
        menuDir.mkdirs();
        navDir.mkdirs();

        // Menu
        writeFile(new File(menuDir, "bottom_nav_menu.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <item\n"
                        + "        android:id=\"@+id/navigation_home\"\n"
                        + "        android:icon=\"@android:drawable/ic_menu_agenda\"\n"
                        + "        android:title=\"@string/title_home\" />\n"
                        + "    <item\n"
                        + "        android:id=\"@+id/navigation_dashboard\"\n"
                        + "        android:icon=\"@android:drawable/ic_menu_compass\"\n"
                        + "        android:title=\"@string/title_dashboard\" />\n"
                        + "    <item\n"
                        + "        android:id=\"@+id/navigation_notifications\"\n"
                        + "        android:icon=\"@android:drawable/ic_popup_reminder\"\n"
                        + "        android:title=\"@string/title_notifications\" />\n"
                        + "</menu>\n");

        // Navigation
        writeFile(new File(navDir, "mobile_navigation.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<navigation xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:id=\"@+id/mobile_navigation\"\n"
                        + "    app:startDestination=\"@+id/navigation_home\">\n"
                        + "    <fragment\n"
                        + "        android:id=\"@+id/navigation_home\"\n"
                        + "        android:name=\"" + pkg + ".ui.home.HomeFragment\"\n"
                        + "        android:label=\"@string/title_home\" />\n"
                        + "    <fragment\n"
                        + "        android:id=\"@+id/navigation_dashboard\"\n"
                        + "        android:name=\"" + pkg + ".ui.dashboard.DashboardFragment\"\n"
                        + "        android:label=\"@string/title_dashboard\" />\n"
                        + "    <fragment\n"
                        + "        android:id=\"@+id/navigation_notifications\"\n"
                        + "        android:name=\"" + pkg + ".ui.notifications.NotificationsFragment\"\n"
                        + "        android:label=\"@string/title_notifications\" />\n"
                        + "</navigation>\n");

        // Layouts
        writeFile(new File(appDir, "src/main/res/layout/activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<androidx.constraintlayout.widget.ConstraintLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:id=\"@+id/container\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n\n"
                        + "    <com.google.android.material.bottomnavigation.BottomNavigationView\n"
                        + "        android:id=\"@+id/nav_view\"\n"
                        + "        android:layout_width=\"0dp\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        app:layout_constraintBottom_toBottomOf=\"parent\"\n"
                        + "        app:layout_constraintLeft_toLeftOf=\"parent\"\n"
                        + "        app:layout_constraintRight_toRightOf=\"parent\"\n"
                        + "        app:menu=\"@menu/bottom_nav_menu\" />\n\n"
                        + "    <androidx.fragment.app.FragmentContainerView\n"
                        + "        android:id=\"@+id/nav_host_fragment_activity_main\"\n"
                        + "        android:name=\"androidx.navigation.fragment.NavHostFragment\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"0dp\"\n"
                        + "        app:defaultNavHost=\"true\"\n"
                        + "        app:layout_constraintBottom_toTopOf=\"@+id/nav_view\"\n"
                        + "        app:layout_constraintLeft_toLeftOf=\"parent\"\n"
                        + "        app:layout_constraintRight_toRightOf=\"parent\"\n"
                        + "        app:layout_constraintTop_toTopOf=\"parent\"\n"
                        + "        app:navGraph=\"@navigation/mobile_navigation\" />\n"
                        + "</androidx.constraintlayout.widget.ConstraintLayout>\n");

        writeFile(new File(appDir, "src/main/res/layout/fragment_home.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:id=\"@+id/text_home\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:gravity=\"center\"\n"
                        + "    android:text=\"Home Fragment\"\n"
                        + "    android:textAppearance=\"?textAppearanceHeadlineMedium\" />\n");

        writeFile(new File(appDir, "src/main/res/layout/fragment_dashboard.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:id=\"@+id/text_dashboard\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:gravity=\"center\"\n"
                        + "    android:text=\"Dashboard Fragment\"\n"
                        + "    android:textAppearance=\"?textAppearanceHeadlineMedium\" />\n");

        writeFile(new File(appDir, "src/main/res/layout/fragment_notifications.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:id=\"@+id/text_notifications\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:gravity=\"center\"\n"
                        + "    android:text=\"Notifications Fragment\"\n"
                        + "    android:textAppearance=\"?textAppearanceHeadlineMedium\" />\n");

        // Java/Kotlin code
        File homeDir = new File(srcPkg, "ui/home");
        File dashDir = new File(srcPkg, "ui/dashboard");
        File notifDir = new File(srcPkg, "ui/notifications");
        homeDir.mkdirs();
        dashDir.mkdirs();
        notifDir.mkdirs();

        if (isKotlin) {
            writeFile(new File(srcPkg, "MainActivity.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import androidx.appcompat.app.AppCompatActivity\n"
                            + "import androidx.navigation.findNavController\n"
                            + "import androidx.navigation.ui.AppBarConfiguration\n"
                            + "import androidx.navigation.ui.setupActionBarWithNavController\n"
                            + "import androidx.navigation.ui.setupWithNavController\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding\n\n"
                            + "class MainActivity : AppCompatActivity() {\n"
                            + "    private lateinit var binding: ActivityMainBinding\n\n"
                            + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                            + "        super.onCreate(savedInstanceState)\n"
                            + "        binding = ActivityMainBinding.inflate(layoutInflater)\n"
                            + "        setContentView(binding.root)\n"
                            + "        val navController = findNavController(R.id.nav_host_fragment_activity_main)\n"
                            + "        binding.navView.setupWithNavController(navController)\n"
                            + "    }\n"
                            + "}\n");

            writeFile(new File(homeDir, "HomeFragment.kt"),
                    "package " + pkg + ".ui.home\n\n"
                            + "import android.os.Bundle\n"
                            + "import android.view.LayoutInflater\n"
                            + "import android.view.View\n"
                            + "import android.view.ViewGroup\n"
                            + "import androidx.fragment.app.Fragment\n"
                            + "import " + pkg + ".databinding.FragmentHomeBinding\n\n"
                            + "class HomeFragment : Fragment() {\n"
                            + "    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {\n"
                            + "        return FragmentHomeBinding.inflate(inflater, container, false).root\n"
                            + "    }\n"
                            + "}\n");

            writeFile(new File(dashDir, "DashboardFragment.kt"),
                    "package " + pkg + ".ui.dashboard\n\n"
                            + "import android.os.Bundle\n"
                            + "import android.view.LayoutInflater\n"
                            + "import android.view.View\n"
                            + "import android.view.ViewGroup\n"
                            + "import androidx.fragment.app.Fragment\n"
                            + "import " + pkg + ".databinding.FragmentDashboardBinding\n\n"
                            + "class DashboardFragment : Fragment() {\n"
                            + "    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {\n"
                            + "        return FragmentDashboardBinding.inflate(inflater, container, false).root\n"
                            + "    }\n"
                            + "}\n");

            writeFile(new File(notifDir, "NotificationsFragment.kt"),
                    "package " + pkg + ".ui.notifications\n\n"
                            + "import android.os.Bundle\n"
                            + "import android.view.LayoutInflater\n"
                            + "import android.view.View\n"
                            + "import android.view.ViewGroup\n"
                            + "import androidx.fragment.app.Fragment\n"
                            + "import " + pkg + ".databinding.FragmentNotificationsBinding\n\n"
                            + "class NotificationsFragment : Fragment() {\n"
                            + "    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {\n"
                            + "        return FragmentNotificationsBinding.inflate(inflater, container, false).root\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MainActivity.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import androidx.appcompat.app.AppCompatActivity;\n"
                            + "import androidx.navigation.NavController;\n"
                            + "import androidx.navigation.Navigation;\n"
                            + "import androidx.navigation.ui.NavigationUI;\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding;\n\n"
                            + "public class MainActivity extends AppCompatActivity {\n"
                            + "    private ActivityMainBinding binding;\n\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        binding = ActivityMainBinding.inflate(getLayoutInflater());\n"
                            + "        setContentView(binding.getRoot());\n"
                            + "        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);\n"
                            + "        NavigationUI.setupWithNavController(binding.navView, navController);\n"
                            + "    }\n"
                            + "}\n");

            writeFile(new File(homeDir, "HomeFragment.java"),
                    "package " + pkg + ".ui.home;\n\n"
                            + "import android.os.Bundle;\n"
                            + "import android.view.LayoutInflater;\n"
                            + "import android.view.View;\n"
                            + "import android.view.ViewGroup;\n"
                            + "import androidx.annotation.NonNull;\n"
                            + "import androidx.fragment.app.Fragment;\n"
                            + "import " + pkg + ".databinding.FragmentHomeBinding;\n\n"
                            + "public class HomeFragment extends Fragment {\n"
                            + "    @Override\n"
                            + "    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {\n"
                            + "        return FragmentHomeBinding.inflate(inflater, container, false).getRoot();\n"
                            + "    }\n"
                            + "}\n");

            writeFile(new File(dashDir, "DashboardFragment.java"),
                    "package " + pkg + ".ui.dashboard;\n\n"
                            + "import android.os.Bundle;\n"
                            + "import android.view.LayoutInflater;\n"
                            + "import android.view.View;\n"
                            + "import android.view.ViewGroup;\n"
                            + "import androidx.annotation.NonNull;\n"
                            + "import androidx.fragment.app.Fragment;\n"
                            + "import " + pkg + ".databinding.FragmentDashboardBinding;\n\n"
                            + "public class DashboardFragment extends Fragment {\n"
                            + "    @Override\n"
                            + "    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {\n"
                            + "        return FragmentDashboardBinding.inflate(inflater, container, false).getRoot();\n"
                            + "    }\n"
                            + "}\n");

            writeFile(new File(notifDir, "NotificationsFragment.java"),
                    "package " + pkg + ".ui.notifications;\n\n"
                            + "import android.os.Bundle;\n"
                            + "import android.view.LayoutInflater;\n"
                            + "import android.view.View;\n"
                            + "import android.view.ViewGroup;\n"
                            + "import androidx.annotation.NonNull;\n"
                            + "import androidx.fragment.app.Fragment;\n"
                            + "import " + pkg + ".databinding.FragmentNotificationsBinding;\n\n"
                            + "public class NotificationsFragment extends Fragment {\n"
                            + "    @Override\n"
                            + "    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {\n"
                            + "        return FragmentNotificationsBinding.inflate(inflater, container, false).getRoot();\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeNavigationDrawerTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        File menuDir = new File(appDir, "src/main/res/menu");
        menuDir.mkdirs();

        writeFile(new File(menuDir, "activity_main_drawer.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <group android:checkableBehavior=\"single\">\n"
                        + "        <item\n"
                        + "            android:id=\"@+id/nav_home\"\n"
                        + "            android:icon=\"@android:drawable/ic_menu_agenda\"\n"
                        + "            android:title=\"@string/title_home\" />\n"
                        + "    </group>\n"
                        + "</menu>\n");

        writeFile(new File(appDir, "src/main/res/layout/nav_header_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"176dp\"\n"
                        + "    android:background=\"?attr/colorPrimaryContainer\"\n"
                        + "    android:gravity=\"bottom\"\n"
                        + "    android:orientation=\"vertical\"\n"
                        + "    android:padding=\"16dp\">\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/app_name\"\n"
                        + "        android:textAppearance=\"?textAppearanceHeadlineSmall\" />\n"
                        + "</LinearLayout>\n");

        writeFile(new File(appDir, "src/main/res/layout/activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<androidx.drawerlayout.widget.DrawerLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:id=\"@+id/drawer_layout\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\">\n"
                        + "        <com.google.android.material.appbar.MaterialToolbar\n"
                        + "            android:id=\"@+id/toolbar\"\n"
                        + "            android:layout_width=\"match_parent\"\n"
                        + "            android:layout_height=\"?attr/actionBarSize\"\n"
                        + "            app:title=\"@string/app_name\" />\n"
                        + "        <TextView\n"
                        + "            android:layout_width=\"match_parent\"\n"
                        + "            android:layout_height=\"match_parent\"\n"
                        + "            android:gravity=\"center\"\n"
                        + "            android:text=\"Navigation Drawer Content\"\n"
                        + "            android:textAppearance=\"?textAppearanceHeadlineSmall\" />\n"
                        + "    </LinearLayout>\n\n"
                        + "    <com.google.android.material.navigation.NavigationView\n"
                        + "        android:id=\"@+id/nav_view\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:layout_gravity=\"start\"\n"
                        + "        app:headerLayout=\"@layout/nav_header_main\"\n"
                        + "        app:menu=\"@menu/activity_main_drawer\" />\n"
                        + "</androidx.drawerlayout.widget.DrawerLayout>\n");

        if (isKotlin) {
            writeFile(new File(srcPkg, "MainActivity.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import androidx.appcompat.app.ActionBarDrawerToggle\n"
                            + "import androidx.appcompat.app.AppCompatActivity\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding\n\n"
                            + "class MainActivity : AppCompatActivity() {\n"
                            + "    private lateinit var binding: ActivityMainBinding\n\n"
                            + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                            + "        super.onCreate(savedInstanceState)\n"
                            + "        binding = ActivityMainBinding.inflate(layoutInflater)\n"
                            + "        setContentView(binding.root)\n"
                            + "        setSupportActionBar(binding.toolbar)\n"
                            + "        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.app_name, R.string.app_name)\n"
                            + "        binding.drawerLayout.addDrawerListener(toggle)\n"
                            + "        toggle.syncState()\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MainActivity.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import androidx.appcompat.app.ActionBarDrawerToggle;\n"
                            + "import androidx.appcompat.app.AppCompatActivity;\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding;\n\n"
                            + "public class MainActivity extends AppCompatActivity {\n"
                            + "    private ActivityMainBinding binding;\n\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        binding = ActivityMainBinding.inflate(getLayoutInflater());\n"
                            + "        setContentView(binding.getRoot());\n"
                            + "        setSupportActionBar(binding.toolbar);\n"
                            + "        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.app_name, R.string.app_name);\n"
                            + "        binding.drawerLayout.addDrawerListener(toggle);\n"
                            + "        toggle.syncState();\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeTabbedTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        writeFile(new File(appDir, "src/main/res/layout/activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\">\n\n"
                        + "    <com.google.android.material.tabs.TabLayout\n"
                        + "        android:id=\"@+id/tabs\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\" />\n\n"
                        + "    <androidx.viewpager2.widget.ViewPager2\n"
                        + "        android:id=\"@+id/view_pager\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\" />\n"
                        + "</LinearLayout>\n");

        if (isKotlin) {
            writeFile(new File(srcPkg, "MainActivity.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import androidx.appcompat.app.AppCompatActivity\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding\n\n"
                            + "class MainActivity : AppCompatActivity() {\n"
                            + "    private lateinit var binding: ActivityMainBinding\n\n"
                            + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                            + "        super.onCreate(savedInstanceState)\n"
                            + "        binding = ActivityMainBinding.inflate(layoutInflater)\n"
                            + "        setContentView(binding.root)\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MainActivity.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import androidx.appcompat.app.AppCompatActivity;\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding;\n\n"
                            + "public class MainActivity extends AppCompatActivity {\n"
                            + "    private ActivityMainBinding binding;\n\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        binding = ActivityMainBinding.inflate(getLayoutInflater());\n"
                            + "        setContentView(binding.getRoot());\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeFullscreenTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        writeFile(new File(appDir, "src/main/res/layout/activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:background=\"@color/black\">\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_gravity=\"center\"\n"
                        + "        android:text=\"Fullscreen Immersive Activity\"\n"
                        + "        android:textColor=\"@color/white\"\n"
                        + "        android:textAppearance=\"?textAppearanceHeadlineMedium\" />\n"
                        + "</FrameLayout>\n");

        if (isKotlin) {
            writeFile(new File(srcPkg, "MainActivity.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import androidx.appcompat.app.AppCompatActivity\n"
                            + "import androidx.core.view.WindowCompat\n"
                            + "import androidx.core.view.WindowInsetsCompat\n"
                            + "import androidx.core.view.WindowInsetsControllerCompat\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding\n\n"
                            + "class MainActivity : AppCompatActivity() {\n"
                            + "    private lateinit var binding: ActivityMainBinding\n\n"
                            + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                            + "        super.onCreate(savedInstanceState)\n"
                            + "        binding = ActivityMainBinding.inflate(layoutInflater)\n"
                            + "        setContentView(binding.root)\n"
                            + "        val controller = WindowCompat.getInsetsController(window, window.decorView)\n"
                            + "        controller.hide(WindowInsetsCompat.Type.systemBars())\n"
                            + "        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MainActivity.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import androidx.appcompat.app.AppCompatActivity;\n"
                            + "import androidx.core.view.WindowCompat;\n"
                            + "import androidx.core.view.WindowInsetsCompat;\n"
                            + "import androidx.core.view.WindowInsetsControllerCompat;\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding;\n\n"
                            + "public class MainActivity extends AppCompatActivity {\n"
                            + "    private ActivityMainBinding binding;\n\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        binding = ActivityMainBinding.inflate(getLayoutInflater());\n"
                            + "        setContentView(binding.getRoot());\n"
                            + "        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());\n"
                            + "        controller.hide(WindowInsetsCompat.Type.systemBars());\n"
                            + "        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeWearViewsTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        writeFile(new File(appDir, "src/main/res/layout/activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:padding=\"16dp\">\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_gravity=\"center\"\n"
                        + "        android:text=\"Wear OS App\"\n"
                        + "        android:textAppearance=\"?textAppearanceHeadlineSmall\" />\n"
                        + "</FrameLayout>\n");

        if (isKotlin) {
            writeFile(new File(srcPkg, "MainActivity.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import androidx.appcompat.app.AppCompatActivity\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding\n\n"
                            + "class MainActivity : AppCompatActivity() {\n"
                            + "    private lateinit var binding: ActivityMainBinding\n\n"
                            + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                            + "        super.onCreate(savedInstanceState)\n"
                            + "        binding = ActivityMainBinding.inflate(layoutInflater)\n"
                            + "        setContentView(binding.root)\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MainActivity.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import androidx.appcompat.app.AppCompatActivity;\n"
                            + "import " + pkg + ".databinding.ActivityMainBinding;\n\n"
                            + "public class MainActivity extends AppCompatActivity {\n"
                            + "    private ActivityMainBinding binding;\n\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        binding = ActivityMainBinding.inflate(getLayoutInflater());\n"
                            + "        setContentView(binding.getRoot());\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeWearComposeTemplate(File srcPkg, AndroidProjectConfig config) throws IOException {
        String pkg = config.getPackageName();
        writeFile(new File(srcPkg, "MainActivity.kt"),
                "package " + pkg + "\n\n"
                        + "import android.os.Bundle\n"
                        + "import androidx.activity.ComponentActivity\n"
                        + "import androidx.activity.compose.setContent\n"
                        + "import androidx.compose.foundation.layout.fillMaxSize\n"
                        + "import androidx.compose.runtime.Composable\n"
                        + "import androidx.compose.ui.Modifier\n"
                        + "import androidx.compose.ui.text.style.TextAlign\n"
                        + "import androidx.wear.compose.material.MaterialTheme\n"
                        + "import androidx.wear.compose.material.Scaffold\n"
                        + "import androidx.wear.compose.material.Text\n"
                        + "import androidx.wear.compose.material.TimeText\n\n"
                        + "class MainActivity : ComponentActivity() {\n"
                        + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                        + "        super.onCreate(savedInstanceState)\n"
                        + "        setContent {\n"
                        + "            MaterialTheme {\n"
                        + "                Scaffold(timeText = { TimeText() }) {\n"
                        + "                    Text(\n"
                        + "                        modifier = Modifier.fillMaxSize(),\n"
                        + "                        textAlign = TextAlign.Center,\n"
                        + "                        text = \"Hello Wear OS!\"\n"
                        + "                    )\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n");
    }

    private void writeWearWatchFaceTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        if (isKotlin) {
            writeFile(new File(srcPkg, "MyWatchFaceService.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.service.wallpaper.WallpaperService\n"
                            + "import android.view.SurfaceHolder\n\n"
                            + "class MyWatchFaceService : WallpaperService() {\n"
                            + "    override fun onCreateEngine(): Engine {\n"
                            + "        return MyEngine()\n"
                            + "    }\n\n"
                            + "    inner class MyEngine : Engine() {\n"
                            + "        override fun onSurfaceCreated(holder: SurfaceHolder) {\n"
                            + "            super.onSurfaceCreated(holder)\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MyWatchFaceService.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.service.wallpaper.WallpaperService;\n"
                            + "import android.view.SurfaceHolder;\n\n"
                            + "public class MyWatchFaceService extends WallpaperService {\n"
                            + "    @Override\n"
                            + "    public Engine onCreateEngine() {\n"
                            + "        return new MyEngine();\n"
                            + "    }\n\n"
                            + "    private class MyEngine extends Engine {\n"
                            + "        @Override\n"
                            + "        public void onSurfaceCreated(SurfaceHolder holder) {\n"
                            + "            super.onSurfaceCreated(holder);\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeWearTileTemplate(File srcPkg, String pkg, boolean isKotlin) throws IOException {
        if (isKotlin) {
            writeFile(new File(srcPkg, "MyTileService.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.content.Context\n"
                            + "import androidx.wear.tiles.TileService\n\n"
                            + "class MyTileService : TileService() {\n"
                            + "    // Wear OS Tile Service\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MyTileService.java"),
                    "package " + pkg + ";\n\n"
                            + "import androidx.wear.tiles.TileService;\n\n"
                            + "public class MyTileService extends TileService {\n"
                            + "    // Wear OS Tile Service\n"
                            + "}\n");
        }
    }

    private void writeTvLeanbackTemplate(File appDir, File srcPkg, String pkg, boolean isKotlin) throws IOException {
        writeFile(new File(appDir, "src/main/res/layout/activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:id=\"@+id/main_browse_fragment\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\" />\n");

        if (isKotlin) {
            writeFile(new File(srcPkg, "MainActivity.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.os.Bundle\n"
                            + "import androidx.fragment.app.FragmentActivity\n\n"
                            + "class MainActivity : FragmentActivity() {\n"
                            + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                            + "        super.onCreate(savedInstanceState)\n"
                            + "        setContentView(R.layout.activity_main)\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MainActivity.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.os.Bundle;\n"
                            + "import androidx.fragment.app.FragmentActivity;\n\n"
                            + "public class MainActivity extends FragmentActivity {\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        setContentView(R.layout.activity_main);\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeTvComposeTemplate(File srcPkg, AndroidProjectConfig config) throws IOException {
        String pkg = config.getPackageName();
        writeFile(new File(srcPkg, "MainActivity.kt"),
                "package " + pkg + "\n\n"
                        + "import android.os.Bundle\n"
                        + "import androidx.activity.ComponentActivity\n"
                        + "import androidx.activity.compose.setContent\n"
                        + "import androidx.compose.foundation.layout.fillMaxSize\n"
                        + "import androidx.compose.ui.Modifier\n"
                        + "import androidx.tv.material3.MaterialTheme\n"
                        + "import androidx.tv.material3.Text\n\n"
                        + "class MainActivity : ComponentActivity() {\n"
                        + "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                        + "        super.onCreate(savedInstanceState)\n"
                        + "        setContent {\n"
                        + "            MaterialTheme {\n"
                        + "                Text(text = \"Hello Android TV!\", modifier = Modifier.fillMaxSize())\n"
                        + "            }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n");
    }

    private void writeAutoCarAppTemplate(File srcPkg, String pkg, boolean isKotlin) throws IOException {
        if (isKotlin) {
            writeFile(new File(srcPkg, "MyCarAppService.kt"),
                    "package " + pkg + "\n\n"
                            + "import android.content.Intent\n"
                            + "import androidx.car.app.CarAppService\n"
                            + "import androidx.car.app.Screen\n"
                            + "import androidx.car.app.Session\n"
                            + "import androidx.car.app.model.Pane\n"
                            + "import androidx.car.app.model.PaneTemplate\n"
                            + "import androidx.car.app.model.Row\n"
                            + "import androidx.car.app.model.Template\n"
                            + "import androidx.car.app.validation.HostValidator\n\n"
                            + "class MyCarAppService : CarAppService() {\n"
                            + "    override fun createHostValidator(): HostValidator {\n"
                            + "        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR\n"
                            + "    }\n\n"
                            + "    override fun onCreateSession(): Session {\n"
                            + "        return object : Session() {\n"
                            + "            override fun onCreateScreen(intent: Intent): Screen {\n"
                            + "                return MyCarScreen(carContext)\n"
                            + "            }\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n\n"
                            + "class MyCarScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {\n"
                            + "    override fun onGetTemplate(): Template {\n"
                            + "        val pane = Pane.Builder()\n"
                            + "            .addRow(Row.Builder().setTitle(\"Welcome to Automotive\").build())\n"
                            + "            .build()\n"
                            + "        return PaneTemplate.Builder(pane)\n"
                            + "            .setTitle(\"Car App\")\n"
                            + "            .build()\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MyCarAppService.java"),
                    "package " + pkg + ";\n\n"
                            + "import android.content.Intent;\n"
                            + "import androidx.annotation.NonNull;\n"
                            + "import androidx.car.app.CarAppService;\n"
                            + "import androidx.car.app.CarContext;\n"
                            + "import androidx.car.app.Screen;\n"
                            + "import androidx.car.app.Session;\n"
                            + "import androidx.car.app.model.Pane;\n"
                            + "import androidx.car.app.model.PaneTemplate;\n"
                            + "import androidx.car.app.model.Row;\n"
                            + "import androidx.car.app.model.Template;\n"
                            + "import androidx.car.app.validation.HostValidator;\n\n"
                            + "public class MyCarAppService extends CarAppService {\n"
                            + "    @NonNull\n"
                            + "    @Override\n"
                            + "    public HostValidator createHostValidator() {\n"
                            + "        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;\n"
                            + "    }\n\n"
                            + "    @NonNull\n"
                            + "    @Override\n"
                            + "    public Session onCreateSession() {\n"
                            + "        return new Session() {\n"
                            + "            @NonNull\n"
                            + "            @Override\n"
                            + "            public Screen onCreateScreen(@NonNull Intent intent) {\n"
                            + "                return new MyCarScreen(getCarContext());\n"
                            + "            }\n"
                            + "        };\n"
                            + "    }\n\n"
                            + "    private static class MyCarScreen extends Screen {\n"
                            + "        public MyCarScreen(@NonNull CarContext carContext) {\n"
                            + "            super(carContext);\n"
                            + "        }\n\n"
                            + "        @NonNull\n"
                            + "        @Override\n"
                            + "        public Template onGetTemplate() {\n"
                            + "            Pane pane = new Pane.Builder()\n"
                            + "                .addRow(new Row.Builder().setTitle(\"Welcome to Automotive\").build())\n"
                            + "                .build();\n"
                            + "            return new PaneTemplate.Builder(pane)\n"
                            + "                .setTitle(\"Car App\")\n"
                            + "                .build();\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private void writeAutoMediaTemplate(File srcPkg, String pkg, boolean isKotlin) throws IOException {
        if (isKotlin) {
            writeFile(new File(srcPkg, "MyMediaPlaybackService.kt"),
                    "package " + pkg + "\n\n"
                            + "import androidx.media3.session.MediaSession\n"
                            + "import androidx.media3.session.MediaSessionService\n\n"
                            + "class MyMediaPlaybackService : MediaSessionService() {\n"
                            + "    private var mediaSession: MediaSession? = null\n\n"
                            + "    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {\n"
                            + "        return mediaSession\n"
                            + "    }\n\n"
                            + "    override fun onDestroy() {\n"
                            + "        mediaSession?.run {\n"
                            + "            player.release()\n"
                            + "            release()\n"
                            + "            mediaSession = null\n"
                            + "        }\n"
                            + "        super.onDestroy()\n"
                            + "    }\n"
                            + "}\n");
        } else {
            writeFile(new File(srcPkg, "MyMediaPlaybackService.java"),
                    "package " + pkg + ";\n\n"
                            + "import androidx.annotation.Nullable;\n"
                            + "import androidx.media3.session.MediaSession;\n"
                            + "import androidx.media3.session.MediaSessionService;\n\n"
                            + "public class MyMediaPlaybackService extends MediaSessionService {\n"
                            + "    private MediaSession mediaSession;\n\n"
                            + "    @Nullable\n"
                            + "    @Override\n"
                            + "    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {\n"
                            + "        return mediaSession;\n"
                            + "    }\n\n"
                            + "    @Override\n"
                            + "    public void onDestroy() {\n"
                            + "        if (mediaSession != null) {\n"
                            + "            mediaSession.getPlayer().release();\n"
                            + "            mediaSession.release();\n"
                            + "            mediaSession = null;\n"
                            + "        }\n"
                            + "        super.onDestroy();\n"
                            + "    }\n"
                            + "}\n");
        }
    }

    private String sanitizeThemeName(String name) {
        if (name == null || name.isEmpty()) return "MyApplication";
        String s = name.replaceAll("[^a-zA-Z0-9]", "");
        return s.isEmpty() ? "MyApplication" : s;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "\\'");
    }

    private String getDefaultWrapperProperties() {
        return "distributionBase=GRADLE_USER_HOME\n"
                + "distributionPath=wrapper/dists\n"
                + "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.9-bin.zip\n"
                + "zipStoreBase=GRADLE_USER_HOME\n"
                + "zipStorePath=wrapper/dists\n";
    }

    private String getDefaultGradlewScript() {
        return "#!/bin/sh\n"
                + "exec gradle \"$@\"\n";
    }

    private String getDefaultGradlewBat() {
        return "@rem Gradle startup script for Windows\r\n"
                + "@gradle %*\r\n";
    }
}
