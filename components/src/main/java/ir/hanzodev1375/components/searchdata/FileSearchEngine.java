package ir.hanzodev1375.components.searchdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ir.hanzodev1375.components.searchdata.model.ContentMatch;
import ir.hanzodev1375.components.searchdata.model.FileSearchResult;
import ir.hanzodev1375.components.searchdata.model.SearchMode;
import ir.hanzodev1375.components.searchdata.model.SearchQuery;
import ir.hanzodev1375.components.searchdata.model.SearchType;
import ir.hanzodev1375.components.searchdata.utils.GlobExcluder;

public class FileSearchEngine {
  private static final int MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;
  private GlobExcluder excluder = new GlobExcluder(null);
  private static final String[] TEXT_EXTENSIONS = {
    ".java",
    ".kt",
    ".xml",
    ".json",
    ".js",
    ".ts",
    ".html",
    ".css",
    ".py",
    ".c",
    ".cpp",
    ".h",
    ".hpp",
    ".gradle",
    ".md",
    ".txt",
    ".yaml",
    ".yml",
    ".properties",
    ".sh",
    ".bat",
    ".rb",
    ".go",
    ".rs",
    ".dart",
    ".php",
    ".sql",
    ".swift",
    ".lua",
    ".g4",
    ".kts",
    ".toml",
    ".groovy",
    ".tsx",
    ".jsx",
    ".scss",
    ".sass"
  };

  public interface SearchCallback {
    void onResult(FileSearchResult result);

    void onComplete(int totalFound);

    void onError(String message);
  }

  public interface ReplaceCallback {
    void onComplete(int replacedCount, int changedFiles);

    void onError(String message);
  }

  /** Replaces every match of {@code query} with {@code replacement} inside matched text files. */
  public void replace(
      SearchQuery query,
      String replacement,
      GlobExcluder excluder,
      AtomicBoolean cancelled,
      ReplaceCallback callback) {
    if (query == null || !query.isValid() || replacement == null) {
      callback.onError("Invalid query");
      return;
    }
    if (query.getType() != SearchType.FILE_CONTENT) {
      callback.onError("Replace is only available for content search");
      return;
    }
    if (excluder != null) this.excluder = excluder;
    Pattern pattern = buildPattern(query);
    if (pattern == null) {
      callback.onError("Invalid regex pattern");
      return;
    }
    File root = new File(query.getRootPath());
    if (!root.exists() || !root.isDirectory()) {
      callback.onError("Invalid directory");
      return;
    }
    String effectiveReplacement =
        query.getMode() == SearchMode.REGEX
            ? replacement
            : Matcher.quoteReplacement(replacement);
    int[] counts = {0, 0};
    replaceRecursive(root, pattern, effectiveReplacement, cancelled, counts);
    if (!cancelled.get()) callback.onComplete(counts[0], counts[1]);
  }

  private void replaceRecursive(
      File dir,
      Pattern pattern,
      String replacement,
      AtomicBoolean cancelled,
      int[] counts) {
    if (cancelled.get()) return;
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File file : files) {
      if (cancelled.get()) return;
      if (file.isDirectory()) {
        if (excluder.isExcluded(file.getAbsolutePath())) continue;
        replaceRecursive(file, pattern, replacement, cancelled, counts);
      } else {
        if (excluder.isExcluded(file.getAbsolutePath())) continue;
        replaceInFile(file, pattern, replacement, counts);
      }
    }
  }

  private void replaceInFile(File file, Pattern pattern, String replacement, int[] counts) {
    if (!isTextFile(file.getName()) || file.length() > MAX_FILE_SIZE_BYTES) return;
    try {
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      Matcher matcher = pattern.matcher(content);
      int found = 0;
      while (matcher.find()) found++;
      if (found == 0) return;
      String updated = pattern.matcher(content).replaceAll(replacement);
      if (updated.equals(content)) return;
      Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));
      counts[0] += found;
      counts[1]++;
    } catch (IOException e) {
      // Read-only or non-UTF-8 file: leave it untouched and keep going.
    }
  }

  public void search(SearchQuery query, AtomicBoolean cancelled, SearchCallback callback) {
    search(query, null, cancelled, callback);
  }

  public void search(
      SearchQuery query,
      GlobExcluder excluder,
      AtomicBoolean cancelled,
      SearchCallback callback) {
    if (excluder != null) this.excluder = excluder;
    if (!query.isValid()) {
      callback.onError("Invalid query");
      return;
    }
    Pattern pattern = buildPattern(query);
    if (pattern == null) {
      callback.onError("Invalid regex pattern");
      return;
    }
    File root = new File(query.getRootPath());
    if (!root.exists() || !root.isDirectory()) {
      callback.onError("Invalid directory");
      return;
    }
    int[] count = {0};
    searchRecursive(root, query, pattern, cancelled, callback, count);
    if (!cancelled.get()) callback.onComplete(count[0]);
  }

  private void searchRecursive(
      File dir,
      SearchQuery query,
      Pattern pattern,
      AtomicBoolean cancelled,
      SearchCallback callback,
      int[] count) {
    if (cancelled.get()) return;
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File file : files) {
      if (cancelled.get()) return;
      if (file.isDirectory()) {
        if (excluder.isExcluded(file.getAbsolutePath())) continue;
        searchRecursive(file, query, pattern, cancelled, callback, count);
      } else {
        if (excluder.isExcluded(file.getAbsolutePath())) continue;
        FileSearchResult result = matchFile(file, query, pattern);
        if (result != null) {
          count[0]++;
          callback.onResult(result);
        }
      }
    }
  }

  private FileSearchResult matchFile(File file, SearchQuery query, Pattern pattern) {
    String name = file.getName();
    String path = file.getAbsolutePath();
    if (query.getType() == SearchType.FILE_NAME) {
      if (pattern.matcher(name).find()) return new FileSearchResult(name, path, null);
    } else {
      if (!isTextFile(name) || file.length() > MAX_FILE_SIZE_BYTES) return null;
      List<ContentMatch> matches = searchInFile(file, pattern);
      if (!matches.isEmpty()) return new FileSearchResult(name, path, matches);
    }
    return null;
  }

  private List<ContentMatch> searchInFile(File file, Pattern pattern) {
    List<ContentMatch> matches = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      int lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        Matcher m = pattern.matcher(line);
        List<int[]> ranges = new ArrayList<>();
        while (m.find()) ranges.add(new int[] {m.start(), m.end()});
        if (!ranges.isEmpty()) matches.add(new ContentMatch(lineNumber, line, ranges));
        lineNumber++;
      }
    } catch (IOException ignored) {
    }
    return matches;
  }

  private Pattern buildPattern(SearchQuery query) {
    try {
      String q = query.getQuery();
      switch (query.getMode()) {
        case REGEX:
          return Pattern.compile(q);
        case CASE_SENSITIVE:
          return Pattern.compile(Pattern.quote(q));
        default:
          return Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
      }
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isTextFile(String name) {
    String lower = name.toLowerCase();
    for (String ext : TEXT_EXTENSIONS) if (lower.endsWith(ext)) return true;
    return false;
  }
}
