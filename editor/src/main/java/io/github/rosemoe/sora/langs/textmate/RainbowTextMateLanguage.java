package io.github.rosemoe.sora.langs.textmate;

import android.content.Context;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration;

/**
 * {@link TextMateLanguage} ای که آنالایزرش {@link RainbowTextMateAnalyzer} است تا براکت ها رنگین
 * کمانی شوند. چون در همان پکیج سورا است، فیلد پکیج-پرایوت {@code textMateAnalyzer} را جایگزین می
 * کند و بقیه ی امکانات (مثل auto-complete شناسه ها) دست نمی خورند.
 */
public class RainbowTextMateLanguage extends TextMateLanguage {

  private RainbowTextMateLanguage(
      IGrammar grammar,
      LanguageConfiguration configuration,
      GrammarRegistry grammarRegistry,
      ThemeRegistry themeRegistry,
      boolean collectIdentifiers,
      Context context,
      String scope) {
    super(grammar, configuration, grammarRegistry, themeRegistry, collectIdentifiers);
    this.textMateAnalyzer =
        new RainbowTextMateAnalyzer(this, grammar, configuration, themeRegistry, context, scope);
  }

  public static RainbowTextMateLanguage create(Context context, String scope) {
    GrammarRegistry grammarRegistry = GrammarRegistry.getInstance();
    IGrammar grammar = grammarRegistry.findGrammar(scope);
    if (grammar == null) {
      return null;
    }
    LanguageConfiguration configuration = grammarRegistry.findLanguageConfiguration(scope);
    ThemeRegistry themeRegistry = ThemeRegistry.getInstance();
    return new RainbowTextMateLanguage(
        grammar, configuration, grammarRegistry, themeRegistry, true, context, scope);
  }
}
