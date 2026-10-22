package ir.hanzodev1375.ghostide.project;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ir.hanzodev1375.components.glass.ChipCompat;
import ir.hanzodev1375.components.glass.ChipGroupCompat;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.project.android.AndroidCategory;
import ir.hanzodev1375.ghostide.project.android.AndroidProjectConfig;
import ir.hanzodev1375.ghostide.project.android.AndroidProjectGenerator;
import ir.hanzodev1375.ghostide.project.android.AndroidTemplate;
import ir.theme.M3Theme;

import java.io.File;
import java.util.List;

public class AndroidStudioProjectDialog {

    public interface OnProjectCreated {
        void onCreated(String projectPath);
    }

    private final Context context;
    private final String savePath;
    private final OnProjectCreated callback;

    private AlertDialog dialog;
    private AndroidCategory selectedCategory = AndroidCategory.PHONE_TABLET;
    private AndroidTemplate selectedTemplate = AndroidTemplate.PHONE_EMPTY_VIEWS;

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
    private TextView tvLanguageHint;
    private ChipGroupCompat chipGroupDsl;
    private ChipCompat chipDslKotlin;
    private ChipCompat chipDslGroovy;
    private ChipGroupCompat chipGroupMinSdk;
    private TextView tvSaveLocation;

    private boolean isUserEditedPackage = false;

    public AndroidStudioProjectDialog(Context context, String savePath, OnProjectCreated callback) {
        this.context = context;
        this.savePath = savePath;
        this.callback = callback;
    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_android_studio_project, null);

        bindViews(view);
        setupBackdrops(view);
        setupListeners();
        populateTemplatesForCategory(AndroidCategory.PHONE_TABLET);

        M3Theme.applyTopLevel(view);

        dialog = new DialogCompat(context)
                .setTitle(R.string.android_project_new_title)
                .setView(view)
                .setPositiveButton(R.string.project_create, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> handleCreateClick());
        });

        dialog.show();
    }

    private void bindViews(View view) {
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        chipGroupTemplates = view.findViewById(R.id.chipGroupTemplates);
        tvTemplateDescription = view.findViewById(R.id.tvTemplateDescription);
        tilProjectName = view.findViewById(R.id.tilProjectName);
        etProjectName = view.findViewById(R.id.etProjectName);
        tilPackageName = view.findViewById(R.id.tilPackageName);
        etPackageName = view.findViewById(R.id.etPackageName);
        chipGroupLanguage = view.findViewById(R.id.chipGroupLanguage);
        chipLangKotlin = view.findViewById(R.id.chipLangKotlin);
        chipLangJava = view.findViewById(R.id.chipLangJava);
        tvLanguageHint = view.findViewById(R.id.tvLanguageHint);
        chipGroupDsl = view.findViewById(R.id.chipGroupDsl);
        chipDslKotlin = view.findViewById(R.id.chipDslKotlin);
        chipDslGroovy = view.findViewById(R.id.chipDslGroovy);
        chipGroupMinSdk = view.findViewById(R.id.chipGroupMinSdk);
        tvSaveLocation = view.findViewById(R.id.tvSaveLocation);

        if (tvSaveLocation != null) {
            tvSaveLocation.setText(context.getString(R.string.android_save_location, savePath));
        }
    }

    private void setupBackdrops(View view) {
        View backdrop = findActivityContentView(context);
        if (backdrop != null) {
            chipGroupCategory.setBackdropSource(backdrop);
            chipGroupTemplates.setBackdropSource(backdrop);
            chipGroupLanguage.setBackdropSource(backdrop);
            chipGroupDsl.setBackdropSource(backdrop);
            chipGroupMinSdk.setBackdropSource(backdrop);
        }
    }

    private void setupListeners() {
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipCatPhone) {
                selectedCategory = AndroidCategory.PHONE_TABLET;
            } else if (id == R.id.chipCatWear) {
                selectedCategory = AndroidCategory.WEAR_OS;
            } else if (id == R.id.chipCatTv) {
                selectedCategory = AndroidCategory.ANDROID_TV;
            } else if (id == R.id.chipCatAuto) {
                selectedCategory = AndroidCategory.AUTOMOTIVE;
            }
            populateTemplatesForCategory(selectedCategory);
        });

        etPackageName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // If user clears or enters custom text, mark as user edited
                String currentName = etProjectName.getText() != null ? etProjectName.getText().toString().trim() : "";
                String autoPkg = "com.example." + sanitizePackageSegment(currentName);
                if (s != null && !s.toString().equals(autoPkg)) {
                    isUserEditedPackage = true;
                }
            }
        });

        etProjectName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isUserEditedPackage) {
                    String name = s != null ? s.toString().trim() : "";
                    String segment = sanitizePackageSegment(name);
                    etPackageName.setText("com.example." + segment);
                }
            }
        });
    }

    private void populateTemplatesForCategory(AndroidCategory category) {
        chipGroupTemplates.removeAllViews();
        List<AndroidTemplate> templates = AndroidTemplate.getByCategory(category);
        if (templates.isEmpty()) return;

        View backdrop = findActivityContentView(context);

        ChipCompat firstChip = null;
        for (AndroidTemplate t : templates) {
            ChipCompat chip = new ChipCompat(context);
            chip.setId(View.generateViewId());
            chip.setText(t.getTitle());
            chip.setCheckable(true);
            chip.setTag(t);
            if (backdrop != null) {
                chip.setBackdropSource(backdrop);
            }

            chipGroupTemplates.addView(chip);
            if (firstChip == null) {
                firstChip = chip;
            }
        }

        chipGroupTemplates.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            View checkedView = group.findViewById(checkedIds.get(0));
            if (checkedView != null && checkedView.getTag() instanceof AndroidTemplate) {
                onTemplateSelected((AndroidTemplate) checkedView.getTag());
            }
        });

        if (firstChip != null) {
            firstChip.setChecked(true);
            onTemplateSelected((AndroidTemplate) firstChip.getTag());
        }
    }

    private void onTemplateSelected(AndroidTemplate template) {
        selectedTemplate = template;
        if (tvTemplateDescription != null) {
            tvTemplateDescription.setText(template.getDescription());
        }

        // Language constraints
        if (template.isSupportsCompose()) {
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

        // Adjust Min SDK based on template defaults
        int defaultSdk = template.getDefaultMinSdk();
        selectMinSdkChip(defaultSdk);
    }

    private void selectMinSdkChip(int sdk) {
        int targetId = R.id.chipSdk24;
        switch (sdk) {
            case 21: targetId = R.id.chipSdk21; break;
            case 24: targetId = R.id.chipSdk24; break;
            case 26: targetId = R.id.chipSdk26; break;
            case 28: targetId = R.id.chipSdk28; break;
            case 30: targetId = R.id.chipSdk30; break;
            case 31: targetId = R.id.chipSdk31; break;
            case 34: targetId = R.id.chipSdk34; break;
            case 35: targetId = R.id.chipSdk35; break;
            default:
                if (sdk >= 30) targetId = R.id.chipSdk30;
                else targetId = R.id.chipSdk24;
                break;
        }
        chipGroupMinSdk.check(targetId);
    }

    private void handleCreateClick() {
        String name = etProjectName.getText() != null ? etProjectName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            tilProjectName.setError(context.getString(R.string.android_error_name_empty));
            return;
        }
        tilProjectName.setError(null);

        String pkg = etPackageName.getText() != null ? etPackageName.getText().toString().trim() : "";
        if (pkg.isEmpty() || !isValidPackage(pkg)) {
            tilPackageName.setError(context.getString(R.string.android_error_pkg_invalid));
            return;
        }
        tilPackageName.setError(null);

        File targetFolder = new File(savePath, name);
        if (targetFolder.exists()) {
            tilProjectName.setError("A folder named \"" + name + "\" already exists here.");
            return;
        }

        AndroidProjectConfig config = new AndroidProjectConfig();
        config.setProjectName(name);
        config.setPackageName(pkg);
        config.setParentPath(savePath);
        config.setCategory(selectedCategory);
        config.setTemplate(selectedTemplate);

        // Language
        boolean isKotlin = chipLangKotlin.isChecked() || selectedTemplate.isSupportsCompose();
        config.setLanguage(isKotlin ? AndroidProjectConfig.Language.KOTLIN : AndroidProjectConfig.Language.JAVA);

        // DSL
        boolean isKts = chipDslKotlin.isChecked();
        config.setDslType(isKts ? DslType.KOTLIN : DslType.GROOVY);

        // Min SDK
        config.setMinSdk(getSelectedMinSdk());
        config.setTargetSdk(34);
        config.setCompileSdk(34);

        dialog.dismiss();

        new AndroidProjectGenerator(context).generateAsync(config, new AndroidProjectGenerator.OnGenerateListener() {
            @Override
            public void onSuccess(String projectPath) {
                if (callback != null) {
                    callback.onCreated(projectPath);
                }
            }

            @Override
            public void onError(String message) {
                new DialogCompat(context)
                        .setTitle(R.string.project_error_title)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });
    }

    private int getSelectedMinSdk() {
        int checkedId = chipGroupMinSdk.getCheckedChipId();
        if (checkedId == R.id.chipSdk21) return 21;
        if (checkedId == R.id.chipSdk24) return 24;
        if (checkedId == R.id.chipSdk26) return 26;
        if (checkedId == R.id.chipSdk28) return 28;
        if (checkedId == R.id.chipSdk30) return 30;
        if (checkedId == R.id.chipSdk31) return 31;
        if (checkedId == R.id.chipSdk34) return 34;
        if (checkedId == R.id.chipSdk35) return 35;
        return selectedTemplate.getDefaultMinSdk();
    }

    private static String sanitizePackageSegment(String name) {
        if (name == null || name.isEmpty()) return "myapplication";
        String s = name.toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (s.isEmpty() || Character.isDigit(s.charAt(0))) {
            s = "app" + s;
        }
        return s;
    }

    private boolean isValidPackage(String pkg) {
        if (!pkg.contains(".")) return false;
        for (String segment : pkg.split("\\.", -1)) {
            if (segment.isEmpty() || !Character.isLetter(segment.charAt(0))) return false;
            if (!segment.matches("[a-zA-Z][a-zA-Z0-9_]*")) return false;
        }
        return true;
    }

    private static View findActivityContentView(Context context) {
        Context current = context;
        while (current instanceof android.content.ContextWrapper) {
            if (current instanceof android.app.Activity) {
                return ((android.app.Activity) current).findViewById(android.R.id.content);
            }
            current = ((android.content.ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
