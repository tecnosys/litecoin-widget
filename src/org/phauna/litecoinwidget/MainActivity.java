package org.phauna.litecoinwidget;

import android.app.Activity;
import android.preference.PreferenceActivity;
import android.os.Bundle;

import android.os.AsyncTask;
import android.util.Log;
import android.app.Activity;
//import android.support.v4.app.FragmentActivity;
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

public class MainActivity extends PreferenceActivity implements
  OnSharedPreferenceChangeListener {

  int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
  SharedPreferences mPrefs;

  /** Called when the activity is first created. */
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences);

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

    String prefsName = "widget" + mAppWidgetId;
    PreferenceManager manager = getPreferenceManager();
    manager.setSharedPreferencesName(prefsName);
    mPrefs = manager.getSharedPreferences();

    Log.d(C.LOG, "opening mainactivity for widget id " + mAppWidgetId);
  }

  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (key.equals(C.pref_key_exchange)) {
      // manually call the service once (might be able to do it with a broadcast alternatively..)
      Intent intent = new Intent(getApplicationContext(),
          UpdateWidgetService.class);
      int[] widgetIds = { mAppWidgetId };
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
      // Update the widgets via the service
      startService(intent);

      // why do I need to put the extraAppWidgetID?
      Intent result = new Intent();
      result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      setResult(Activity.RESULT_OK, result);
      finish();
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
