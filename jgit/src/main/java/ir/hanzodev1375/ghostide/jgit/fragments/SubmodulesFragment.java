package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.hanzodev1375.components.views.EmptyView;
import ir.hanzodev1375.ghostide.jgit.GitHubClient;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.adapter.SubmoduleAdapter;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.theme.M3Theme;

public class SubmodulesFragment extends Fragment {

  private static final String OAUTH_USER = "oauth2";

  private GitViewModel viewModel;
  private SubmoduleAdapter adapter;
  private GitHubClient gitHubClient;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_submodules, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);
    gitHubClient = new GitHubClient(requireContext());

    RecyclerView recyclerView = view.findViewById(R.id.recyclerViewSubmodules);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setHasFixedSize(true);
    adapter = new SubmoduleAdapter();
    recyclerView.setAdapter(adapter);
    ((EmptyView) view.findViewById(R.id.emptyView)).bindTo(recyclerView);
    M3Theme.apply(view);

    viewModel.submodules.observe(getViewLifecycleOwner(), adapter::submitList);
    viewModel.refreshSubmodules();

    view.findViewById(R.id.btnInitSubmodules).setOnClickListener(v -> viewModel.initSubmodules());
    view.findViewById(R.id.btnSyncSubmodules).setOnClickListener(v -> viewModel.syncSubmodules());
    view.findViewById(R.id.btnUpdateSubmodules)
        .setOnClickListener(
            v -> {
              String token = token();
              viewModel.updateSubmodules(token != null ? OAUTH_USER : null, token);
            });
    view.findViewById(R.id.btnAddSubmodule)
        .setOnClickListener(v -> showAddSubmoduleDialog());
  }

  private String token() {
    String token = gitHubClient.getToken();
    return token != null && !token.isEmpty() ? token : null;
  }

  private void showAddSubmoduleDialog() {
    View content =
        LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_submodule, null);
    EditText editUrl = content.findViewById(R.id.editSubmoduleUrl);
    EditText editPath = content.findViewById(R.id.editSubmodulePath);
    EditText editName = content.findViewById(R.id.editSubmoduleName);

    new DialogCompat(requireContext())
        .setTitle(getString(R.string.submodule_add_title))
        .setView(content)
        .setPositiveButton(
            getString(R.string.submodule_add),
            (d, w) -> {
              String url = text(editUrl);
              String path = text(editPath);
              String name = text(editName);
              if (url.isEmpty()) {
                editUrl.setError(getString(R.string.submodule_url_required));
                return;
              }
              if (path.isEmpty()) {
                editPath.setError(getString(R.string.submodule_path_required));
                return;
              }
              String token = token();
              viewModel.addSubmodule(
                  url,
                  path,
                  name.isEmpty() ? null : name,
                  token != null ? OAUTH_USER : null,
                  token);
            })
        .setNegativeButton(getString(R.string.cancel), null)
        .show();
  }

  private static String text(EditText edit) {
    return edit.getText() != null ? edit.getText().toString().trim() : "";
  }
}
