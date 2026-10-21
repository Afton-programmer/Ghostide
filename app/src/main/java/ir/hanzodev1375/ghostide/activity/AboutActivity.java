package ir.hanzodev1375.ghostide.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;
import ir.hanzodev1375.ghostide.adapters.ContributorAdapter;
import ir.hanzodev1375.ghostide.mvvm.viewmodel.AboutViewModel;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.components.childern.ViewChilder;
import ir.hanzodev1375.ghostide.R;
import ir.theme.M3Theme;
import ir.theme.ThemeUtils;

public class AboutActivity extends BaseCompat {
  private GlassCompat cardDisplay, cardMemory, cardStorage, cardDeviceInfo;
  private GlassCompat cardContributors, cardStars;
  private LinearLayout bodyDeviceInfo, bodyDisplay, bodyMemory, bodyStorage;
  private View arrowDeviceInfo, arrowDisplay, arrowMemory, arrowStorage;
  private RecyclerView contributorList;
  private TextView contributorStatus, starsValue;
  private ContributorAdapter contributorAdapter;
  private AboutViewModel viewModel;
  private boolean expDeviceInfo, expDisplay, expMemory, expStorage;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);
    setupBackgroundBlur();

    View root = findViewById(R.id.root_scroll);
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
          int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
          v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), bottom);
          return insets;
        });

    bindViews();
    fillVersion();
    setupGlassBackdrop(cardContributors);
    setupGlassBackdrop(cardStars);
    setupGlassBackdrop(cardDeviceInfo);
    setupGlassBackdrop(cardDisplay);
    setupGlassBackdrop(cardMemory);
    setupGlassBackdrop(cardStorage);
    fillData();
    setupClickListeners();
    setupContributors();
    animateCardsIn();

    M3Theme.applyTopLevel(findViewById(R.id.contentAreaAbout));
    applyCardTints();
    root.post(this::applyCardTints);
  }

  private void setupGlassBackdrop(GlassCompat glass) {
    glass.setBackdropSource(findViewById(R.id.contentAreaAbout));
    glass.setEnableDynamicBackground(true);
    glass.setEnableChromaticAberration(true);
    glass.setEnableEdgeHighlight(true);
  }

  private void fillVersion() {
    TextView versionTitle = findViewById(R.id.tv_about_version);
    String version = "";
    try {
      String packageVersion =
          getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
      if (packageVersion != null) version = packageVersion;
    } catch (PackageManager.NameNotFoundException e) {
      version = "";
    }
    versionTitle.setText(getString(R.string.about_version_title, version));
  }

  private void setupContributors() {
    contributorAdapter = new ContributorAdapter();
    contributorList.setLayoutManager(
        new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
    contributorList.setAdapter(contributorAdapter);

    viewModel = new ViewModelProvider(this).get(AboutViewModel.class);
    viewModel
        .getStars()
        .observe(
            this,
            value -> {
              if (value == null || value < 0) {
                starsValue.setText(R.string.about_unavailable);
              } else {
                starsValue.setText(NumberFormat.getIntegerInstance().format(value));
              }
            });
    viewModel
        .getContributors()
        .observe(
            this,
            contributors -> {
              if (contributors == null) return;
              if (contributors.isEmpty()) {
                contributorStatus.setText(R.string.about_unavailable);
                contributorStatus.setVisibility(View.VISIBLE);
                contributorList.setVisibility(View.GONE);
                return;
              }
              contributorStatus.setVisibility(View.GONE);
              contributorList.setVisibility(View.VISIBLE);
              contributorAdapter.setItems(contributors);
            });
    viewModel.load();
  }

  private void applyCardTints() {
    tintCard(cardContributors, M3Theme.primaryContainer());
    tintCard(cardStars, M3Theme.tertiaryContainer());
    tintCard(cardDeviceInfo, M3Theme.secondaryContainer());
    tintCard(cardDisplay, M3Theme.primaryFixedDim());
    tintCard(cardMemory, M3Theme.secondaryFixedDim());
    tintCard(cardStorage, M3Theme.tertiaryFixedDim());
  }

  private void tintCard(GlassCompat card, Integer color) {
    if (card == null || color == null) return;
    card.setGlassTint(color, 0.5f);
  }

  @Override
  protected void applyOwnTheme(ThemeUtils themeUtils) {
    applyCardTints();
  }

  private void setupBackgroundBlur() {
    ViewChilder background = findViewById(R.id.backgroundIconAbout);
    View content = findViewById(R.id.root_scroll);
    if (background == null || content == null) return;
    setupBackgroundBlur(background, content);
  }

  private void bindViews() {
    cardContributors = findViewById(R.id.card_contributors);
    cardStars = findViewById(R.id.card_stars);
    cardDeviceInfo = findViewById(R.id.card_device_info);
    cardDisplay = findViewById(R.id.card_display);
    cardMemory = findViewById(R.id.card_memory);
    cardStorage = findViewById(R.id.card_storage);
    contributorList = findViewById(R.id.rv_contributors);
    contributorStatus = findViewById(R.id.tv_contributors_status);
    starsValue = findViewById(R.id.tv_stars_value);

    bodyDeviceInfo = findViewById(R.id.body_device_info);
    bodyDisplay = findViewById(R.id.body_display);
    bodyMemory = findViewById(R.id.body_memory);
    bodyStorage = findViewById(R.id.body_storage);

    arrowDeviceInfo = findViewById(R.id.arrow_device_info);
    arrowDisplay = findViewById(R.id.arrow_display);
    arrowMemory = findViewById(R.id.arrow_memory);
    arrowStorage = findViewById(R.id.arrow_storage);
  }

  @SuppressLint("SetTextI18n")
  private void fillData() {

    fillDouble(R.id.row_model, "Model", Build.MODEL, "Manufacturer", Build.MANUFACTURER);

    fillDouble(R.id.row_device, "Device", Build.DEVICE, "Brand", Build.BRAND);

    fillDouble(
        R.id.row_sdk,
        "SDK Version",
        String.valueOf(Build.VERSION.SDK_INT),
        "Android Version",
        Build.VERSION.RELEASE);

    fillDouble(
        R.id.row_arch,
        "Architecture",
        System.getProperty("os.arch", "unknown"),
        "Hardware",
        Build.HARDWARE);

    fillSingle(R.id.row_abis, String.join(", ", Build.SUPPORTED_ABIS), "Supported ABIs");

    android.hardware.display.DisplayManager dm =
        (android.hardware.display.DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
    Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

    fillSingle(R.id.row_refresh, "Hz " + (int) display.getRefreshRate(), "Max Refresh Rate");

    ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    var cfg = am.getDeviceConfigurationInfo();
    String glVer = cfg.getGlEsVersion();
    fillSingle(R.id.row_opengl, glVer != null ? glVer : "N/A", "OpenGL Version");

    boolean vulkan = getPackageManager().hasSystemFeature("android.hardware.vulkan.level");
    fillSingle(R.id.row_vulkan, vulkan ? "Yes" : "No", "Supports Vulkan");

    boolean hdr = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && display.isHdr();
    fillSingle(R.id.row_hdr, hdr ? "Yes" : "No", "Supports HDR");

    boolean wideColor =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && display.isWideColorGamut();
    fillSingle(R.id.row_wide_color, wideColor ? "Yes" : "No", "Wide Color Gamut");

    ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
    am.getMemoryInfo(memInfo);
    long totalMb = memInfo.totalMem / (1024 * 1024);

    fillSingle(R.id.row_total_memory, "MB " + totalMb, "Total Memory");
    fillSingle(R.id.row_low_ram, memInfo.lowMemory ? "Yes" : "No", "Low RAM Device");
    fillSingle(R.id.row_large_heap, isLargeHeap() ? "Yes" : "No", "Large Heap");
    fillSingle(
        R.id.row_runtime,
        Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] + " (64-bit)" : "unknown",
        "Runtime");

    StatFs internal = new StatFs(Environment.getDataDirectory().getPath());
    long intTotal = internal.getTotalBytes() / (1024L * 1024 * 1024);
    long intFree = internal.getFreeBytes() / (1024L * 1024 * 1024);
    fillSingle(R.id.row_internal_total, intTotal + " GB", "Internal Total");
    fillSingle(R.id.row_internal_free, intFree + " GB", "Internal Free");

    File extDir = getExternalFilesDir(null);
    if (extDir != null) {
      StatFs ext = new StatFs(extDir.getPath());
      long extTotal = ext.getTotalBytes() / (1024L * 1024 * 1024);
      long extFree = ext.getFreeBytes() / (1024L * 1024 * 1024);
      fillSingle(R.id.row_external_total, extTotal + " GB", "External Total");
      fillSingle(R.id.row_external_free, extFree + " GB", "External Free");
    } else {
      fillSingle(R.id.row_external_total, "N/A", "External Total");
      fillSingle(R.id.row_external_free, "N/A", "External Free");
    }
  }

  private void fillSingle(int rowId, String value, String label) {
    View row = findViewById(rowId);
    if (row == null) return;
    setText(row, R.id.tv_value, value);
    setText(row, R.id.tv_label, label);
  }

  private void fillDouble(int rowId, String label1, String value1, String label2, String value2) {
    View row = findViewById(rowId);
    if (row == null) return;
    setText(row, R.id.tv_label, label1);
    setText(row, R.id.tv_value, value1);
    setText(row, R.id.tv_label2, label2);
    setText(row, R.id.tv_value2, value2);
  }

  private void setText(View parent, int id, String text) {
    TextView tv = parent.findViewById(id);
    if (tv != null) tv.setText(text);
  }

  private boolean isLargeHeap() {
    try {
      return (getApplicationInfo().flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0;
    } catch (Exception e) {
      return false;
    }
  }

  private void setupClickListeners() {
    cardDeviceInfo.setOnClickListener(
        v -> {
          expDeviceInfo = !expDeviceInfo;
          toggleCard(bodyDeviceInfo, arrowDeviceInfo, expDeviceInfo);
        });
    cardDisplay.setOnClickListener(
        v -> {
          expDisplay = !expDisplay;
          toggleCard(bodyDisplay, arrowDisplay, expDisplay);
        });
    cardMemory.setOnClickListener(
        v -> {
          expMemory = !expMemory;
          toggleCard(bodyMemory, arrowMemory, expMemory);
        });
    cardStorage.setOnClickListener(
        v -> {
          expStorage = !expStorage;
          toggleCard(bodyStorage, arrowStorage, expStorage);
        });
  }

  private void toggleCard(LinearLayout body, View arrow, boolean expand) {

    ObjectAnimator rot =
        ObjectAnimator.ofFloat(arrow, "rotation", expand ? -90f : 0f, expand ? 0f : -90f);
    rot.setDuration(300);
    rot.setInterpolator(new AccelerateDecelerateInterpolator());
    rot.start();

    if (expand) {
      body.setVisibility(View.VISIBLE);
      body.setAlpha(0f);
      body.animate()
          .alpha(1f)
          .setDuration(250)
          .setInterpolator(new DecelerateInterpolator())
          .start();
      expandHeight(body);
    } else {
      body.animate()
          .alpha(0f)
          .setDuration(200)
          .setInterpolator(new DecelerateInterpolator())
          .setListener(
              new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator a) {
                  body.setVisibility(View.GONE);
                  body.setAlpha(1f);
                  body.animate().setListener(null);
                }
              })
          .start();
      collapseHeight(body);
    }
  }

 private void expandHeight(final View v) {
    v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    final int target = v.getMeasuredHeight();
    v.getLayoutParams().height = 0;
    v.requestLayout();
    ValueAnimator a = ValueAnimator.ofInt(0, target);
    a.setDuration(320).setInterpolator(new DecelerateInterpolator());
    a.addUpdateListener(
        va -> {
          v.getLayoutParams().height = (int) va.getAnimatedValue();
          v.requestLayout();
        });
    a.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator an) {
            v.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
          }
        });
    a.start();
  }

  private void collapseHeight(final View v) {
    final int init = v.getMeasuredHeight();
    ValueAnimator a = ValueAnimator.ofInt(init, 0);
    a.setDuration(260).setInterpolator(new AccelerateDecelerateInterpolator());
    a.addUpdateListener(
        va -> {
          v.getLayoutParams().height = (int) va.getAnimatedValue();
          v.requestLayout();
        });
    a.start();
  }

  private void animateCardsIn() {
    List<View> cards = new ArrayList<>();
    cards.add(cardContributors);
    cards.add(cardStars);
    cards.add(cardDeviceInfo);
    cards.add(cardDisplay);
    cards.add(cardMemory);
    cards.add(cardStorage);
    for (int i = 0; i < cards.size(); i++) {
      View c = cards.get(i);
      c.setAlpha(0f);
      c.setTranslationY(64f);
      c.animate()
          .alpha(1f)
          .translationY(0f)
          .setStartDelay(70L * i)
          .setDuration(420)
          .setInterpolator(new DecelerateInterpolator(1.6f))
          .start();
    }
  }
}
