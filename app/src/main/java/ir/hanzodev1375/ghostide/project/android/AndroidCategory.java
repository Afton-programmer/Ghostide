package ir.hanzodev1375.ghostide.project.android;

public enum AndroidCategory {
  PHONE_TABLET("Phone & Tablet"),
  WEAR_OS("Wear OS"),
  ANDROID_TV("Android TV"),
  AUTOMOTIVE("Automotive");

  private final String displayName;

  AndroidCategory(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
