package ir.hanzodev1375.components.input;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;

/**
 * EditText for {@link TextInputGlass}.
 *
 * <p>The Material underline is removed so the glass panel behind the field stays visible. Text and
 * hint colors are applied by {@code M3Theme.apply(...)} like every other input in the project.
 */
public class TextInputEditTextGlass extends TextInputEditText {

  public TextInputEditTextGlass(@NonNull Context context) {
    super(context);
    init();
  }

  public TextInputEditTextGlass(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public TextInputEditTextGlass(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setBackground(null);
  }
}
