package ir.hanzodev1375.ghostide.project.android;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ir.hanzodev1375.components.glass.ChipCompat;
import ir.hanzodev1375.components.glass.ChipGroupCompat;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.views.GhostToast;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.project.DslType;
import ir.theme.M3Theme;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class AndroidStudioProjectSheet extends BaseBlurBottomSheet {

  public static final String TAG = "AndroidStudioProjectSheet";
  public static final String RESULT_KEY = "android_project_created_result";
  public static final String ARG_RESULT_PATH = "result_project_path";

  private static final String ARG_SAVE_PATH = "arg_save_path";

  private String savePath;
  private final Handler handler = new Handler(Looper.getMainLooper());

  private ChipGroupCompat chipGroupCategory;
  private ChipGroupCompat chipGroupTemplates;
  private TextView tvTemplateDescription;
  private TextInputLayout tilProjectName;
  private TextInputEditText etProjectName;
  private TextInputLayout tilPackageName;
  private TextInputEditText etPackageName;
  private ChipCompat chipLangKotlin;
  private ChipCompat chipLangJava;
  private TextView tvLanguageHint;
  private ChipGroupCompat chipGroupDsl;
  private ChipGroupCompat chipGroupMinSdk;
  private TextView tvSaveLocation;
  private MaterialButton btnCreateProject;
  private MaterialButton btnCancelWizard;

  private View scrollWizardContent;
  private View layoutSyncProgress;
  private TextView tvSyncDetail;
  private TextView tvSyncPercent;
  private LinearProgressIndicator progressSync;

  private AndroidCategory selectedCategory = AndroidCategory.PHONE_TABLET;
  private AndroidTemplate selectedTemplate = AndroidTemplate.PHONE_EMPTY_VIEWS;
  private boolean userEditedPackage = false;

  public static AndroidStudioProjectSheet newInstance(String savePath) {
      AndroidStudioProjectSheet sheet = new AndroidStudioProjectSheet();
      Bundle args = new Bundle();
      args.putString(ARG_SAVE_PATH, savePath);
      sheet.setArguments(args);
      return sheet;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      if (getArguments() != null) {
          savePath = getArguments().getString(ARG_SAVE_PATH);
      }
      if (savePath == null || savePath.isEmpty()) {
          File def = new File(Environment.getExternalStorageDirectory(), "Ghostide/projects");
          if (!def.exists()) def.mkdirs();
          savePath = def.getAbsolutePath();
      }
  }

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
      View view = LayoutInflater.from(requireContext())
              .inflate(R.layout.bottom_sheet_android_project, contentContainer, false);
      contentContainer.addView(view);

      bindViews(view);
      setupListeners();
      updateCategory(AndroidCategory.PHONE_TABLET);
      tvSaveLocation.setText(getString(R.string.android_save_location, savePath));
      M3Theme.apply(view);
  }

  private void bindViews(View view) {
      scrollWizardContent = view.findViewById(R.id.scrollWizardContent);
      chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
      chipGroupTemplates = view.findViewById(R.id.chipGroupTemplates);
      tvTemplateDescription = view.findViewById(R.id.tvTemplateDescription);
      tilProjectName = view.findViewById(R.id.tilProjectName);
      etProjectName = view.findViewById(R.id.etProjectName);
      tilPackageName = view.findViewById(R.id.tilPackageName);
      etPackageName = view.findViewById(R.id.etPackageName);
      chipLangKotlin = view.findViewById(R.id.chipLangKotlin);
      chipLangJava = view.findViewById(R.id.chipLangJava);
      tvLanguageHint = view.findViewById(R.id.tvLanguageHint);
      chipGroupDsl = view.findViewById(R.id.chipGroupDsl);
      chipGroupMinSdk = view.findViewById(R.id.chipGroupMinSdk);
      tvSaveLocation = view.findViewById(R.id.tvSaveLocation);
      btnCreateProject = view.findViewById(R.id.btnCreateProject);
      btnCancelWizard = view.findViewById(R.id.btnCancelWizard);

      layoutSyncProgress = view.findViewById(R.id.layoutSyncProgress);
      tvSyncDetail = view.findViewById(R.id.tvSyncDetail);
      progressSync = view.findViewById(R.id.progressSync);
      tvSyncPercent = view.findViewById(R.id.tvSyncPercent);
  }

  private void setupListeners() {
      chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
          if (checkedIds.isEmpty()) return;
          int id = checkedIds.get(0);
          if (id == R.id.chipCatPhone) {
              updateCategory(AndroidCategory.PHONE_TABLET);
          } else if (id == R.id.chipCatWear) {
              updateCategory(AndroidCategory.WEAR_OS);
          } else if (id == R.id.chipCatTv) {
              updateCategory(AndroidCategory.ANDROID_TV);
          } else if (id == R.id.chipCatAuto) {
              updateCategory(AndroidCategory.AUTOMOTIVE);
          }
      });

      etProjectName.addTextChangedListener(new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
              tilProjectName.setError(null);
              if (!userEditedPackage) {
                  String clean = s.toString().trim().toLowerCase(Locale.ROOT)
                          .replaceAll("[^a-zA-Z0-9]", "");
                  if (clean.isEmpty()) clean = "myapplication";
                  if (Character.isDigit(clean.charAt(0))) clean = "app" + clean;
                  etPackageName.setText("com.example." + clean);
              }
          }

          @Override
          public void afterTextChanged(Editable s) {}
      });

      etPackageName.addTextChangedListener(new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
              tilPackageName.setError(null);
              if (etPackageName.hasFocus()) {
                  userEditedPackage = true;
              }
          }

          @Override
          public void afterTextChanged(Editable s) {}
      });

      btnCancelWizard.setOnClickListener(v -> dismiss());
      btnCreateProject.setOnClickListener(v -> validateAndCreateProject());
  }

  private void updateCategory(AndroidCategory category) {
      this.selectedCategory = category;
      chipGroupTemplates.removeAllViews();

      List<AndroidTemplate> templates = AndroidTemplate.getByCategory(category);
      ChipCompat firstChip = null;
      for (AndroidTemplate template : templates) {
          ChipCompat chip = new ChipCompat(requireContext());
          chip.setId(View.generateViewId());
          chip.setText(template.getTitle());
          chip.setCheckable(true);
          chip.setTag(template);
          chipGroupTemplates.addView(chip);
          if (firstChip == null) firstChip = chip;
      }

      chipGroupTemplates.setOnCheckedStateChangeListener((group, checkedIds) -> {
          if (checkedIds.isEmpty()) return;
          View checkedView = group.findViewById(checkedIds.get(0));
          if (checkedView != null && checkedView.getTag() instanceof AndroidTemplate) {
              selectTemplate((AndroidTemplate) checkedView.getTag());
          }
      });

      if (firstChip != null) {
          firstChip.setChecked(true);
          selectTemplate((AndroidTemplate) firstChip.getTag());
      }
  }

  private void selectTemplate(AndroidTemplate template) {
      this.selectedTemplate = template;
      tvTemplateDescription.setText(template.getDescription());

      if (template.isSupportsCompose() || !template.isSupportsJava()) {
          chipLangKotlin.setChecked(true);
          chipLangJava.setEnabled(false);
          chipLangJava.setAlpha(0.4f);
          tvLanguageHint.setVisibility(View.VISIBLE);
          tvLanguageHint.setText(R.string.android_compose_kotlin_only);
      } else {
          chipLangJava.setEnabled(true);
          chipLangJava.setAlpha(1.0f);
          tvLanguageHint.setVisibility(View.GONE);
      }

      selectMinSdkChip(template.getDefaultMinSdk());
  }

  private void selectMinSdkChip(int sdk) {
      int targetId;
      switch (sdk) {
          case 21: targetId = R.id.chipSdk21; break;
          case 24: targetId = R.id.chipSdk24; break;
          case 26: targetId = R.id.chipSdk26; break;
          case 28: targetId = R.id.chipSdk28; break;
          case 30: targetId = R.id.chipSdk30; break;
          case 31: targetId = R.id.chipSdk31; break;
          case 34: targetId = R.id.chipSdk34; break;
          case 35: targetId = R.id.chipSdk35; break;
          default: targetId = sdk >= 30 ? R.id.chipSdk30 : R.id.chipSdk24; break;
      }
      chipGroupMinSdk.check(targetId);
  }

  private void validateAndCreateProject() {
      String name = etProjectName.getText() != null ? etProjectName.getText().toString().trim() : "";
      if (name.isEmpty()) {
          tilProjectName.setError(getString(R.string.android_error_name_empty));
          return;
      }

      String pkg = etPackageName.getText() != null ? etPackageName.getText().toString().trim() : "";
      if (!isValidPackageName(pkg)) {
          tilPackageName.setError(getString(R.string.android_error_pkg_invalid));
          return;
      }

      AndroidProjectConfig.Language language = chipLangKotlin.isChecked()
              ? AndroidProjectConfig.Language.KOTLIN
              : AndroidProjectConfig.Language.JAVA;

      if (selectedTemplate.isSupportsCompose() && language != AndroidProjectConfig.Language.KOTLIN) {
          GhostToast.makeText(requireContext(), R.string.android_compose_kotlin_only, GhostToast.LENGTH_SHORT).show();
          return;
      }

      AndroidProjectConfig config = new AndroidProjectConfig();
      config.setProjectName(name);
      config.setPackageName(pkg);
      config.setParentPath(savePath);
      config.setCategory(selectedCategory);
      config.setTemplate(selectedTemplate);
      config.setLanguage(language);
      config.setDslType(getSelectedDsl());
      config.setMinSdk(getSelectedMinSdk());
      config.setTargetSdk(34);
      config.setCompileSdk(34);

      startGeneration(config);
  }

  private void startGeneration(AndroidProjectConfig config) {
      scrollWizardContent.setVisibility(View.GONE);
      layoutSyncProgress.setVisibility(View.VISIBLE);

      final String[] steps = new String[] {
          getString(R.string.android_sync_step_1),
          getString(R.string.android_sync_step_2),
          getString(R.string.android_sync_step_3),
          getString(R.string.android_sync_step_4),
          getString(R.string.android_sync_step_5)
      };
      final int[] progressValues = new int[] { 20, 40, 60, 80, 100 };
      final String[] createdPath = new String[1];
      final String[] errorMessage = new String[1];

      new AndroidProjectGenerator(requireContext()).generateAsync(config,
              new AndroidProjectGenerator.OnGenerateListener() {
                  @Override
                  public void onSuccess(String projectPath) {
                      createdPath[0] = projectPath;
                  }

                  @Override
                  public void onError(String message) {
                      errorMessage[0] = message;
                  }
              });

      animateSyncStep(0, steps, progressValues, createdPath, errorMessage);
  }

  private void animateSyncStep(int index, String[] steps, int[] progressValues,
                               String[] createdPath, String[] errorMessage) {
      if (!isAdded()) return;

      if (index < steps.length) {
          tvSyncDetail.setText(steps[index]);
          progressSync.setProgress(progressValues[index]);
          tvSyncPercent.setText(progressValues[index] + "%");
          handler.postDelayed(() -> {
              if (!isAdded()) return;
              if (errorMessage[0] != null) {
                  showSyncError(errorMessage[0]);
                  return;
              }
              animateSyncStep(index + 1, steps, progressValues, createdPath, errorMessage);
          }, 450);
          return;
      }

      if (errorMessage[0] != null) {
          showSyncError(errorMessage[0]);
          return;
      }

      if (createdPath[0] == null) {
          handler.postDelayed(() -> {
              if (!isAdded()) return;
              if (errorMessage[0] != null) {
                  showSyncError(errorMessage[0]);
              } else if (createdPath[0] != null) {
                  onProjectCreated(createdPath[0]);
              }
          }, 350);
          return;
      }

      onProjectCreated(createdPath[0]);
  }

  private void showSyncError(String message) {
      layoutSyncProgress.setVisibility(View.GONE);
      scrollWizardContent.setVisibility(View.VISIBLE);
      GhostToast.makeText(requireContext(), message, GhostToast.LENGTH_LONG).show();
  }

  private void onProjectCreated(String projectPath) {
      if (!isAdded()) return;
      GhostToast.makeText(requireContext(), R.string.android_project_created_toast, GhostToast.LENGTH_SHORT).show();
      Bundle result = new Bundle();
      result.putString(ARG_RESULT_PATH, projectPath);
      getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
      dismiss();
  }

  private DslType getSelectedDsl() {
      List<Integer> checked = chipGroupDsl.getCheckedChipIds();
      if (!checked.isEmpty() && checked.get(0) == R.id.chipDslGroovy) {
          return DslType.GROOVY;
      }
      return DslType.KOTLIN;
  }

  private int getSelectedMinSdk() {
      List<Integer> checked = chipGroupMinSdk.getCheckedChipIds();
      if (checked.isEmpty()) return selectedTemplate.getDefaultMinSdk();
      int id = checked.get(0);
      if (id == R.id.chipSdk21) return 21;
      if (id == R.id.chipSdk24) return 24;
      if (id == R.id.chipSdk26) return 26;
      if (id == R.id.chipSdk28) return 28;
      if (id == R.id.chipSdk30) return 30;
      if (id == R.id.chipSdk31) return 31;
      if (id == R.id.chipSdk34) return 34;
      if (id == R.id.chipSdk35) return 35;
      return selectedTemplate.getDefaultMinSdk();
  }

  private static boolean isValidPackageName(String value) {
      if (value == null || value.isEmpty()) return false;
      String[] parts = value.split("\\.");
      if (parts.length < 2) return false;
      for (String part : parts) {
          if (part.isEmpty() || !Character.isJavaIdentifierStart(part.charAt(0))) return false;
          for (int i = 1; i < part.length(); i++) {
              if (!Character.isJavaIdentifierPart(part.charAt(i))) return false;
          }
      }
      return true;
  }

  @Override
  public void onDestroyView() {
      handler.removeCallbacksAndMessages(null);
      super.onDestroyView();
  }
}
