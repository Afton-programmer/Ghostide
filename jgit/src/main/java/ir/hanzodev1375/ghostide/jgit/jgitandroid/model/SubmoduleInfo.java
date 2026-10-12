package ir.hanzodev1375.ghostide.jgit.jgitandroid.model;

public class SubmoduleInfo {

  public enum State {
    MISSING,
    UNINITIALIZED,
    INITIALIZED,
    REV_CHECKED_OUT
  }

  private final String path;
  private final String url;
  private final State state;
  private final String indexCommit;
  private final String headCommit;

  public SubmoduleInfo(
      String path, String url, State state, String indexCommit, String headCommit) {
    this.path = path;
    this.url = url;
    this.state = state;
    this.indexCommit = indexCommit;
    this.headCommit = headCommit;
  }

  public String getPath() {
    return path;
  }

  public String getUrl() {
    return url;
  }

  public State getState() {
    return state;
  }

  public String getIndexCommit() {
    return indexCommit;
  }

  public String getHeadCommit() {
    return headCommit;
  }
}
