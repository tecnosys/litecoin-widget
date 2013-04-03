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

import android.net.Uri;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import android.net.NetworkInfo;
import android.net.ConnectivityManager;

public class UpdateWidgetService extends Service {

  @Override
  public void onStart(Intent intent, int startId) {

    int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    Log.d(C.LOG, "onStart, widgetId is " + widgetId);
    if (widgetId == -1) {
      stopSelf();
      return;
    }

    SharedPreferences prefs =
      this.getApplicationContext().getSharedPreferences("widget" + widgetId, Context.MODE_PRIVATE);

    String exchangeId = prefs.getString(C.pref_key_exchange, C.EXCHANGE_VIRCUREX);

    ConnectivityManager connMgr = (ConnectivityManager)
      getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo == null || (!networkInfo.isConnected())) {
      Log.d(C.LOG, "no internet connection");
    } else {
      new GetPriceTask().execute(new PriceTaskArgs(widgetId, exchangeId));
    }

    stopSelf();

    super.onStart(intent, startId);
  }

  public class PriceTaskArgs {
    public int mWidgetId;
    public String mExchangeId;
    public PriceTaskArgs(int widgetId, String exchangeId) {
      mWidgetId = widgetId;
      mExchangeId = exchangeId;
    }
  }

  private class GetPriceTask extends AsyncTask<PriceTaskArgs, Void, PriceInfo> {

    @Override protected PriceInfo doInBackground(PriceTaskArgs... args) {
      Log.d(C.LOG, "doInBackground");
      String eid = args[0].mExchangeId;
      int wid = args[0].mWidgetId;
      if (eid.equals(C.EXCHANGE_VIRCUREX)) {
        double priceBTC = Downloaders.getVircurexPrice("LTC");
        return new PriceInfo(eid, priceBTC, 0, wid);
      } else if (eid.equals(C.EXCHANGE_VIRCUREX_NMC)) {
        double priceBTC = Downloaders.getVircurexPrice("NMC");
        return new PriceInfo(eid, priceBTC, 0, wid);
      } else if (eid.equals(C.EXCHANGE_VIRCUREX_PPC)) {
        double priceBTC = Downloaders.getVircurexPrice("PPC");
        return new PriceInfo(eid, priceBTC, 0, wid);
      } else if (eid.equals(C.EXCHANGE_BTCE)) {
        double priceBTC = Downloaders.getBtcePrice("ltc", "btc");
        double priceUSD = Downloaders.getBtcePrice("ltc", "usd");
        return new PriceInfo(eid, priceBTC, priceUSD, wid);
      } else if (eid.equals(C.EXCHANGE_BITFLOOR)) {
        double price = Downloaders.getBitfloorPriceBTCUSD();
        return new PriceInfo(eid, 0, price, wid);
      } else if (eid.equals(C.EXCHANGE_BTCE_BTC)) {
        double price = Downloaders.getBtcePrice("btc", "usd");
        return new PriceInfo(eid, 0, price, wid);
      } else if (eid.equals(C.EXCHANGE_MTGOX)) {
        double price = Downloaders.getMtgoxPrice();
        return new PriceInfo(eid, 0, price, wid);
      }
      return null;
    }

    @Override protected void onPostExecute(PriceInfo result) {
      Log.d(C.LOG, "onPostExecute: " + result.getWidgetId());
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
        UpdateWidgetService.this.getApplicationContext()
      );
      RemoteViews remoteViews = new RemoteViews(UpdateWidgetService.this
        .getApplicationContext().getPackageName(),
        R.layout.widget_layout);

      String exchange = result.getExchangeConfig();
      if (exchange.equals(C.EXCHANGE_VIRCUREX) || exchange.equals(C.EXCHANGE_BTCE)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.litecoin);
      } else if (exchange.equals(C.EXCHANGE_BITFLOOR) || exchange.equals(C.EXCHANGE_BTCE_BTC) || exchange.equals(C.EXCHANGE_MTGOX)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.bitcoin);
      } else if (exchange.equals(C.EXCHANGE_VIRCUREX_NMC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.namecoin);
      } else if (exchange.equals(C.EXCHANGE_VIRCUREX_PPC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.ppcoin);
      }
      if (exchange.equals(C.EXCHANGE_VIRCUREX) || exchange.equals(C.EXCHANGE_VIRCUREX_NMC)) {
        remoteViews.setTextViewText(R.id.priceBTC, "B" + roundBTC(result.getPriceBTC()));
        remoteViews.setViewVisibility(R.id.priceUSD, View.GONE);
        remoteViews.setViewVisibility(R.id.priceBTC, View.VISIBLE);
      } else if (exchange.equals(C.EXCHANGE_VIRCUREX_PPC)) {
        // note "roundBTCX" below
        remoteViews.setTextViewText(R.id.priceBTC, "B" + roundBTCX(result.getPriceBTC()));
        remoteViews.setViewVisibility(R.id.priceUSD, View.GONE);
        remoteViews.setViewVisibility(R.id.priceBTC, View.VISIBLE);
      } else if (exchange.equals(C.EXCHANGE_BTCE)) {
        remoteViews.setTextViewText(R.id.priceBTC, "B" + roundBTC(result.getPriceBTC()));
        remoteViews.setViewVisibility(R.id.priceUSD, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.priceBTC, View.VISIBLE);
        remoteViews.setTextViewText(R.id.priceUSD, "$" + roundBTC(result.getPriceUSD()));
      } else if (exchange.equals(C.EXCHANGE_BTCE_BTC) || exchange.equals(C.EXCHANGE_MTGOX) || exchange.equals(C.EXCHANGE_BITFLOOR)) {
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
      clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, result.getWidgetId());
      // to make the intent unique, otherwise they all wind up referencing the same intent:
      clickIntent.setData(Uri.parse("widget:" + result.getWidgetId()));

      PendingIntent pendingIntent = PendingIntent.getService(
          UpdateWidgetService.this.getApplicationContext(),
          0, clickIntent, PendingIntent.FLAG_ONE_SHOT);
      remoteViews.setOnClickPendingIntent(R.id.widgetframe, pendingIntent);
      Log.d(C.LOG, "onPostExecute setting remoteviews for " + result.getWidgetId());
      appWidgetManager.updateAppWidget(result.getWidgetId(), remoteViews);
    }

  }

  static String roundBTC(double d) {
    DecimalFormat df = new DecimalFormat("#.####");
    return df.format(d);
  }

  static String roundBTCX(double d) {
    DecimalFormat df = new DecimalFormat("#.#####");
    return df.format(d);
  }

  static String roundUSD(double d) {
    DecimalFormat df = new DecimalFormat("#.##");
    return df.format(d);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

}
