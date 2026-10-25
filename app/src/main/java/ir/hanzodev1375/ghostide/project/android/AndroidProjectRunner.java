package ir.hanzodev1375.ghostide.project.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.hanzodev1375.components.views.GhostToast;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.AndroidClasspathResolver;
import ir.hanzodev1375.ghostide.shizuku.ShizukuManager;
import ir.hanzodev1375.ghostide.terminal.activity.TerminalActivity;
import ir.hanzodev1375.ghostide.terminal.sheet.TerminalBottomSheetFragment;
import ninja.coder.appuploader.main.ApkInstallerCompat;

/**
 * AndroidProjectRunner handles building and running Android projects in Ghost IDE.
 * It identifies project roots, ensures JDK 17 environment, runs assembleDebug via Gradle wrapper,
 * watches for output APK generation, and automatically launches the Android package installer.
 */
public final class AndroidProjectRunner {

    private static final String TAG = "AndroidProjectRunner";
    private static final int MAX_CHECKS = 240; // 240 * 1500ms = 6 minutes timeout

    private AndroidProjectRunner() {}

    /**
     * Resolves the Android project root by walking up from the given file or directory.
     * Looks for Gradle project markers (settings.gradle, settings.gradle.kts, gradlew)
     * and Android module structures (app/src/main/AndroidManifest.xml).
     */
    public static File findProjectRoot(File fileOrDir) {
        if (fileOrDir == null) return null;
        File dir = fileOrDir.isDirectory() ? fileOrDir : fileOrDir.getParentFile();
        if (dir == null) return null;

        File cursor = dir;
        int depth = 0;
        File potentialRoot = null;

        while (cursor != null && depth < 10) {
            if (new File(cursor, "settings.gradle").exists()
                    || new File(cursor, "settings.gradle.kts").exists()
                    || new File(cursor, "gradlew").exists()
                    || new File(cursor, "gradlew.bat").exists()) {
                return cursor;
            }

            if (new File(cursor, "app/src/main/AndroidManifest.xml").exists()
                    || new File(cursor, "app/build.gradle.kts").exists()
                    || new File(cursor, "app/build.gradle").exists()) {
                return cursor;
            }

            if (new File(cursor, "src/main/AndroidManifest.xml").exists()) {
                potentialRoot = cursor.getParentFile() != null ? cursor.getParentFile() : cursor;
            }

            cursor = cursor.getParentFile();
            depth++;
        }

        return potentialRoot;
    }

    /**
     * Locates the latest built APK file for the given Android project root.
     */
    public static File findLatestBuiltApk(File projectRoot) {
        if (projectRoot == null || !projectRoot.isDirectory()) return null;

        File marker = new File(projectRoot, ".last_built_apk");
        if (marker.isFile() && marker.length() > 0) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(marker), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    File apk = new File(projectRoot, line.trim());
                    if (apk.isFile() && apk.exists()) return apk;
                    File abs = new File(line.trim());
                    if (abs.isFile() && abs.exists()) return abs;
                }
            } catch (IOException e) {
                Log.w(TAG, "Error reading marker: " + marker.getAbsolutePath(), e);
            }
        }

        File[] candidateDirs = new File[] {
            new File(projectRoot, "app/build/outputs/apk/debug"),
            new File(projectRoot, "build/outputs/apk/debug"),
            new File(projectRoot, "app/build/outputs/apk/release"),
            new File(projectRoot, "build/outputs/apk/release")
        };

        File latestApk = null;
        long latestMtime = 0;

        for (File dir : candidateDirs) {
            if (dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".apk"));
                if (files != null) {
                    for (File f : files) {
                        if (f.lastModified() > latestMtime) {
                            latestMtime = f.lastModified();
                            latestApk = f;
                        }
                    }
                }
            }
        }

        return latestApk;
    }

    /**
     * Installs an APK using Shizuku (if available and granted) or the system package installer.
     */
    public static void installApk(Activity activity, File apkFile) {
        if (activity == null || apkFile == null || !apkFile.exists()) {
            if (activity != null) {
                GhostToast.makeText(activity, R.string.android_apk_not_found, GhostToast.LENGTH_SHORT).show();
            }
            return;
        }

        GhostToast.makeText(activity, R.string.android_installing_apk, GhostToast.LENGTH_SHORT).show();

        if (ShizukuManager.isAvailable() && ShizukuManager.hasPermission()) {
            ShizukuManager.exec(
                    new String[] {"pm", "install", "-r", "-i", activity.getPackageName(), apkFile.getAbsolutePath()},
                    new ShizukuManager.ExecCallback() {
                        @Override
                        public void onResult(String output) {
                            boolean success = output != null && output.toLowerCase().contains("success");
                            if (success) {
                                GhostToast.makeText(activity, "App installed successfully via Shizuku", GhostToast.LENGTH_SHORT).show();
                            } else {
                                installApkViaSystem(activity, apkFile);
                            }
                        }

                        @Override
                        public void onUnavailable() {
                            installApkViaSystem(activity, apkFile);
                        }
                    });
            return;
        }

        installApkViaSystem(activity, apkFile);
    }

    private static void installApkViaSystem(Activity activity, File apkFile) {
        try {
            new ApkInstallerCompat(activity, apkFile, null, null).install();
        } catch (Exception t) {
            try {
                Uri apkUri = FileProvider.getUriForFile(
                        activity,
                        activity.getPackageName() + ".fileprovider",
                        apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } catch (Exception ex) {
                GhostToast.makeText(activity, "Install error: " + ex.getMessage(), GhostToast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Executes the Android project. If an APK was already built, prompts whether to install or rebuild.
     * Otherwise immediately builds and monitors for completion.
     */
    public static void runProject(Activity activity, File projectRoot, boolean asBottomSheet) {
        if (activity == null || projectRoot == null) return;

        File existingApk = findLatestBuiltApk(projectRoot);
        if (existingApk != null && existingApk.exists()) {
            new DialogCompat(activity)
                    .setTitle(R.string.android_run_dialog_title)
                    .setMessage(String.format(
                            activity.getString(R.string.android_run_dialog_msg),
                            projectRoot.getName()))
                    .setPositiveButton(
                            R.string.android_run_action_install,
                            (dialog, which) -> {
                                dialog.dismiss();
                                installApk(activity, existingApk);
                            })
                    .setNegativeButton(
                            R.string.android_run_action_rebuild,
                            (dialog, which) -> {
                                dialog.dismiss();
                                startBuildAndWatch(activity, projectRoot, asBottomSheet);
                            })
                    .setNeutralButton(
                            R.string.action_cancel,
                            (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            startBuildAndWatch(activity, projectRoot, asBottomSheet);
        }
    }

    /**
     * Starts the assembleDebug Gradle build in the terminal and polls for the finished APK.
     */
    public static void startBuildAndWatch(Activity activity, File projectRoot, boolean asBottomSheet) {
        File marker = new File(projectRoot, ".last_built_apk");
        if (marker.exists()) {
            marker.delete();
        }

        final long buildStartTime = System.currentTimeMillis();

        String command = buildGradleBashCommand(activity, projectRoot);

        if (asBottomSheet) {
            runInBottomSheet(activity, command);
        } else {
            runInActivity(activity, command);
        }

        watchBuildOutput(activity, projectRoot, buildStartTime);
    }

    /**
     * Constructs the bash command line to run inside Debian PRoot.
     * Handles setting JAVA_HOME (using OpenJDK 17 or Temurin 17), ANDROID_HOME, and gradlew.
     */
    public static String buildGradleBashCommand(Context context, File projectRoot) {
        File rootfs = DebianBootstrap.getRootfsDir(context);
        String sdkGuestPath = AndroidClasspathResolver.findAndroidSdkGuestPath(rootfs);

        String androidHomeExport = "";
        if (sdkGuestPath != null && !sdkGuestPath.isEmpty()) {
            androidHomeExport = "export ANDROID_HOME='" + sdkGuestPath + "' ANDROID_SDK_ROOT='" + sdkGuestPath + "'; ";
        }

        File gradlew = new File(projectRoot, "gradlew");
        String gradleExecutable;
        if (gradlew.exists()) {
            gradleExecutable = "chmod +x '" + gradlew.getAbsolutePath() + "'; ./gradlew";
        } else {
            gradleExecutable = "if ! command -v gradle >/dev/null 2>&1; then apt-get update && apt-get install -y gradle; fi; gradle";
        }

        return "clear; "
                + "cd '" + projectRoot.getAbsolutePath() + "' && "
                + "echo '🚀 Building Android Project (assembleDebug)...'; "
                + "GHOST_JDK=$(ls -d /usr/lib/jvm/temurin-17-jdk-* 2>/dev/null | head -n 1); "
                + "if [ -z \"$GHOST_JDK\" ]; then "
                + "  GHOST_JDK=$(ls -d /usr/lib/jvm/java-17-openjdk-* 2>/dev/null | head -n 1); "
                + "fi; "
                + "if [ -z \"$GHOST_JDK\" ]; then "
                + "  if ! command -v javac >/dev/null 2>&1; then "
                + "    echo '📦 Installing OpenJDK 17 for Android Gradle...'; "
                + "    export DEBIAN_FRONTEND=noninteractive; "
                + "    apt-get update && apt-get install -y openjdk-17-jdk-headless ca-certificates; "
                + "    GHOST_JDK=$(ls -d /usr/lib/jvm/java-17-openjdk-* 2>/dev/null | head -n 1); "
                + "  fi; "
                + "fi; "
                + "if [ -n \"$GHOST_JDK\" ]; then "
                + "  export JAVA_HOME=\"$GHOST_JDK\"; "
                + "  export PATH=\"$JAVA_HOME/bin:$PATH\"; "
                + "fi; "
                + androidHomeExport
                + gradleExecutable + " assembleDebug; "
                + "BUILD_STATUS=$?; "
                + "if [ $BUILD_STATUS -eq 0 ]; then "
                + "  APK=$(find app/build/outputs/apk/debug -name '*.apk' 2>/dev/null | head -n 1); "
                + "  if [ -z \"$APK\" ]; then APK=$(find build/outputs/apk/debug -name '*.apk' 2>/dev/null | head -n 1); fi; "
                + "  if [ -n \"$APK\" ]; then "
                + "    echo '✅ Build Successful!'; "
                + "    echo \"📦 Output APK: $APK\"; "
                + "    echo \"$APK\" > .last_built_apk; "
                + "    echo '📲 Launching Android Package Installer...'; "
                + "  fi; "
                + "else "
                + "  echo \"❌ Build failed with exit code $BUILD_STATUS\"; "
                + "fi";
    }

    private static void watchBuildOutput(Activity activity, File projectRoot, long buildStartTime) {
        Handler handler = new Handler(Looper.getMainLooper());
        final int[] checksCount = new int[] { 0 };

        Runnable poller = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) return;

                checksCount[0]++;

                File marker = new File(projectRoot, ".last_built_apk");
                File foundApk = null;

                if (marker.isFile() && marker.length() > 0) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(new FileInputStream(marker), StandardCharsets.UTF_8))) {
                        String line = reader.readLine();
                        if (line != null && !line.trim().isEmpty()) {
                            File apk = new File(projectRoot, line.trim());
                            if (apk.isFile() && apk.exists()) {
                                foundApk = apk;
                            }
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "Error reading marker: " + marker.getAbsolutePath(), e);
                    }
                }

                if (foundApk == null) {
                    File apkDir = new File(projectRoot, "app/build/outputs/apk/debug");
                    if (apkDir.isDirectory()) {
                        File[] apks = apkDir.listFiles((d, n) -> n.endsWith(".apk"));
                        if (apks != null) {
                            for (File a : apks) {
                                if (a.lastModified() >= buildStartTime) {
                                    foundApk = a;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (foundApk != null && foundApk.exists()) {
                    installApk(activity, foundApk);
                    return;
                }

                if (checksCount[0] < MAX_CHECKS) {
                    handler.postDelayed(this, 1500);
                }
            }
        };

        handler.postDelayed(poller, 2500);
    }

    private static void runInActivity(Context context, String command) {
        Intent i = new Intent(context, TerminalActivity.class);
        i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
        if (!(context instanceof Activity)) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(i);
    }

    private static void runInBottomSheet(Activity activity, String command) {
        if (!(activity instanceof FragmentActivity)) {
            runInActivity(activity, command);
            return;
        }
        FragmentManager fm = ((FragmentActivity) activity).getSupportFragmentManager();
        Fragment previous = fm.findFragmentByTag(TerminalBottomSheetFragment.TAG);
        if (previous != null) {
            if (previous instanceof DialogFragment) {
                ((DialogFragment) previous).dismissAllowingStateLoss();
            } else {
                fm.beginTransaction().remove(previous).commitAllowingStateLoss();
            }
        }
        TerminalBottomSheetFragment.newInstance(command, null).show(fm, TerminalBottomSheetFragment.TAG);
    }
}
