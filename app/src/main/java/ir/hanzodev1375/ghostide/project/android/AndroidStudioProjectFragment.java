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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ir.hanzodev1375.components.glass.ChipCompat;
import ir.hanzodev1375.components.glass.ChipGroupCompat;
import ir.hanzodev1375.components.views.GhostToast;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.activity.EditorActivity;
import ir.hanzodev1375.ghostide.project.DslType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AndroidStudioProjectFragment extends Fragment {

    public static final String VIRTUAL_PATH = "ghostide://new-android-project";
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
    private ChipGroupCompat chipGroupLanguage;
    private ChipCompat chipLangKotlin;
    private ChipCompat chipLangJava;
    private ChipGroupCompat chipGroupDsl;
    private ChipGroupCompat chipGroupMinSdk;
    private TextView tvSaveLocation;
    private MaterialButton btnCreateProject;
    private MaterialButton btnCancelWizard;

    private ScrollView scrollWizardContent;
    private LinearLayout layoutSyncProgress;
    private TextView tvSyncTitle;
    private TextView tvSyncDetail;
    private LinearProgressIndicator progressSync;
    private TextView tvSyncPercent;

    private AndroidCategory selectedCategory = AndroidCategory.PHONE_TABLET;
    private AndroidTemplate selectedTemplate = AndroidTemplate.PHONE_EMPTY_VIEWS;
    private boolean userEditedPackage = false;

    public static AndroidStudioProjectFragment newInstance() {
        return newInstance(null);
    }

    public static AndroidStudioProjectFragment newInstance(String savePath) {
        AndroidStudioProjectFragment fragment = new AndroidStudioProjectFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SAVE_PATH, savePath);
        fragment.setArguments(args);
        return fragment;
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

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_android_studio_project, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupListeners();
        updateCategory(AndroidCategory.PHONE_TABLET);
        updateSaveLocationText();
    }

    private void bindViews(View v) {
        scrollWizardContent = v.findViewById(R.id.scrollWizardContent);
        chipGroupCategory = v.findViewById(R.id.chipGroupCategory);
        chipGroupTemplates = v.findViewById(R.id.chipGroupTemplates);
        tvTemplateDescription = v.findViewById(R.id.tvTemplateDescription);
        tilProjectName = v.findViewById(R.id.tilProjectName);
        etProjectName = v.findViewById(R.id.etProjectName);
        tilPackageName = v.findViewById(R.id.tilPackageName);
        etPackageName = v.findViewById(R.id.etPackageName);
        chipGroupLanguage = v.findViewById(R.id.chipGroupLanguage);
        chipLangKotlin = v.findViewById(R.id.chipLangKotlin);
        chipLangJava = v.findViewById(R.id.chipLangJava);
        chipGroupDsl = v.findViewById(R.id.chipGroupDsl);
        chipGroupMinSdk = v.findViewById(R.id.chipGroupMinSdk);
        tvSaveLocation = v.findViewById(R.id.tvSaveLocation);
        btnCreateProject = v.findViewById(R.id.btnCreateProject);
        btnCancelWizard = v.findViewById(R.id.btnCancelWizard);

        layoutSyncProgress = v.findViewById(R.id.layoutSyncProgress);
        tvSyncTitle = v.findViewById(R.id.tvSyncTitle);
        tvSyncDetail = v.findViewById(R.id.tvSyncDetail);
        progressSync = v.findViewById(R.id.progressSync);
        tvSyncPercent = v.findViewById(R.id.tvSyncPercent);
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

        btnCancelWizard.setOnClickListener(v -> closeWizardTab());
        btnCreateProject.setOnClickListener(v -> validateAndCreateProject());
    }

    private void updateCategory(AndroidCategory category) {
        this.selectedCategory = category;
        chipGroupTemplates.removeAllViews();

        List<AndroidTemplate> templates = AndroidTemplate.getByCategory(category);
        for (int i = 0; i < templates.size(); i++) {
            AndroidTemplate t = templates.get(i);
            ChipCompat chip = new ChipCompat(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(t.getTitle());
            chip.setCheckable(true);
            chip.setTag(t);

            if (i == 0) {
                chip.setChecked(true);
                selectTemplate(t);
            }

            chipGroupTemplates.addView(chip);
        }

        chipGroupTemplates.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            View view = group.findViewById(checkedIds.get(0));
            if (view != null && view.getTag() instanceof AndroidTemplate) {
                selectTemplate((AndroidTemplate) view.getTag());
            }
        });
    }

    private void selectTemplate(AndroidTemplate template) {
        this.selectedTemplate = template;
        tvTemplateDescription.setText(template.getDescription());

        if (template.isSupportsCompose() || !template.isSupportsJava()) {
            chipLangKotlin.setChecked(true);
            chipLangJava.setEnabled(false);
        } else {
            chipLangJava.setEnabled(true);
        }
    }

    private void updateSaveLocationText() {
        tvSaveLocation.setText(getString(R.string.android_save_location, savePath));
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

        AndroidProjectConfig.Language lang = chipLangKotlin.isChecked()
                ? AndroidProjectConfig.Language.KOTLIN
                : AndroidProjectConfig.Language.JAVA;

        if (selectedTemplate.isSupportsCompose() && lang != AndroidProjectConfig.Language.KOTLIN) {
            GhostToast.makeText(requireContext(), R.string.android_compose_kotlin_only, GhostToast.LENGTH_SHORT).show();
            return;
        }

        DslType dsl = getSelectedDsl();
        int minSdk = getSelectedMinSdk();

        AndroidProjectConfig config = new AndroidProjectConfig();
        config.setProjectName(name);
        config.setPackageName(pkg);
        config.setParentPath(savePath);
        config.setCategory(selectedCategory);
        config.setTemplate(selectedTemplate);
        config.setLanguage(lang);
        config.setDslType(dsl);
        config.setMinSdk(minSdk);

        startGradleSyncAndCreate(config);
    }

    private void startGradleSyncAndCreate(AndroidProjectConfig config) {
        scrollWizardContent.setVisibility(View.GONE);
        layoutSyncProgress.setVisibility(View.VISIBLE);

        final String[] steps = new String[] {
            getString(R.string.android_sync_step_1),
            getString(R.string.android_sync_step_2),
            getString(R.string.android_sync_step_3),
            getString(R.string.android_sync_step_4),
            getString(R.string.android_sync_step_5)
        };

        final int[] progressVals = new int[] { 20, 40, 60, 80, 100 };

        // Real project generation on background thread
        final String[] createdPathHolder = new String[1];
        final String[] errorHolder = new String[1];

        AndroidProjectGenerator generator = new AndroidProjectGenerator(requireContext());
        generator.generateAsync(config, new AndroidProjectGenerator.OnGenerateListener() {
            @Override
            public void onSuccess(String projectPath) {
                createdPathHolder[0] = projectPath;
            }

            @Override
            public void onError(String message) {
                errorHolder[0] = message;
            }
        });

        // Stepped simulation for realistic Android Studio Gradle sync pacing (total ~3s)
        animateSyncStep(0, steps, progressVals, config, createdPathHolder, errorHolder);
    }

    private void animateSyncStep(
            int stepIndex,
            String[] steps,
            int[] progressVals,
            AndroidProjectConfig config,
            String[] createdPathHolder,
            String[] errorHolder) {
        if (!isAdded()) return;

        if (stepIndex < steps.length) {
            tvSyncDetail.setText(steps[stepIndex]);
            progressSync.setProgress(progressVals[stepIndex]);
            tvSyncPercent.setText(progressVals[stepIndex] + "%");

            handler.postDelayed(() -> {
                if (!isAdded()) return;
                if (errorHolder[0] != null) {
                    showSyncError(errorHolder[0]);
                    return;
                }
                animateSyncStep(stepIndex + 1, steps, progressVals, config, createdPathHolder, errorHolder);
            }, 600);
        } else {
            // All steps finished
            if (errorHolder[0] != null) {
                showSyncError(errorHolder[0]);
                return;
            }

            String projectPath = createdPathHolder[0];
            if (projectPath == null) {
                // Background thread still finishing last touches, check again shortly
                handler.postDelayed(() -> {
                    if (!isAdded()) return;
                    if (errorHolder[0] != null) {
                        showSyncError(errorHolder[0]);
                    } else if (createdPathHolder[0] != null) {
                        finishAndOpenProject(createdPathHolder[0], config);
                    }
                }, 400);
                return;
            }

            finishAndOpenProject(projectPath, config);
        }
    }

    private void showSyncError(String error) {
        layoutSyncProgress.setVisibility(View.GONE);
        scrollWizardContent.setVisibility(View.VISIBLE);
        GhostToast.makeText(requireContext(), error, GhostToast.LENGTH_LONG).show();
    }

    private void finishAndOpenProject(String projectPath, AndroidProjectConfig config) {
        if (!isAdded()) return;

        GhostToast.makeText(
                requireContext(),
                getString(R.string.android_project_created_toast),
                GhostToast.LENGTH_SHORT)
            .show();

        if (getActivity() instanceof EditorActivity) {
            EditorActivity editor = (EditorActivity) getActivity();

            // Find primary source file
            File mainFile = findMainSourceFile(projectPath, config);

            // Open main generated file in new editor tab first
            if (mainFile != null && mainFile.exists()) {
                editor.openFile(mainFile.getAbsolutePath(), mainFile.getName());
            }

            // Close this wizard tab
            editor.closeTabByPath(VIRTUAL_PATH);
        }
    }

    private File findMainSourceFile(String projectPath, AndroidProjectConfig config) {
        String pkgPath = config.getPackageName().replace('.', '/');
        File srcDir = new File(projectPath, "app/src/main/java/" + pkgPath);
        if (srcDir.exists() && srcDir.isDirectory()) {
            File[] files = srcDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().startsWith("MainActivity")) {
                        return f;
                    }
                }
                for (File f : files) {
                    if (f.isFile() && (f.getName().endsWith(".kt") || f.getName().endsWith(".java"))) {
                        return f;
                    }
                }
            }
        }
        File buildFile = new File(projectPath, "app/build.gradle.kts");
        if (buildFile.exists()) return buildFile;
        File buildGroovy = new File(projectPath, "app/build.gradle");
        if (buildGroovy.exists()) return buildGroovy;
        return new File(projectPath);
    }

    private void closeWizardTab() {
        if (getActivity() instanceof EditorActivity) {
            ((EditorActivity) getActivity()).closeTabByPath(VIRTUAL_PATH);
        }
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
        if (checked.isEmpty()) return 24;
        int id = checked.get(0);
        if (id == R.id.chipSdk21) return 21;
        if (id == R.id.chipSdk24) return 24;
        if (id == R.id.chipSdk26) return 26;
        if (id == R.id.chipSdk28) return 28;
        if (id == R.id.chipSdk30) return 30;
        if (id == R.id.chipSdk31) return 31;
        if (id == R.id.chipSdk34) return 34;
        if (id == R.id.chipSdk35) return 35;
        return 24;
    }

    private static boolean isValidPackageName(String s) {
        if (s == null || s.isEmpty()) return false;
        String[] parts = s.split("\\.");
        if (parts.length < 2) return false;
        for (String p : parts) {
            if (p.isEmpty() || !Character.isJavaIdentifierStart(p.charAt(0))) return false;
            for (int i = 1; i < p.length(); i++) {
                if (!Character.isJavaIdentifierPart(p.charAt(i))) return false;
            }
        }
        return true;
    }
}
