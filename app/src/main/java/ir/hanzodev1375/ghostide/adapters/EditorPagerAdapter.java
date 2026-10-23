package ir.hanzodev1375.ghostide.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.fragments.EditorFragment;
import ir.hanzodev1375.ghostide.models.TabModel;
import ir.hanzodev1375.ghostide.project.android.AndroidStudioProjectFragment;

public class EditorPagerAdapter extends FragmentStateAdapter {
  private final List<TabModel> tabs = new ArrayList<>();
  private String wizardSavePath;

  public EditorPagerAdapter(@NonNull FragmentActivity fa, List<TabModel> initialTabs) {
    super(fa);
    if (initialTabs != null) {
      tabs.addAll(initialTabs);
    }
  }

  public void setTabs(List<TabModel> newTabs) {
    tabs.clear();
    tabs.addAll(newTabs);
    notifyDataSetChanged();
  }

  public void setWizardSavePath(String wizardSavePath) {
    this.wizardSavePath = wizardSavePath;
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    TabModel tab = tabs.get(position);
    if (tab != null && AndroidStudioProjectFragment.VIRTUAL_PATH.equals(tab.getFilePath())) {
      return AndroidStudioProjectFragment.newInstance(wizardSavePath);
    }
    return EditorFragment.newInstance(tab.getFilePath(), tab.isReadOnly());
  }

  @Override
  public int getItemCount() {
    return tabs.size();
  }

  @Override
  public long getItemId(int position) {
    return tabs.get(position).getFilePath().hashCode();
  }

  @Override
  public boolean containsItem(long itemId) {
    for (TabModel tab : tabs) {
      if (tab.getFilePath().hashCode() == itemId) {
        return true;
      }
    }
    return false;
  }

  public Fragment getFragmentAtPosition(int position, FragmentActivity activity) {
    long itemId = getItemId(position);  
    String tag = "f" + itemId;
    return activity.getSupportFragmentManager().findFragmentByTag(tag);
  }
}
