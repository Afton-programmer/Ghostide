package ir.hanzodev1375.ghostide.enums;

public enum SortType {
  NAME("name"),
  SIZE("size"),
  DATE("date"),
  TYPE("type");

  private final String key;

  SortType(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public static SortType fromKey(String key) {
    if (key != null) {
      for (SortType type : values()) {
        if (type.key.equals(key)) return type;
      }
    }
    return NAME;
  }
}
