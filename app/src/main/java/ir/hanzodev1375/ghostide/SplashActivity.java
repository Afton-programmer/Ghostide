package ir.hanzodev1375.ghostide;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import ir.hanzodev1375.components.glsl.GLSLShaderView;
import ir.hanzodev1375.components.glsl.GlslPalette;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.activity.FileManagerActivity;
import ir.hanzodev1375.ghostide.onboarding.OnboardingActivity;
import ir.hanzodev1375.ghostide.onboarding.OnboardingPrefs;
import ir.hanzodev1375.ghostide.utils.ObjectUtil;
import ir.theme.M3Theme;

public class SplashActivity extends BaseCompat {

  private static final long LOGO_FADE_MS = 800L;
  private static final long SPLASH_HOLD_MS = 3000L;

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable proceedRunnable = this::startFileManager;
  private boolean started = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    GLSLShaderView shader = findViewById(R.id.splash_shader);
    int[] colors = GlslPalette.background();
    shader.setColors(colors[0], colors[1], colors[2]);

    ImageView logo = findViewById(R.id.logo);
    logo.setAlpha(0f);
    logo.animate().alpha(1f).setDuration(LOGO_FADE_MS).start();

    handler.postDelayed(proceedRunnable, LOGO_FADE_MS + SPLASH_HOLD_MS);

    M3Theme.apply(findViewById(android.R.id.content));
  }

  private void startFileManager() {
    if (started || isFinishing()) return;
    started = true;
    ImageView logo = findViewById(R.id.logo);
    Intent intent;
    if (OnboardingPrefs.shouldShow(this)) {
      intent = new Intent(this, OnboardingActivity.class);
    } else {
      intent = new Intent(this, FileManagerActivity.class);
    }
    startActivityWithSharedElement(intent, logo, ObjectUtil.TRANSITION_LOGO);
    finish();
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacks(proceedRunnable);
    super.onDestroy();
  }
}
