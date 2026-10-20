package ir.hanzodev1375.components.searchdata.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.databinding.BottomSheetSearchBinding;
import ir.hanzodev1375.components.searchdata.adapter.SearchResultAdapter;
import ir.hanzodev1375.components.searchdata.interfaces.OnLineClickListener;
import ir.hanzodev1375.components.searchdata.model.FileSearchResult;
import ir.hanzodev1375.components.searchdata.model.SearchMode;
import ir.hanzodev1375.components.searchdata.model.SearchQuery;
import ir.hanzodev1375.components.searchdata.model.SearchType;
import ir.hanzodev1375.components.searchdata.viewmodel.SearchViewModel;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.theme.M3Theme;
import java.util.List;
import java.util.regex.Pattern;

public class SearchBottomSheet extends BaseBlurBottomSheet {

  public static final String TAG = "SearchBottomSheet";
  private static final String ARG_ROOT_PATH = "root_path";
  private BottomSheetSearchBinding binding;
  private SearchViewModel viewModel;
  private SearchResultAdapter adapter;
  private OnLineClickListener lineClickListener;

  public static SearchBottomSheet newInstance(String rootPath) {
    SearchBottomSheet sheet = new SearchBottomSheet();
    Bundle args = new Bundle();
    args.putString(ARG_ROOT_PATH, rootPath);
    sheet.setArguments(args);
    return sheet;
  }

  public void setOnLineClickListener(OnLineClickListener l) {
    this.lineClickListener = l;
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    binding = BottomSheetSearchBinding.inflate(getLayoutInflater(), contentContainer, false);
    contentContainer.addView(binding.getRoot());
    viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
    setupRecyclerView();
    setupSearchBar();
    setupModeToggle();
    setupTypeSwitch();
    observeViewModel();
    M3Theme.applyTopLevel(binding.getRoot());
    M3Theme.toggleGroup(binding.toggleSearchMode);
  }

  private void setupRecyclerView() {
    adapter = new SearchResultAdapter();
    adapter.setListener(
        new OnLineClickListener() {
          @Override
          public void onLineClick(String filePath, int lineNumber) {
            if (lineClickListener != null) {
              lineClickListener.onLineClick(filePath, lineNumber);
              dismiss();
            }
          }

          @Override
          public void onFileClick(FileSearchResult result) {
            if (lineClickListener != null) {
              lineClickListener.onFileClick(result);
              dismiss();
            }
          }
        });
    binding.rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.rvResults.setAdapter(adapter);
    binding.rvResults.setHasFixedSize(false);
  }

  private void setupSearchBar() {
    binding.etSearch.addTextChangedListener(
        new android.text.TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void afterTextChanged(android.text.Editable s) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            binding.btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            if (s.length() == 0) {
              viewModel.cancelSearch();
              adapter.clear();
              binding.tvResultCount.setVisibility(View.GONE);
              binding.layoutEmpty.setVisibility(View.GONE);
            }
          }
        });
    binding.btnClear.setOnClickListener(v -> binding.etSearch.setText(""));
    binding.btnSearch.setOnClickListener(v -> triggerSearch());
    binding.btnReplaceAll.setOnClickListener(v -> confirmReplace());
    binding.btnExclude.setOnClickListener(
        v -> new ExcludedSearchSheet().show(getChildFragmentManager(), ExcludedSearchSheet.TAG));
    binding.etSearch.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            triggerSearch();
            return true;
          }
          return false;
        });
  }

  private void setupModeToggle() {
    binding.toggleSearchMode.addOnButtonCheckedListener(
        (group, checkedId, isChecked) -> {
          if (isChecked && !binding.etSearch.getText().toString().isEmpty()) triggerSearch();
        });
    binding.btnModeNormal.setChecked(true);
  }

  private void setupTypeSwitch() {
    binding.switchContentSearch.setOnCheckedChangeListener(
        (btn, isChecked) -> {
          binding.tvSwitchLabel.setText(
              isChecked
                  ? getString(R.string.search_type_content)
                  : getString(R.string.search_type_filename));
          binding.cardReplace.setVisibility(isChecked ? View.VISIBLE : View.GONE);
          if (!binding.etSearch.getText().toString().isEmpty()) triggerSearch();
        });
    binding.cardReplace.setVisibility(
        binding.switchContentSearch.isChecked() ? View.VISIBLE : View.GONE);
  }

  private SearchQuery buildQuery() {
    String query = binding.etSearch.getText().toString().trim();
    String rootPath = getArguments() != null ? getArguments().getString(ARG_ROOT_PATH) : null;
    if (query.isEmpty() || rootPath == null) return null;
    SearchMode mode = getSelectedMode();
    if (mode == SearchMode.REGEX && !isValidRegex(query)) {
      showSnackbar(getString(R.string.search_error_invalid_regex));
      return null;
    }
    SearchType type =
        binding.switchContentSearch.isChecked() ? SearchType.FILE_CONTENT : SearchType.FILE_NAME;
    return new SearchQuery(query, rootPath, mode, type);
  }

  private void triggerSearch() {
    SearchQuery query = buildQuery();
    if (query == null) return;
    viewModel.startSearch(query);
  }

  private void confirmReplace() {
    SearchQuery query = buildQuery();
    if (query == null) return;
    if (query.getType() != SearchType.FILE_CONTENT) {
      showSnackbar(getString(R.string.search_replace_none));
      return;
    }
    List<FileSearchResult> results = viewModel.getResults().getValue();
    int fileCount = results != null ? results.size() : 0;
    if (fileCount == 0) {
      showSnackbar(getString(R.string.search_replace_none));
      return;
    }
    String replacement = binding.etReplace.getText().toString();
    new DialogCompat(requireContext())
        .setTitle(R.string.search_replace_confirm_title)
        .setMessage(getString(R.string.search_replace_confirm_message, fileCount))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            R.string.search_replace_all, (dialog, which) -> viewModel.replace(query, replacement))
        .show();
  }

  private SearchMode getSelectedMode() {
    int id = binding.toggleSearchMode.getCheckedButtonId();
    if (id == binding.btnModeRegex.getId()) return SearchMode.REGEX;
    if (id == binding.btnModeCase.getId()) return SearchMode.CASE_SENSITIVE;
    return SearchMode.NORMAL;
  }

  private boolean isValidRegex(String pattern) {
    try {
      Pattern.compile(pattern);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private void observeViewModel() {
    viewModel
        .getResults()
        .observe(
            getViewLifecycleOwner(),
            list -> {
              adapter.submitList(list);
              boolean isEmpty = list == null || list.isEmpty();
              SearchViewModel.State s = viewModel.getState().getValue();
              binding.layoutEmpty.setVisibility(
                  isEmpty && s == SearchViewModel.State.DONE ? View.VISIBLE : View.GONE);
              binding.rvResults.setVisibility(!isEmpty ? View.VISIBLE : View.GONE);
            });
    viewModel
        .getState()
        .observe(
            getViewLifecycleOwner(),
            s -> {
              boolean searching = s == SearchViewModel.State.SEARCHING;
              binding.progressBar.setVisibility(searching ? View.VISIBLE : View.GONE);
              if (s == SearchViewModel.State.DONE) {
                Integer count = viewModel.getResultCount().getValue();
                int c = count != null ? count : 0;
                binding.tvResultCount.setVisibility(View.VISIBLE);
                binding.tvResultCount.setText(getString(R.string.search_result_count, c));
                if (c == 0) showSnackbar(getString(R.string.search_no_results));
                else showSnackbar(getString(R.string.search_found, c));
              }
            });
    viewModel
        .getErrorMessage()
        .observe(
            getViewLifecycleOwner(),
            msg -> {
              if (msg != null && !msg.isEmpty()) showSnackbar(msg);
            });
    viewModel
        .getReplaceCount()
        .observe(
            getViewLifecycleOwner(),
            count -> {
              if (count == null) return;
              if (count < 0) {
                viewModel.clearReplaceCount();
                return;
              }
              showSnackbar(
                  count == 0
                      ? getString(R.string.search_replace_none)
                      : getString(R.string.search_replace_done, count));
              if (count > 0) triggerSearch();
              viewModel.clearReplaceCount();
            });
  }

  private void showSnackbar(String message) {
    if (getView() != null) {
      Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    viewModel.cancelSearch();
    viewModel.clearReplaceCount();
    binding = null;
  }
}
