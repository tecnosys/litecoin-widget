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
import android.preference.CheckBoxPreference;
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

  }

  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (key.equals(C.pref_key_exchange)) {
      String exchange = prefs.getString(C.pref_key_exchange, C.CFG_VREX_LTC);
      Preference ePref = findPreference(C.pref_key_exchange);
      ePref.setSummary(exchange);
    }
    if (key.equals(C.pref_key_owc)) {
      String owc = prefs.getString(C.pref_key_owc, C.USD);
      Preference ePref = findPreference(C.pref_key_owc);
      ePref.setSummary(owc);
    }
    if (key.equals(C.pref_key_color)) {
      String color = prefs.getString(C.pref_key_color, "light_grey");
      Preference ePref = findPreference(C.pref_key_color);
      ePref.setSummary(color);
    }
    if (key.equals(C.pref_key_done)) {
      // Update the widget via the service
      Intent intent = new Intent(getApplicationContext(), UpdateWidgetService.class);
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      // put the preferences directly, because they haven't necessarily committed
      // yet when I want to run service to update the widget for the first time:
      intent.putExtra(C.pref_key_exchange, prefs.getString(C.pref_key_exchange, C.CFG_VREX_LTC));
      intent.putExtra(C.pref_key_owc, prefs.getString(C.pref_key_owc, C.USD));
      intent.putExtra(C.pref_key_color, prefs.getString(C.pref_key_color, "light_grey"));
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
