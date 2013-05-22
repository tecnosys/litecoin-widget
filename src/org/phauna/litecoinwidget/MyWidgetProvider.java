package org.phauna.litecoinwidget;

import java.util.Random;

import android.app.PendingIntent;
import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.net.Uri;

/* could store the list of appWidgetIds myself because AppWidgetManager is returning
 * a bunch of previously-deleted ones, and that would reduce extraneous network
 * traffics.. */

public class MyWidgetProvider extends AppWidgetProvider {

  private void disableAlarm(Context context, int widgetId) {
    Log.d(C.LOG, "disableAlarm " + widgetId);
    Intent intent = new Intent(context, UpdateWidgetService.class);
    intent.setData(Uri.parse("widget:" + widgetId));
    PendingIntent pi = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(pi);
  }

  @Override
  public void onDeleted(Context context, int[] widgetIds) {
    for (int i = 0; i < widgetIds.length; i++) {
      disableAlarm(context, widgetIds[i]);
    }
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {

    Log.d(C.LOG, "MyWidgetProvider: onUpdate");

    // Get all ids
    ComponentName thisWidget = new ComponentName(context,
        MyWidgetProvider.class);
    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

    // use appWidgetIds here instead?
    for (int widgetId : allWidgetIds) {

      // Build the intent to call the service
      Intent intent = new Intent(context.getApplicationContext(),
          UpdateWidgetService.class);
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
      intent.putExtra(C.EXTRA_IS_MANUAL_UPDATE, false);

      // Update the widget via the service
      context.startService(intent);
    }

  }
}
