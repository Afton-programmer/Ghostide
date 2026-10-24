package ir.hanzodev1375.ghostide.project.android;

import ir.hanzodev1375.ghostide.project.DslType;

public class AndroidProjectConfig {

  public enum Language {
    KOTLIN("Kotlin", ".kt"),
    JAVA("Java", ".java");

    private final String displayName;
    private final String fileExtension;

    Language(String displayName, String fileExtension) {
      this.displayName = displayName;
      this.fileExtension = fileExtension;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getFileExtension() {
      return fileExtension;
    }
  }

  private String projectName;
  private String packageName;
  private String parentPath;
  private AndroidCategory category = AndroidCategory.PHONE_TABLET;
  private AndroidTemplate template = AndroidTemplate.PHONE_EMPTY_VIEWS;
  private Language language = Language.KOTLIN;
  private DslType dslType = DslType.KOTLIN;
  private int minSdk = 24;
  private int targetSdk = 34;
  private int compileSdk = 37;

  public AndroidProjectConfig() {}

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getParentPath() {
    return parentPath;
  }

  public void setParentPath(String parentPath) {
    this.parentPath = parentPath;
  }

  public AndroidCategory getCategory() {
    return category;
  }

  public void setCategory(AndroidCategory category) {
    this.category = category;
  }

  public AndroidTemplate getTemplate() {
    return template;
  }

  public void setTemplate(AndroidTemplate template) {
    this.template = template;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public DslType getDslType() {
    return dslType;
  }

  public void setDslType(DslType dslType) {
    this.dslType = dslType;
  }

  public int getMinSdk() {
    return minSdk;
  }

  public void setMinSdk(int minSdk) {
    this.minSdk = minSdk;
  }

  public int getTargetSdk() {
    return targetSdk;
  }

  public void setTargetSdk(int targetSdk) {
    this.targetSdk = targetSdk;
  }

  public int getCompileSdk() {
    return compileSdk;
  }

  public void setCompileSdk(int compileSdk) {
    this.compileSdk = compileSdk;
  }
}
