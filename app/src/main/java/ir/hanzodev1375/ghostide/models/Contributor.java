package ir.hanzodev1375.ghostide.models;

/** A GitHub contributor shown on the About screen. */
public final class Contributor {

  private final String username;
  private final String avatarUrl;

  public Contributor(String username, String avatarUrl) {
    this.username = username;
    this.avatarUrl = avatarUrl;
  }

  public String getUsername() {
    return username;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }
}
