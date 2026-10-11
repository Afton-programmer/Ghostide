package io.github.rosemoe.sora.langs.textmate;

import android.content.Context;
import io.github.rosemoe.sora.lang.analysis.IncrementalAnalyzeManager.LineTokenizeResult;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.text.ContentReference;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.rainbow.BracketSyntax;
import ir.hanzodev1375.ghostide.codeeditors.rainbow.BracketSyntaxLoader;
import ir.hanzodev1375.ghostide.codeeditors.rainbow.RainbowBracketScanner;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration;

/**
 * آنالایزر TextMate که براکت ها را با رنگ زیرین span رنگین کمانی می کند.
 *
 * <p>span های TextMate همان چیزی هستند که ادیتور برای رنگ «خود گلیف» رندر می کند؛ برخلاف {@code
 * HighlightText} که فقط پس زمینه می کشد. عمق براکت ها از {@link RainbowBracketScanner} گرفته می شود
 * که کامنت/رشته و جفت براکت ها را از language-configuration.json می خواند.
 */
public class RainbowTextMateAnalyzer extends TextMateAnalyzer {

  private static final int[] COLORS = {
    GhostColorScheme.BRACKET1,
    GhostColorScheme.BRACKET2,
    GhostColorScheme.BRACKET3,
    GhostColorScheme.BRACKET4,
    GhostColorScheme.BRACKET5,
    GhostColorScheme.BRACKET6
  };

  private static final int MAX_CHARS = 3_000_000;

  private final BracketSyntax syntax;
  private RainbowBracketScanner.Result data;
  private long dataVersion = -1;

  public RainbowTextMateAnalyzer(
      TextMateLanguage language,
      IGrammar grammar,
      LanguageConfiguration configuration,
      ThemeRegistry themeRegistry,
      Context context,
      String scope) {
    super(language, grammar, configuration, themeRegistry);
    this.syntax = BracketSyntaxLoader.forScope(context.getApplicationContext(), scope, false);
  }

  @Override
  public synchronized LineTokenizeResult<MyState, Span> tokenizeLine(
      CharSequence line, MyState state, int lineIndex) {
    LineTokenizeResult<MyState, Span> result = super.tokenizeLine(line, state, lineIndex);
    if (syntax == null || result == null || result.tokens == null || result.tokens.isEmpty()) {
      return result;
    }
    RainbowBracketScanner.Result d = ensureData();
    if (d == null || d.size == 0) {
      return result;
    }
    int start = d.firstIndexAtOrAfterLine(lineIndex);
    if (start >= d.size || d.line[start] != lineIndex) {
      return result;
    }
    int end = start;
    while (end < d.size && d.line[end] == lineIndex) {
      end++;
    }
    int count = end - start;
    int[] cols = new int[count];
    int[] depths = new int[count];
    for (int i = 0; i < count; i++) {
      cols[i] = d.col[start + i];
      depths[i] = d.depth[start + i];
    }
    result.tokens = applyColors(result.tokens, line.length(), cols, depths);
    return result;
  }

  private RainbowBracketScanner.Result ensureData() {
    ContentReference ref = getContentRef();
    if (ref == null) {
      data = null;
      return null;
    }
    long version = ref.getDocumentVersion();
    if (data != null && dataVersion == version) {
      return data;
    }
    dataVersion = version;
    String text;
    try {
      text = ref.getReference().toString();
    } catch (RuntimeException e) {
      data = null;
      return null;
    }
    if (text == null || text.length() > MAX_CHARS) {
      data = null;
      return null;
    }
    data = RainbowBracketScanner.scan(text, syntax);
    return data;
  }

  private static List<Span> applyColors(
      List<Span> tokens, int lineLength, int[] cols, int[] depths) {
    List<Span> out = new ArrayList<>(tokens.size() + cols.length);
    int bi = 0;
    for (int i = 0; i < tokens.size(); i++) {
      Span span = tokens.get(i);
      int startColumn = span.getColumn();
      int endColumn =
          (i + 1 < tokens.size())
              ? tokens.get(i + 1).getColumn()
              : Math.max(startColumn, lineLength);
      if (endColumn <= startColumn) {
        out.add(span);
        continue;
      }
      long style = span.getStyle();
      int background = TextStyle.getBackgroundColorId(style);
      boolean bold = TextStyle.isBold(style);
      boolean italics = TextStyle.isItalics(style);
      boolean strike = TextStyle.isStrikeThrough(style);
      boolean noCompletion = TextStyle.isNoCompletion(style);
      int cursor = startColumn;
      while (bi < cols.length && cols[bi] < cursor) {
        bi++;
      }
      while (bi < cols.length && cols[bi] < endColumn) {
        int bracketColumn = cols[bi];
        if (bracketColumn > cursor) {
          out.add(SpanFactory.obtain(cursor, style));
        }
        int color = COLORS[Math.abs(depths[bi]) % COLORS.length];
        out.add(
            SpanFactory.obtain(
                bracketColumn,
                TextStyle.makeStyle(color, background, bold, italics, strike, noCompletion)));
        cursor = bracketColumn + 1;
        bi++;
      }
      if (cursor < endColumn) {
        out.add(SpanFactory.obtain(cursor, style));
      }
    }
    return out;
  }
}
