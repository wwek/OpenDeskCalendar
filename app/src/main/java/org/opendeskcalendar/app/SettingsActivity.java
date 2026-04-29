package org.opendeskcalendar.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.opendeskcalendar.app.R;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.PreferencesStore;
import org.opendeskcalendar.app.ui.ChineseText;
import org.opendeskcalendar.app.ui.ThemePalette;

import java.util.ArrayList;
import java.util.List;

public final class SettingsActivity extends Activity {
    private PreferencesStore store;
    private AppSettings current;
    private ThemePalette palette;
    private Spinner themeSpinner;
    private Spinner fontSpinner;
    private Spinner refreshSpinner;
    private CheckBox secondsCheck;
    private CheckBox hour24Check;
    private CheckBox mondayCheck;
    private CheckBox keepOnCheck;
    private CheckBox bootCheck;
    private CheckBox lunarCheck;
    private CheckBox almanacCheck;
    private CheckBox wifiCheck;
    private CheckBox confirmExitCheck;
    private CheckBox nightDimCheck;
    private CheckBox indoorCheck;
    private EditText hostEdit;
    private EditText keyEdit;
    private EditText indoorEndpointEdit;
    private EditText indoorTokenEdit;
    private TextView cityValue;
    private TextView backupValue;
    private String backupPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        store = new PreferencesStore(this);
        current = store.getSettings();
        palette = ThemePalette.from(current);
        backupPackage = current.backupLauncherPackage;
        setContentView(buildContent());
        bindCurrent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        current = store.getSettings();
        if (cityValue != null) {
            cityValue.setText(ChineseText.display(this, current.displayCity()));
        }
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(palette.background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(14));
        root.setBackgroundColor(palette.background);
        scrollView.addView(root);

        TextView title = text(getString(R.string.settings_title), 26, true);
        root.addView(title);
        root.addView(section(getString(R.string.settings_section_appearance)));
        themeSpinner = spinner(getResources().getStringArray(R.array.theme_names));
        root.addView(row(getString(R.string.settings_theme), themeSpinner));
        secondsCheck = check(getString(R.string.settings_show_seconds));
        hour24Check = check(getString(R.string.settings_24_hour));
        mondayCheck = check(getString(R.string.settings_week_monday));
        root.addView(secondsCheck);
        root.addView(hour24Check);
        root.addView(mondayCheck);
        fontSpinner = spinner(getResources().getStringArray(R.array.font_size_names));
        root.addView(row(getString(R.string.settings_font_size), fontSpinner));

        root.addView(section(getString(R.string.settings_section_weather)));
        cityValue = text("", 16, false);
        Button cityButton = button(getString(R.string.modify));
        cityButton.setOnClickListener(v -> startActivity(new Intent(this, CitySettingsActivity.class)));
        root.addView(row(getString(R.string.settings_city), cityValue, cityButton));
        hostEdit = edit(getString(R.string.settings_host_hint));
        root.addView(row("Host", hostEdit));
        keyEdit = edit(getString(R.string.settings_key_hint));
        keyEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(row("Key", keyEdit));
        refreshSpinner = spinner(getResources().getStringArray(R.array.refresh_names));
        root.addView(row(getString(R.string.settings_refresh_rate), refreshSpinner));

        root.addView(section(getString(R.string.settings_section_indoor)));
        indoorCheck = check(getString(R.string.settings_indoor_enabled));
        root.addView(indoorCheck);
        indoorEndpointEdit = edit(getString(R.string.settings_indoor_endpoint_hint));
        root.addView(row(getString(R.string.settings_indoor_endpoint), indoorEndpointEdit));
        indoorTokenEdit = edit(getString(R.string.settings_indoor_token_hint));
        indoorTokenEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(row(getString(R.string.settings_indoor_token), indoorTokenEdit));

        root.addView(section(getString(R.string.settings_section_calendar)));
        lunarCheck = check(getString(R.string.settings_show_lunar));
        almanacCheck = check(getString(R.string.settings_show_almanac));
        root.addView(lunarCheck);
        root.addView(almanacCheck);
        TextView holiday = text(getString(R.string.settings_holiday_version), 15, false);
        root.addView(row(getString(R.string.settings_holiday_data), holiday));

        root.addView(section(getString(R.string.settings_section_startup)));
        bootCheck = check(getString(R.string.settings_boot_autostart));
        root.addView(bootCheck);
        Button homeGuide = button(getString(R.string.settings_home_guide));
        homeGuide.setOnClickListener(v -> showHomeGuide());
        root.addView(row(getString(R.string.settings_home_mode), homeGuide));
        backupValue = text("", 15, false);
        Button backupButton = button(getString(R.string.choose));
        backupButton.setOnClickListener(v -> chooseBackupLauncher());
        root.addView(row(getString(R.string.settings_backup_launcher), backupValue, backupButton));

        root.addView(section(getString(R.string.settings_section_device)));
        keepOnCheck = check(getString(R.string.settings_keep_screen_on));
        wifiCheck = check(getString(R.string.settings_show_wifi));
        nightDimCheck = check(getString(R.string.settings_night_dim));
        confirmExitCheck = check(getString(R.string.settings_confirm_exit));
        root.addView(keepOnCheck);
        root.addView(wifiCheck);
        root.addView(nightDimCheck);
        root.addView(confirmExitCheck);

        root.addView(section(getString(R.string.settings_section_diagnostics)));
        Button errors = button(getString(R.string.settings_recent_errors));
        errors.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.settings_recent_errors)
                .setMessage(store.errorSummary())
                .setPositiveButton(R.string.close, null)
                .show());
        root.addView(errors);
        Button export = button(getString(R.string.settings_export_logs));
        export.setOnClickListener(v -> exportLogs());
        root.addView(export);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, dp(20), 0, dp(8));
        Button cancel = button(getString(R.string.cancel));
        cancel.setOnClickListener(v -> finish());
        Button save = button(getString(R.string.save));
        save.setOnClickListener(v -> save());
        actions.addView(cancel);
        actions.addView(save);
        root.addView(actions);
        return scrollView;
    }

    private void bindCurrent() {
        cityValue.setText(ChineseText.display(this, current.displayCity()));
        themeSpinner.setSelection(themeIndex(current.theme));
        secondsCheck.setChecked(current.showSeconds);
        hour24Check.setChecked(current.use24Hour);
        mondayCheck.setChecked(current.weekStartsMonday);
        keepOnCheck.setChecked(current.keepScreenOn);
        bootCheck.setChecked(current.bootAutostart);
        lunarCheck.setChecked(current.showLunar);
        almanacCheck.setChecked(current.showAlmanac);
        wifiCheck.setChecked(current.showWifi);
        fontSpinner.setSelection(current.fontScale);
        refreshSpinner.setSelection(refreshIndex(current.weatherRefreshMinutes));
        hostEdit.setText(current.weatherHost);
        keyEdit.setText(current.weatherKey);
        confirmExitCheck.setChecked(current.confirmExit);
        nightDimCheck.setChecked(current.nightDimEnabled);
        indoorCheck.setChecked(current.indoorEnabled);
        indoorEndpointEdit.setText(current.indoorEndpoint);
        indoorTokenEdit.setText(current.indoorToken);
        backupValue.setText(backupPackage.length() == 0 ? getString(R.string.not_set) : backupPackage);
    }

    private void save() {
        current = store.getSettings();
        AppSettings settings = new AppSettings(
                themeValue(themeSpinner.getSelectedItemPosition()),
                secondsCheck.isChecked(),
                hour24Check.isChecked(),
                mondayCheck.isChecked(),
                keepOnCheck.isChecked(),
                bootCheck.isChecked(),
                lunarCheck.isChecked(),
                almanacCheck.isChecked(),
                wifiCheck.isChecked(),
                fontSpinner.getSelectedItemPosition(),
                refreshValue(refreshSpinner.getSelectedItemPosition()),
                current.cityName,
                current.districtName,
                current.latitude,
                current.longitude,
                hostEdit.getText().toString().trim(),
                keyEdit.getText().toString().trim(),
                backupPackage,
                confirmExitCheck.isChecked(),
                nightDimCheck.isChecked(),
                indoorCheck.isChecked(),
                indoorEndpointEdit.getText().toString().trim(),
                indoorTokenEdit.getText().toString().trim());
        store.saveSettings(settings);
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showHomeGuide() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.home_guide_title)
                .setMessage(R.string.home_guide_message)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.open_home_chooser, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(Intent.createChooser(intent, getString(R.string.choose_default_home)));
                })
                .show();
    }

    private void chooseBackupLauncher() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager manager = getPackageManager();
        List<ResolveInfo> apps = manager.queryIntentActivities(intent, 0);
        final ArrayList<String> labels = new ArrayList<String>();
        final ArrayList<String> packages = new ArrayList<String>();
        labels.add(getString(R.string.not_set));
        packages.add("");
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) {
                continue;
            }
            labels.add(info.loadLabel(manager).toString());
            packages.add(pkg);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_backup_launcher)
                .setItems(labels.toArray(new String[labels.size()]), (dialog, which) -> {
                    backupPackage = packages.get(which);
                    backupValue.setText(backupPackage.length() == 0 ? getString(R.string.not_set) : labels.get(which));
                })
                .show();
    }

    private void exportLogs() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_recent_errors));
        intent.putExtra(Intent.EXTRA_TEXT, store.errorSummary());
        startActivity(Intent.createChooser(intent, getString(R.string.settings_export_logs)));
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                styleSpinnerView(view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                styleSpinnerView(view);
                return view;
            }
        });
        spinner.setBackgroundColor(palette.panel);
        return spinner;
    }

    private void styleSpinnerView(View view) {
        view.setBackgroundColor(palette.panel);
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setTextColor(palette.primary);
            textView.setTextSize(16);
        }
    }

    private CheckBox check(String label) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(label);
        checkBox.setTextSize(16);
        checkBox.setTextColor(palette.primary);
        checkBox.setPadding(0, dp(4), 0, dp(4));
        return checkBox;
    }

    private EditText edit(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(palette.primary);
        editText.setHintTextColor(palette.muted);
        editText.setSingleLine(true);
        return editText;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(palette.primary);
        return button;
    }

    private TextView section(String label) {
        TextView view = text(label, 18, true);
        view.setPadding(0, dp(18), 0, dp(6));
        return view;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(bold ? palette.primary : palette.secondary);
        if (bold) {
            view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private View row(String label, View value) {
        return row(label, value, null);
    }

    private View row(String label, View value, View action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        TextView labelView = text(label, 16, false);
        row.addView(labelView, new LinearLayout.LayoutParams(dp(112), LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(value, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (action != null) {
            row.addView(action);
        }
        return row;
    }

    private String themeValue(int index) {
        if (index == 1) return AppSettings.THEME_DARK;
        if (index == 2) return AppSettings.THEME_MONO;
        if (index == 3) return AppSettings.THEME_EINK;
        return AppSettings.THEME_LIGHT;
    }

    private int themeIndex(String theme) {
        if (AppSettings.THEME_DARK.equals(theme)) return 1;
        if (AppSettings.THEME_MONO.equals(theme)) return 2;
        if (AppSettings.THEME_EINK.equals(theme)) return 3;
        return 0;
    }

    private int refreshValue(int index) {
        if (index == 0) return 30;
        if (index == 2) return 120;
        return 60;
    }

    private int refreshIndex(int minutes) {
        if (minutes <= 30) return 0;
        if (minutes >= 120) return 2;
        return 1;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
