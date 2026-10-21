package ir.hanzodev1375.ghostide.mvvm.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.jgit.GitHubClient;

/**
 * Owns the GitHub star check and star request for the main screen.
 *
 * <p>The star check is attempted at most once per signed-in account. A prompt is emitted while the
 * repository is not starred and is marked as shown once the screen presents it.
 */
public class GitHubStarViewModel extends AndroidViewModel {

  public static final String REPOSITORY_URL = "https://github.com/HanzoDev30/Ghostide";

  private static final String OWNER = "HanzoDev30";
  private static final String REPOSITORY = "Ghostide";

  public enum PromptType {
    NOT_STARRED,
    STAR_SUCCESS,
    STAR_FAILURE
  }

  public static final class PromptEvent {

    private final PromptType type;
    private boolean handled;

    private PromptEvent(PromptType type) {
      this.type = type;
    }

    public PromptType consume() {
      if (handled) {
        return null;
      }
      handled = true;
      return type;
    }
  }

  private final MutableLiveData<PromptEvent> promptEvent = new MutableLiveData<>();
  private final GitHubClient gitHubClient;
  private final PreferencesUtils preferences;

  private volatile boolean checkStarted;
  private volatile boolean starRequestInFlight;

  public GitHubStarViewModel(Application application) {
    super(application);
    gitHubClient = new GitHubClient(application);
    preferences = new PreferencesUtils(application);
  }

  public LiveData<PromptEvent> getPromptEvent() {
    return promptEvent;
  }

  public void checkStarStatus() {
    if (checkStarted || !gitHubClient.isLoggedIn() || preferences.isGitHubStarPromptShown()) {
      return;
    }
    checkStarted = true;
    gitHubClient.isRepositoryStarred(
        OWNER,
        REPOSITORY,
        new GitHubClient.GitHubStarStatusCallback() {
          @Override
          public void onStarred() {
            preferences.setGitHubStarPromptShown();
          }

          @Override
          public void onNotStarred() {
            promptEvent.postValue(new PromptEvent(PromptType.NOT_STARRED));
          }

          @Override
          public void onFailure(String errorMessage) {
            checkStarted = false;
          }
        });
  }

  public void markPromptShown() {
    preferences.setGitHubStarPromptShown();
  }

  public void starRepository() {
    if (starRequestInFlight) {
      return;
    }
    if (!gitHubClient.isLoggedIn()) {
      promptEvent.postValue(new PromptEvent(PromptType.STAR_FAILURE));
      return;
    }
    starRequestInFlight = true;
    gitHubClient.starRepository(
        OWNER,
        REPOSITORY,
        new GitHubClient.GitHubActionCallback() {
          @Override
          public void onSuccess() {
            starRequestInFlight = false;
            promptEvent.postValue(new PromptEvent(PromptType.STAR_SUCCESS));
          }

          @Override
          public void onFailure(String errorMessage) {
            starRequestInFlight = false;
            promptEvent.postValue(new PromptEvent(PromptType.STAR_FAILURE));
          }
        });
  }
}
