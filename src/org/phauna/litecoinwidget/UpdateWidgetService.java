package org.phauna.litecoinwidget;

import android.os.AsyncTask;
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

import android.net.NetworkInfo;
import android.net.ConnectivityManager;

public class UpdateWidgetService extends Service {

  int mWidgetId;
  String mExchangeId;

  @Override
  public void onStart(Intent intent, int startId) {

    mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    Log.d(C.LOG, "onStart, widgetId is " + mWidgetId);
    if (mWidgetId == -1) {
      stopSelf();
      return;
    }

    SharedPreferences prefs =
      this.getApplicationContext().getSharedPreferences("widget" + mWidgetId, Context.MODE_PRIVATE);

    mExchangeId = prefs.getString(C.pref_key_exchange, C.EXCHANGE_VIRCUREX);

    ConnectivityManager connMgr = (ConnectivityManager)
      getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo == null || (!networkInfo.isConnected())) {
      Log.d(C.LOG, "no internet connection");
    } else {
      new GetPriceTask().execute();
    }

    stopSelf();

    super.onStart(intent, startId);
  }

  private class GetPriceTask extends AsyncTask<Void, Void, PriceInfo> {

    @Override protected PriceInfo doInBackground(Void... unused) {
      Log.d(C.LOG, "doInBackground");
      if (mExchangeId.equals(C.EXCHANGE_VIRCUREX)) {
        // vircurex only has BTC price
        double priceBTC = Downloaders.getVircurexPriceBTC();
        return new PriceInfo(mExchangeId, priceBTC, 0);
      } else if (mExchangeId.equals(C.EXCHANGE_BTCE)) {
        // get both prices
        double priceBTC = Downloaders.getBtcePriceBTC();
        double priceUSD = Downloaders.getBtcePriceUSD();
        return new PriceInfo(mExchangeId, priceBTC, priceUSD);
      } else if (mExchangeId.equals(C.EXCHANGE_BITFLOOR)) {
        double price = Downloaders.getBitfloorPriceBTCUSD();
        return new PriceInfo(mExchangeId, 0, price);
      } else if (mExchangeId.equals(C.EXCHANGE_BTCE_BTC)) {
        double price = Downloaders.getBtcePriceBTCUSD();
        return new PriceInfo(mExchangeId, 0, price);
      }
      return null;
    }

    @Override protected void onPostExecute(PriceInfo result) {
      Log.d(C.LOG, "in onPostExecute");
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
        UpdateWidgetService.this.getApplicationContext()
      );
      RemoteViews remoteViews = new RemoteViews(UpdateWidgetService.this
        .getApplicationContext().getPackageName(),
        R.layout.widget_layout);

      String exchange = result.getExchangeConfig();
      if (exchange.equals(C.EXCHANGE_VIRCUREX) || exchange.equals(C.EXCHANGE_BTCE)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.litecoin);
      } else if (exchange.equals(C.EXCHANGE_BITFLOOR) || exchange.equals(C.EXCHANGE_BTCE_BTC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.bitcoin);
      }
      if (exchange.equals(C.EXCHANGE_VIRCUREX)) {
        // vircurex only has BTC price
        remoteViews.setTextViewText(R.id.priceBTC, "B" + roundBTC(result.getPriceBTC()));
        remoteViews.setViewVisibility(R.id.priceUSD, View.GONE);
        remoteViews.setViewVisibility(R.id.priceBTC, View.VISIBLE);
      } else if (exchange.equals(C.EXCHANGE_BTCE)) {
        remoteViews.setTextViewText(R.id.priceBTC, "B" + roundBTC(result.getPriceBTC()));
        remoteViews.setViewVisibility(R.id.priceUSD, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.priceBTC, View.VISIBLE);
        remoteViews.setTextViewText(R.id.priceUSD, "$" + roundBTC(result.getPriceUSD()));
      } else if (exchange.equals(C.EXCHANGE_BITFLOOR)) {
        remoteViews.setTextViewText(R.id.priceUSD, "$" + roundUSD(result.getPriceUSD()));
        remoteViews.setViewVisibility(R.id.priceUSD, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.priceBTC, View.GONE);
      } else if (exchange.equals(C.EXCHANGE_BTCE_BTC)) {
        remoteViews.setTextViewText(R.id.priceUSD, "$" + roundUSD(result.getPriceUSD()));
        remoteViews.setViewVisibility(R.id.priceUSD, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.priceBTC, View.GONE);
      }
      long now = new Date().getTime();
      String dateText = DateUtils.formatSameDayTime(
        now, now, java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
      ).toString();
      remoteViews.setTextViewText(R.id.exchange_name, C.exchangeName(exchange));
      remoteViews.setTextViewText(R.id.time, dateText);

      // refresh when clicked
      Intent clickIntent = new Intent(UpdateWidgetService.this.getApplicationContext(),
          UpdateWidgetService.class);
      clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
      clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);

      // update ALL widgets at once when you click
      PendingIntent pendingIntent = PendingIntent.getService(
          UpdateWidgetService.this.getApplicationContext(),
          0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      remoteViews.setOnClickPendingIntent(R.id.widgetframe, pendingIntent);
      appWidgetManager.updateAppWidget(mWidgetId, remoteViews);
    }

  }

  String roundBTC(double d) {
    DecimalFormat df = new DecimalFormat("#.####");
    return df.format(d);
  }

  String roundUSD(double d) {
    DecimalFormat df = new DecimalFormat("#.##");
    return df.format(d);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

}
