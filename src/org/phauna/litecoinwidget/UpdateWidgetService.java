package org.phauna.litecoinwidget;

import java.text.DecimalFormat;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.graphics.Color;
import java.util.Date;
import android.text.format.DateUtils;
import android.content.Context;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class UpdateWidgetService extends Service {

  @Override
  public void onStart(Intent intent, int startId) {

    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
        .getApplicationContext());

    int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

    for (int widgetId : allWidgetIds) {

      SharedPreferences prefs =
        this.getApplicationContext().getSharedPreferences("widget" + widgetId, Context.MODE_PRIVATE);

      String exchange = prefs.getString(C.pref_key_exchange, C.EXCHANGE_VIRCUREX);

      RemoteViews remoteViews = new RemoteViews(this
          .getApplicationContext().getPackageName(),
          R.layout.widget_layout);

      if (exchange.equals(C.EXCHANGE_VIRCUREX)) {
        Log.d(C.LOG, "exchange is vircurex");
        // vircurex only has BTC price
        double priceBTC = Downloaders.getVircurexPriceBTC();
        remoteViews.setTextViewText(R.id.priceBTC, "B" + roundBTC(priceBTC));
        remoteViews.setViewVisibility(R.id.priceUSD, View.GONE);
      } else if (exchange.equals(C.EXCHANGE_BTCE)) {
        Log.d(C.LOG, "exchange is btc-e");
        // get both prices
        double priceBTC = Downloaders.getBtcePriceBTC();
        double priceUSD = Downloaders.getBtcePriceUSD();
        remoteViews.setTextViewText(R.id.priceBTC, "B" + roundBTC(priceBTC));
        remoteViews.setViewVisibility(R.id.priceUSD, View.VISIBLE);
        remoteViews.setTextViewText(R.id.priceUSD, "$" + roundBTC(priceUSD));
      }

      long now = new Date().getTime();
      String dateText = DateUtils.formatSameDayTime(
        now, now, java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
      ).toString();
      remoteViews.setTextViewText(R.id.exchange_name, C.exchangeName(exchange));
      remoteViews.setTextViewText(R.id.time, dateText);

      // refresh when clicked
      Intent clickIntent = new Intent(this.getApplicationContext(), MyWidgetProvider.class);

      clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
      //clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
      //clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

      //PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(),
       //  widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      // update ALL widgets at once when you click
      PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(),
          0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      remoteViews.setOnClickPendingIntent(R.id.widgetframe, pendingIntent);
      appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }
    stopSelf();

    super.onStart(intent, startId);
  }

  String roundBTC(double d) {
    DecimalFormat df = new DecimalFormat("#.####");
    return df.format(d);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

}
