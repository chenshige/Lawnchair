package ch.deletescape.lawnchair.weather;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import ch.deletescape.lawnchair.Launcher;
import ch.deletescape.lawnchair.LauncherAppState;
import ch.deletescape.lawnchair.R;
import ch.deletescape.lawnchair.Utilities;

public class WeatherHelper implements SharedPreferences.OnSharedPreferenceChangeListener, Runnable, WeatherAPI.WeatherCallback {
    private static final String KEY_UNITS = "pref_weather_units";
    private static final String KEY_CITY = "pref_weather_city";
    private static final int DELAY = 30 * 3600 * 1000;
    private final WeatherAPI mApi;
    private WeatherAPI.WeatherData mWeatherData;
    private TextView mTemperatureView;
    private Handler mHandler;
    private ImageView mIconView;
    private WeatherIconProvider iconProvider;
    private boolean stopped = false;

    public WeatherHelper(TextView temperatureView, ImageView iconView, Context context) {
        mTemperatureView = temperatureView;
        mIconView = iconView;
        iconProvider = new WeatherIconProvider(context);
        setupOnClickListener(context);
        mHandler = new Handler();
        SharedPreferences prefs = Utilities.getPrefs(context);
        mApi = WeatherAPI.Companion.create(context,
                Integer.parseInt(prefs.getString("pref_weatherProvider", "0")));
        mApi.setWeatherCallback(this);
        setCity(prefs.getString(KEY_CITY, "Lucerne, CH"));
        setUnits(prefs.getString(KEY_UNITS, "metric"));
        refresh();
    }

    private void refresh() {
        if (!stopped) {
            mApi.getCurrentWeather();
            mHandler.postDelayed(this, DELAY);
        }
    }

    @Override
    public void onWeatherData(@NotNull WeatherAPI.WeatherData data) {
        mWeatherData = data;
        updateTextView();
        updateIconView();
    }

    private void updateTextView() {
        mTemperatureView.setText(mWeatherData.getTemperatureString());
    }

    private void updateIconView() {
        mIconView.setImageDrawable(iconProvider.getIcon(mWeatherData.getIcon()));
    }

    private void setCity(String city) {
        mApi.setCity(city);
    }

    private void setUnits(String units) {
        mApi.setUnits(units.equals("imperial") ? WeatherAPI.Units.IMPERIAL : WeatherAPI.Units.METRIC);
    }

    private void setupOnClickListener(final Context context) {
        mTemperatureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Launcher launcher = LauncherAppState.getInstance().getLauncher();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("dynact://velour/weather/ProxyActivity"));
                    intent.setComponent(new ComponentName("com.google.android.googlequicksearchbox",
                            "com.google.android.apps.gsa.velour.DynamicActivityTrampoline"));
                    intent.setSourceBounds(launcher.getViewBounds(mTemperatureView));
                    context.startActivity(intent, launcher.getActivityLaunchOptions(mTemperatureView));
                } catch (ActivityNotFoundException | IllegalArgumentException e) {
                    Toast.makeText(context, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
        switch (key) {
            case KEY_UNITS:
                setUnits(sharedPrefs.getString(KEY_UNITS, "metric"));
                updateTextView();
                break;
            case KEY_CITY:
                setCity(sharedPrefs.getString(KEY_CITY, mApi.getCity()));
                break;
        }
    }

    @Override
    public void run() {
        refresh();
    }

    public void stop() {
        stopped = true;
        mHandler.removeCallbacks(this);
    }
}
