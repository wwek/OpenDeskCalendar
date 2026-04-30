package org.opendeskcalendar.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.opendeskcalendar.app.R;
import org.opendeskcalendar.app.data.AppSettings;
import org.opendeskcalendar.app.data.PreferencesStore;
import org.opendeskcalendar.app.net.WeatherService;
import org.opendeskcalendar.app.ui.ChineseText;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CitySettingsActivity extends Activity {
    private PreferencesStore store;
    private AppSettings current;
    private EditText queryEdit;
    private EditText latEdit;
    private EditText lonEdit;
    private RadioGroup resultGroup;
    private final ArrayList<WeatherService.CityResult> results = new ArrayList<WeatherService.CityResult>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        store = new PreferencesStore(this);
        current = store.getSettings();
        setContentView(buildContent());
        queryEdit.setText(current.cityName);
        latEdit.setText(String.valueOf(current.latitude));
        lonEdit.setText(String.valueOf(current.longitude));
        search();
    }

    private ScrollView buildContent() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(14));
        scrollView.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button close = button(getString(R.string.close));
        close.setOnClickListener(v -> finish());
        TextView title = text(getString(R.string.activity_city_settings), 22, true);
        Button done = button(getString(R.string.done));
        done.setOnClickListener(v -> saveSelected());
        top.addView(close);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(done);
        root.addView(top);

        queryEdit = new EditText(this);
        queryEdit.setSingleLine(true);
        queryEdit.setHint(R.string.city_hint);
        root.addView(queryEdit);

        Button search = button(getString(R.string.city_search));
        search.setOnClickListener(v -> search());
        root.addView(search);

        root.addView(section(getString(R.string.city_results)));
        resultGroup = new RadioGroup(this);
        resultGroup.setOrientation(RadioGroup.VERTICAL);
        root.addView(resultGroup);

        root.addView(section(getString(R.string.city_manual)));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        latEdit = numberEdit(getString(R.string.city_latitude));
        lonEdit = numberEdit(getString(R.string.city_longitude));
        row.addView(latEdit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lonEdit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(row);
        return scrollView;
    }

    private void search() {
        String query = queryEdit.getText().toString().trim();
        if (query.length() == 0) {
            Toast.makeText(this, R.string.city_name_required, Toast.LENGTH_SHORT).show();
            return;
        }
        new SearchTask(this).execute(query);
    }

    private void bindResults(List<WeatherService.CityResult> cities) {
        results.clear();
        results.addAll(cities);
        resultGroup.removeAllViews();
        if (results.isEmpty()) {
            TextView empty = text(getString(R.string.city_empty), 16, false);
            resultGroup.addView(empty);
            return;
        }
        for (int i = 0; i < results.size(); i++) {
            WeatherService.CityResult result = results.get(i);
            RadioButton radio = new RadioButton(this);
            radio.setId(1000 + i);
            radio.setText(String.format(
                    ChineseText.locale(this),
                    "%s\n%s",
                    ChineseText.display(this, result.title()),
                    ChineseText.display(this, result.subtitle())));
            radio.setTextSize(16);
            radio.setPadding(0, dp(6), 0, dp(6));
            resultGroup.addView(radio);
            if (i == 0) {
                radio.setChecked(true);
            }
        }
    }

    private void saveSelected() {
        String city = queryEdit.getText().toString().trim();
        String district = "";
        double lat;
        double lon;
        int checked = resultGroup.getCheckedRadioButtonId();
        if (checked >= 1000 && checked - 1000 < results.size()) {
            WeatherService.CityResult result = results.get(checked - 1000);
            city = result.admin == null || result.admin.length() == 0 ? result.name : result.admin;
            district = result.name;
            lat = result.latitude;
            lon = result.longitude;
        } else {
            try {
                lat = Double.parseDouble(latEdit.getText().toString().trim());
                lon = Double.parseDouble(lonEdit.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.city_bad_coordinates, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        AppSettings settings = new AppSettings(
                current.theme,
                current.showSeconds,
                current.use24Hour,
                current.weekStartsMonday,
                current.keepScreenOn,
                current.bootAutostart,
                current.showLunar,
                current.showAlmanac,
                current.showWifi,
                current.orientationMode,
                current.fontScale,
                current.weatherRefreshMinutes,
                city,
                district,
                lat,
                lon,
                current.weatherProvider,
                current.weatherHost,
                current.weatherKey,
                current.backupLauncherPackage,
                current.confirmExit,
                current.hourlyAnnouncementEnabled,
                current.halfHourlyAnnouncementEnabled,
                current.hourlyAnnouncementQuietNight,
                current.nightDimEnabled,
                current.burnInProtectionEnabled,
                current.indoorEnabled,
                current.indoorEndpoint,
                current.indoorToken);
        store.saveSettings(settings);
        Toast.makeText(this, R.string.city_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void failSearch(String message) {
        store.recordError("Location", message);
        Toast.makeText(this, R.string.city_search_failed, Toast.LENGTH_SHORT).show();
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
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
        if (bold) {
            view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private EditText numberEdit(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        return editText;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static final class SearchTask extends AsyncTask<String, Void, List<WeatherService.CityResult>> {
        private final WeakReference<CitySettingsActivity> activityRef;
        private Exception error;

        SearchTask(CitySettingsActivity activity) {
            this.activityRef = new WeakReference<CitySettingsActivity>(activity);
        }

        @Override
        protected List<WeatherService.CityResult> doInBackground(String... params) {
            try {
                return new WeatherService().searchCities(params[0]);
            } catch (Exception e) {
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<WeatherService.CityResult> cityResults) {
            CitySettingsActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            if (activity.isFinishing()) {
                return;
            }
            if (cityResults != null) {
                activity.bindResults(cityResults);
            } else {
                activity.failSearch(error == null ? activity.getString(R.string.unknown_error) : error.getMessage());
            }
        }
    }
}
