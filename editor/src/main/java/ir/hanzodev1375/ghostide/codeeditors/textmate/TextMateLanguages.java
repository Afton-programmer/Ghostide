package ir.hanzodev1375.ghostide.codeeditors.textmate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.textmate.RainbowTextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlLanguage;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * جایگزین متناسب برای زبان های قدیمی توکنایزی.
 *
 * <p>Html جدا نگه داشته می شود چون گرامر TextMate آن ناقص است؛ بقیه زبان هایی که گرامرشان داخل
 * assets هست با TextMate ساخته می شوند و آن هایی که گرامر ندارند skip می شوند.
 *
 * <p>اگر {@code overrideScope} داده شود (اسکوپی که یک افزونه مثل provider های LSP اعلام می کند)
 * همان اسکوپ بر نگاشت پسوند مقدم می شود تا گرامر خارج از languages.json هم رنگ بگیرد.
 */
public final class TextMateLanguages {

  /** گرامرهای ثبت‌شده توسط پلاگین ها بعد از languages.json بار می شوند؛ چند بار تلاش می کنیم. */
  private static final int PLUGIN_SCOPE_RETRIES = 40;

  private static final long PLUGIN_SCOPE_RETRY_MS = 150L;

  /** برای اسکوپ های عادی هم چند بار تلاش می کنیم تا آماده شدن گذرای گرامر/تم از دست نرود. */
  private static final int SCOPE_RETRIES = 10;

  private TextMateLanguages() {}

  public static boolean isHtml(String filePath) {
    if (filePath == null) {
      return false;
    }
    String ext = extensionOf(filePath);
    return ext.equals("html") || ext.equals("htm");
  }

  /**
   * نسخه ی همزمان که هیچ گاه ترد را بلاک نمی کند؛ اگر گرامرها آماده نباشند null برمی گرداند. برای
   * بارگذاری درست از {@link #resolveAsync} استفاده کنید.
   */
  public static Language resolve(Context context, String filePath) {
    return resolve(context, filePath, null);
  }

  /** مثل {@link #resolve(Context, String)} ولی با اسکوپ صریح که بر نگاشت پسوند مقدم است. */
  public static Language resolve(Context context, String filePath, String overrideScope) {
    if (context != null) {
      TextMateGrammars.ensureStarted(context);
    }
    if (isHtml(filePath)) {
      return new HtmlLanguage(context, filePath);
    }
    String scope = scopeFor(filePath, overrideScope);
    if (scope == null) {
      return null;
    }
    if (TextMateGrammars.getInitError() != null || !TextMateGrammars.isReady()) {
      return null;
    }
    if (GrammarRegistry.getInstance().findGrammar(scope) == null) {
      return null;
    }
    return createRainbow(context, scope);
  }

  /** زبان TextMate با آنالایزر رنگین کمانی؛ اگر نسخه ی سفارشی نساخت به نسخه ی سورا برمی گردد. */
  private static Language createRainbow(Context context, String scope) {
    try {
      RainbowTextMateLanguage rainbow = RainbowTextMateLanguage.create(context, scope);
      if (rainbow != null) {
        return rainbow;
      }
    } catch (RuntimeException ignored) {
    }
    return TextMateLanguage.create(scope, true);
  }

  /**
   * نسخه ی غیر بلاک کننده؛ اگر گرامرها آماده نباشند منتظر می ماند و کال بک را روی ترد اصلی صدا می
   * زند. اگر زبان پشتیبانی نشود کال بک با null فراخوانی می شود.
   */
  public static void resolveAsync(Context context, String filePath, Consumer<Language> callback) {
    resolveAsync(context, filePath, null, callback);
  }

  /** مثل {@link #resolveAsync(Context, String, Consumer)} ولی با اسکوپ صریح. */
  public static void resolveAsync(
      Context context, String filePath, String overrideScope, Consumer<Language> callback) {
    if (context != null) {
      TextMateGrammars.ensureStarted(context);
    }
    Handler handler = new Handler(Looper.getMainLooper());
    if (isHtml(filePath)) {
      handler.post(() -> callback.accept(new HtmlLanguage(context, filePath)));
      return;
    }
    String scope = scopeFor(filePath, overrideScope);
    if (scope == null) {
      handler.post(() -> callback.accept(null));
      return;
    }
    // اسکوپ پلاگین ممکن است چند صد میلی ثانیه دیرتر از languages.json ثبت شود.
    // برای اسکوپ های عادی هم چند بار تلاش می کنیم تا یک نبود گذرا باعث بی رنگ شدن دائمی نشود.
    boolean pluginScope = overrideScope != null && !overrideScope.isEmpty();
    int retries = pluginScope ? PLUGIN_SCOPE_RETRIES : SCOPE_RETRIES;
    TextMateGrammars.whenReady(
        () -> {
          // حتی اگر initError باشد هم تلاش می کنیم؛ ممکن است گرامر همین اسکوپ قبل از خطا لود شده
          // باشد.
          awaitGrammar(context, handler, scope, retries, false, callback);
        });
  }

  public static boolean isSupported(String filePath) {
    if (isHtml(filePath)) {
      return true;
    }
    String scope = TextMateScopeMap.scopeOf(filePath);
    return scope != null && TextMateGrammars.hasScope(scope);
  }

  private static void awaitGrammar(
      Context context,
      Handler handler,
      String scope,
      int retriesLeft,
      boolean loadRetried,
      Consumer<Language> callback) {
    if (GrammarRegistry.getInstance().findGrammar(scope) != null) {
      try {
        callback.accept(createRainbow(context, scope));
        return;
      } catch (RuntimeException e) {
        // ساخت زبان ممکن است به خاطر آماده نبودن تم به صورت گذرا شکست بخورد؛ دوباره تلاش می کنیم.
        if (retriesLeft <= 0) {
          callback.accept(null);
          return;
        }
      }
    } else {
      // نبود گرامر می تواند به خاطر خطای بارگذاری اولیه باشد؛ یک بار کل بارگذاری را دوباره شروع
      // می کنیم تا در همین باز شدن فایل هم هایلایت برگردد.
      if (!loadRetried && context != null && TextMateGrammars.getInitError() != null) {
        loadRetried = true;
        TextMateGrammars.ensureStarted(context);
      }
      if (retriesLeft <= 0) {
        callback.accept(null);
        return;
      }
    }
    final boolean loadRetriedNext = loadRetried;
    handler.postDelayed(
        () -> awaitGrammar(context, handler, scope, retriesLeft - 1, loadRetriedNext, callback),
        PLUGIN_SCOPE_RETRY_MS);
  }

  private static String scopeFor(String filePath, String overrideScope) {
    if (overrideScope != null && !overrideScope.isEmpty()) {
      return overrideScope;
    }
    return TextMateScopeMap.scopeOf(filePath);
  }

  private static String extensionOf(String filePath) {
    int slash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
    String name = slash >= 0 ? filePath.substring(slash + 1) : filePath;
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return "";
    }
    return name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
