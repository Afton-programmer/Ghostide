package ir.hanzodev1375.components.views;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;
import ir.hanzodev1375.components.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import ir.hanzodev1375.components.databinding.LayoutSyncBinding;
import ir.theme.M3Theme;

/**
 * Android-Studio-style project sync bar: an icon, a headline with a status line and a thin progress
 * bar, shown under the editor tabs while a plugin installs/refreshes a project's dependencies.
 *
 * <p>Besides the plain running state it can also ask the user before doing the work ({@link
 * #showPrompt(CharSequence, CharSequence, CharSequence, CharSequence, Runnable, Runnable)}) and
 * surface a failure with a copy-to-clipboard action ({@link #showError(CharSequence,
 * CharSequence)}), so a plugin can drive the whole install flow without owning any dialog.
 *
 * <p>Colors are read from {@link M3Theme} by {@link #applyTheme()}, so the bar follows the active
 * theme. It is hidden by default; call {@link #show(CharSequence)} to reveal it.
 */
public class SyncBar extends FrameLayout {

  private static final long REVEAL_MS = 220L;
  private static final long HIDE_MS = 180L;

  private final LayoutSyncBinding binding;

  @Nullable private Runnable confirmAction;
  @Nullable private Runnable cancelAction;
  @Nullable private ObjectAnimator iconSpin;
  private boolean errorState;

  public SyncBar(@NonNull Context context) {
    this(context, null);
  }

  public SyncBar(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SyncBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    binding = LayoutSyncBinding.inflate(LayoutInflater.from(context), this, true);
    binding.syncConfirm.setOnClickListener(v -> onConfirmClick());
    binding.syncCancel.setOnClickListener(v -> onCancelClick());
    binding.syncCopy.setOnClickListener(v -> copyError());
    binding.syncClose.setOnClickListener(v -> hide());
    applyTheme();
    hide(false);
  }

  /** Reveals the bar in the running state with an indeterminate progress bar. */
  public void show(CharSequence title) {
    resetActions(true);
    binding.syncTitle.setText(title);
    setMessage(null);
    enterRunning(true);
    reveal();
  }

  /**
   * Reveals the bar asking the user to confirm an action. Nothing runs until the confirm button is
   * tapped; {@code onCancel} is invoked when the cancel button is tapped.
   */
  public void showPrompt(
      CharSequence title,
      CharSequence message,
      CharSequence confirmLabel,
      CharSequence cancelLabel,
      @Nullable Runnable onConfirm,
      @Nullable Runnable onCancel) {
    confirmAction = onConfirm;
    cancelAction = onCancel;
    binding.syncTitle.setText(title);
    setMessage(message);
    binding.syncConfirm.setText(confirmLabel);
    binding.syncCancel.setText(cancelLabel);
    binding.syncProgress.setVisibility(GONE);
    stopSpin();
    errorState = false;
    applyBackground(null);
    binding.syncConfirm.setVisibility(VISIBLE);
    binding.syncCancel.setVisibility(VISIBLE);
    binding.syncCopy.setVisibility(GONE);
    binding.syncClose.setVisibility(GONE);
    applyTheme();
    reveal();
  }

  /** Reveals the bar in a persistent error state; the message can be copied to the clipboard. */
  public void showError(CharSequence title, CharSequence message) {
    resetActions(true);
    binding.syncTitle.setText(title);
    setMessage(message);
    binding.syncProgress.setVisibility(GONE);
    stopSpin();
    errorState = true;
    binding.syncConfirm.setVisibility(GONE);
    binding.syncCancel.setVisibility(GONE);
    binding.syncCopy.setVisibility(VISIBLE);
    binding.syncClose.setVisibility(VISIBLE);
    applyTheme();
    reveal();
  }

  /** Starts or stops the thin progress bar's indeterminate animation. */
  public void setIndeterminate(boolean indeterminate) {
    LinearProgressIndicator progress = binding.syncProgress;
    progress.setIndeterminate(indeterminate);
    if (indeterminate) {
      progress.setProgressCompat(0, false);
    }
  }

  /**
   * Sets determinate progress, {@code 0..100}. A negative value switches back to indeterminate.
   * Calling this leaves any prompt state and shows the running progress.
   */
  public void setProgress(int percent) {
    enterRunning(false);
    if (percent < 0) {
      setIndeterminate(true);
      return;
    }
    binding.syncProgress.setIndeterminate(false);
    binding.syncProgress.setProgressCompat(Math.min(100, Math.max(0, percent)), true);
  }

  public void setTitle(CharSequence title) {
    binding.syncTitle.setText(title);
  }

  public void setMessage(CharSequence message) {
    if (message == null || message.length() == 0) {
      binding.syncMessage.setText("");
      binding.syncMessage.setVisibility(GONE);
    } else {
      binding.syncMessage.setText(message);
      binding.syncMessage.setVisibility(VISIBLE);
    }
  }

  @Nullable
  public CharSequence getTitle() {
    return binding.syncTitle.getText();
  }

  public boolean isActive() {
    return getVisibility() == VISIBLE;
  }

  /** Hides the bar with a short fade-out. */
  public void hide() {
    hide(true);
  }

  private void hide(boolean animate) {
    stopSpin();
    animate().cancel();
    if (!animate) {
      setVisibility(GONE);
      setAlpha(1f);
      setTranslationY(0f);
      return;
    }
    animate()
        .alpha(0f)
        .translationY(-slideOffset())
        .setDuration(HIDE_MS)
        .withEndAction(() -> setVisibility(GONE))
        .start();
  }

  private void reveal() {
    if (getVisibility() == VISIBLE && getAlpha() == 1f) {
      return;
    }
    animate().cancel();
    setVisibility(VISIBLE);
    setAlpha(0f);
    setTranslationY(-slideOffset());
    animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(REVEAL_MS)
        .setInterpolator(new DecelerateInterpolator())
        .start();
  }

  private void enterRunning(boolean indeterminate) {
    resetActions(true);
    errorState = false;
    applyBackground(null);
    binding.syncConfirm.setVisibility(GONE);
    binding.syncCancel.setVisibility(GONE);
    binding.syncCopy.setVisibility(GONE);
    binding.syncClose.setVisibility(GONE);
    binding.syncProgress.setVisibility(VISIBLE);
    startSpin();
    applyTheme();
    if (indeterminate) {
      setIndeterminate(true);
    }
  }

  private void onConfirmClick() {
    Runnable action = confirmAction;
    confirmAction = null;
    enterRunning(true);
    if (action != null) {
      action.run();
    }
  }

  private void onCancelClick() {
    Runnable action = cancelAction;
    cancelAction = null;
    hide();
    if (action != null) {
      action.run();
    }
  }

  private void copyError() {
    CharSequence message = binding.syncMessage.getText();
    if (message == null || message.length() == 0) {
      return;
    }
    ClipboardManager clipboard =
        (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard == null) {
      return;
    }
    CharSequence label = binding.syncTitle.getText();
    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            label == null ? getContext().getString(R.string.sync_copy_error) : label, message));
    GhostToast.makeText(getContext(), R.string.sync_error_copied, GhostToast.LENGTH_SHORT).show();
  }

  private void resetActions(boolean clearCallbacks) {
    if (clearCallbacks) {
      confirmAction = null;
      cancelAction = null;
    }
  }

  private void startSpin() {
    if (iconSpin != null && iconSpin.isRunning()) {
      return;
    }
    iconSpin = ObjectAnimator.ofFloat(binding.syncIcon, View.ROTATION, 0f, 360f);
    iconSpin.setDuration(1400L);
    iconSpin.setRepeatCount(ObjectAnimator.INFINITE);
    iconSpin.setInterpolator(new LinearInterpolator());
    iconSpin.start();
  }

  private void stopSpin() {
    if (iconSpin != null) {
      iconSpin.cancel();
      iconSpin = null;
    }
    binding.syncIcon.setRotation(0f);
  }

  private float slideOffset() {
    return -20f * getResources().getDisplayMetrics().density;
  }

  /** Re-reads the Material colors; call after a theme change. */
  public void applyTheme() {
    applyBackground(errorState ? M3Theme.errorContainer() : null);

    Integer primary = M3Theme.primary();
    if (errorState) {
      int onError = resolve(M3Theme.onErrorContainer(), Color.WHITE);
      binding.syncTitle.setTextColor(onError);
      binding.syncMessage.setTextColor(onError);
      binding.syncIcon.setImageTintList(ColorStateList.valueOf(resolve(M3Theme.error(), onError)));
      bindCopyCloseTint(onError);
    } else {
      M3Theme.text(binding.syncTitle, binding.syncMessage);
      binding.syncIcon.setImageTintList(
          ColorStateList.valueOf(primary != null ? primary : Color.LTGRAY));
      bindCopyCloseTint(primary != null ? primary : Color.LTGRAY);
    }
    M3Theme.progress(binding.syncProgress);
    M3Theme.button(binding.syncConfirm, binding.syncCancel);
  }

  private void bindCopyCloseTint(int color) {
    ColorStateList tint = ColorStateList.valueOf(color);
    binding.syncCopy.setImageTintList(tint);
    binding.syncClose.setImageTintList(tint);
  }

  private void applyBackground(@Nullable Integer color) {
    if (color != null) {
      setBackgroundColor(color);
      return;
    }
    Integer surface = M3Theme.surfaceContainerHigh();
    setBackgroundColor(surface != null ? surface : Color.DKGRAY);
  }

  private int resolve(@Nullable Integer color, int fallback) {
    return color != null ? color : fallback;
  }
}
