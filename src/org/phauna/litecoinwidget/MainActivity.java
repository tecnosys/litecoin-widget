package org.phauna.litecoinwidget;

import android.app.Activity;
import android.os.Bundle;

import android.os.AsyncTask;
import android.util.Log;
import android.app.Activity;
import android.widget.Toast;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

import java.util.Date;

public class MainActivity extends Activity {

  int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

  /** Called when the activity is first created. */
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setResult(RESULT_CANCELED);

    // Find the widget id from the intent.
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    if (extras != null) {
      mAppWidgetId = extras.getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
      );
    }
    // If they gave us an intent without the widget id, just bail.
    if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish();
    }

    Log.d(C.LOG, "opening mainactivity for widget id " + mAppWidgetId);

    // display the preferences fragment as the main content:
    getFragmentManager().beginTransaction()
      .replace(android.R.id.content, new SettingsFragment())
      .commit();
  }

  public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    SharedPreferences mPrefs;

    @Override public void onCreate(Bundle icy) {
      super.onCreate(icy);

      MainActivity activity = (MainActivity) getActivity();
      if (activity == null) {
        Log.d(C.LOG, "activity is null in onCreate");
      }
      String prefsName = "widget" + activity.mAppWidgetId;
      PreferenceManager manager = getPreferenceManager();
      manager.setSharedPreferencesName(prefsName);
      mPrefs = manager.getSharedPreferences();

      addPreferencesFromResource(R.xml.preferences);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      if (key.equals(C.pref_key_exchange)) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
          Log.d(C.LOG,"activity is null in onchange");
        }

        // manually call the service once (might be able to do it with a broadcast alternatively..)
        Intent intent = new Intent(activity.getApplicationContext(),
            UpdateWidgetService.class);
        int[] widgetIds = { activity.mAppWidgetId };
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        // Update the widgets via the service
        activity.startService(intent);

        // why do I need to put the extraAppWidgetID?
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, activity.mAppWidgetId);
        activity.setResult(Activity.RESULT_OK, result);
        activity.finish();
      }
    }

    @Override public void onResume() {
      super.onResume();
      mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override public void onPause() {
      super.onPause();
      mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }
  }

}
