package ir.theme;

import com.google.gson.Gson;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Decodes a {@code .gth} file into the small color set needed to paint a preview canvas. */
public final class GthDecoder {

  private static final Gson GSON = new Gson();

  private GthDecoder() {}

  public static Result decode(File file) {
    if (file == null || !file.isFile()) {
      return Result.empty();
    }
    try {
      String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      GhostTheme theme = GSON.fromJson(json, GhostTheme.class);
      if (theme == null) {
        return Result.empty();
      }

      Integer primary = null;
      Integer secondary = null;
      Integer tertiary = null;

      MaterialTheme material = theme.getMaterial3();
      if (material != null) {
        primary = M3Theme.color(material.getPrimary());
        secondary = M3Theme.color(material.getSecondary());
        tertiary = M3Theme.color(material.getTertiary());
      }

      WidgetTheme widget = theme.getWidget();
      if (primary == null && widget != null) {
        primary = M3Theme.color(widget.getAccent());
      }

      String imagePath = null;
      if (widget != null && widget.getImagepath() != null && !widget.getImagepath().isEmpty()) {
        imagePath = ThemeMediaPath.resolve(file.getAbsolutePath(), widget.getImagepath());
      }

      return new Result(primary, secondary, tertiary, imagePath);
    } catch (Exception e) {
      return Result.empty();
    }
  }

  /** Colors may be {@code null} when the file is empty, corrupt or uses an old format. */
  public static final class Result {

    private final Integer primary;
    private final Integer secondary;
    private final Integer tertiary;
    private final String imagePath;

    private Result(
        Integer primary, Integer secondary, Integer tertiary, String imagePath) {
      this.primary = primary;
      this.secondary = secondary;
      this.tertiary = tertiary;
      this.imagePath = imagePath;
    }

    static Result empty() {
      return new Result(null, null, null, null);
    }

    public Integer getPrimary() {
      return primary;
    }

    public Integer getSecondary() {
      return secondary;
    }

    public Integer getTertiary() {
      return tertiary;
    }

    public String getImagePath() {
      return imagePath;
    }

    public boolean hasImage() {
      return imagePath != null && !imagePath.isEmpty();
    }
  }
}
