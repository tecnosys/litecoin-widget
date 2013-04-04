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

  @Override public void onStart(Intent intent, int startId) {

    int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    if (widgetId == -1) {
      stopSelf();
      return;
    }

    SharedPreferences prefs =
      this.getApplicationContext().getSharedPreferences("widget" + widgetId, Context.MODE_PRIVATE);

    String exchangeId = intent.getStringExtra(C.pref_key_exchange);
    if (exchangeId == null) {
      exchangeId = prefs.getString(C.pref_key_exchange, C.CFG_VREX_LTC);
    }
    String oldWorldCurrency = intent.getStringExtra(C.pref_key_owc);
    if (oldWorldCurrency == null) {
      oldWorldCurrency = prefs.getString(C.pref_key_owc, C.USD);
    }
    String colorString = intent.getStringExtra(C.pref_key_color);
    if (colorString == null) {
      colorString = prefs.getString(C.pref_key_color, "light_grey");
    }
    int color = C.getColor(colorString);

    ConnectivityManager connMgr = (ConnectivityManager)
      getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo == null || (!networkInfo.isConnected())) {
      Log.w(C.LOG, "no internet connection");
    } else {
      new GetPriceTask().execute(new PriceTaskArgs(widgetId, exchangeId, oldWorldCurrency, color));
    }

    stopSelf();

    super.onStart(intent, startId);
  }

  public class PriceTaskArgs {
    public int mWidgetId;
    public String mExchangeId;
    public String mOldWorldCurrency;
    public int mColor;
    public PriceTaskArgs(int widgetId, String exchangeId, String oldWorldCurrency, int color) {
      mWidgetId = widgetId;
      mExchangeId = exchangeId;
      mOldWorldCurrency = oldWorldCurrency;
      mColor = color;
    }
  }

  private class GetPriceTask extends AsyncTask<PriceTaskArgs, Void, PriceInfo> {

    @Override protected PriceInfo doInBackground(PriceTaskArgs... args) {
      String eid = args[0].mExchangeId;
      int wid = args[0].mWidgetId;
      String owc = args[0].mOldWorldCurrency;
      double priceBTC = 0;
      double priceUSD = 0;
      BtcPriceCache cache = new BtcPriceCache(UpdateWidgetService.this.getApplicationContext());
      if (eid.equals(C.CFG_VREX_LTC)) {
        priceBTC = Downloaders.getVircurexPrice("LTC");
      } else if (eid.equals(C.CFG_VREX_NMC)) {
        priceBTC = Downloaders.getVircurexPrice("NMC");
      } else if (eid.equals(C.CFG_VREX_PPC)) {
        priceBTC = Downloaders.getVircurexPrice("PPC");
      } else if (eid.equals(C.CFG_BTCE_LTC)) {
        priceBTC = Downloaders.getBtcePrice("ltc", "btc");
        priceUSD = Downloaders.getBtcePrice("ltc", "usd");
      } else if (eid.equals(C.CFG_BFLR_BTC)) {
        priceUSD = Downloaders.getBitfloorPriceBTCUSD();
        cache.updatePrice(eid, (float) priceUSD);
      } else if (eid.equals(C.CFG_BTCE_BTC)) {
        priceUSD = Downloaders.getBtcePrice("btc", "usd");
        cache.updatePrice(eid, (float) priceUSD);
      } else if (eid.equals(C.CFG_MGOX_BTC)) {
        priceUSD = Downloaders.getMtgoxPrice();
        cache.updatePrice(eid, (float) priceUSD);
      }
      if (!cache.hasRecentPrice()) {
        cache.updatePrice(C.CFG_BFLR_BTC, (float) Downloaders.getBitfloorPriceBTCUSD());
      }
      cache.save(UpdateWidgetService.this.getApplicationContext());
      double mostRecentBtcPrice = cache.getPrice();
      boolean estimatedPriceOWC = false;
      if (priceUSD == 0) {
        priceUSD = priceBTC * mostRecentBtcPrice;
        estimatedPriceOWC = true;
      }
      double priceOWC = priceUSD;
      if (!owc.equals(C.USD)) {
        priceOWC = convertFromUSD(priceOWC, owc);
        estimatedPriceOWC = true;
      }
      return new PriceInfo(eid, priceBTC, owc, priceOWC, estimatedPriceOWC, wid, args[0].mColor);
    }

    @Override protected void onPostExecute(PriceInfo result) {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
        UpdateWidgetService.this.getApplicationContext()
      );
      RemoteViews remoteViews = new RemoteViews(UpdateWidgetService.this
        .getApplicationContext().getPackageName(),
        R.layout.widget_layout);

      String cfg = result.getExchangeConfig();
      if (cfg.equals(C.CFG_VREX_LTC) || cfg.equals(C.CFG_BTCE_LTC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.litecoin);
      } else if (cfg.equals(C.CFG_BFLR_BTC) || cfg.equals(C.CFG_BTCE_BTC) || cfg.equals(C.CFG_MGOX_BTC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.bitcoin);
      } else if (cfg.equals(C.CFG_VREX_NMC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.namecoin);
      } else if (cfg.equals(C.CFG_VREX_PPC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.ppcoin);
      }
      double btcDouble = result.getPriceBTC();
      double owcDouble = result.getPriceOWC();
      // only update if we got some nonzero result:
      if (btcDouble != 0 || owcDouble != 0) {
        // generate a pretty BTC string (if any):
        String btcString = "";
        if (btcDouble != 0) {
          if (cfg.equals(C.CFG_VREX_PPC)) {
            btcString = "B" + roundBTCX(btcDouble);
          } else {
            btcString = "B" + roundBTC(btcDouble);
          }
        }
        // generate a pretty USD string (if any):
        String owcString = "";
        if (owcDouble != 0) {
          owcString = C.currencySymbol(result.getOWC()) + roundOWC(owcDouble);
          if (result.isEstimatedPriceOWC()) {
            owcString += "*";
          }
        }

        if (owcString.equals("")) {
          remoteViews.setViewVisibility(R.id.priceOWC, View.GONE);
        } else {
          remoteViews.setViewVisibility(R.id.priceOWC, View.VISIBLE);
          remoteViews.setTextViewText(R.id.priceOWC, owcString);
        }

        if (btcString.equals("")) {
          remoteViews.setViewVisibility(R.id.priceBTC, View.GONE);
        } else {
          remoteViews.setViewVisibility(R.id.priceBTC, View.VISIBLE);
          remoteViews.setTextViewText(R.id.priceBTC, btcString);
        }

        long now = new Date().getTime();
        String dateText = DateUtils.formatSameDayTime(
          now, now, java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
        ).toString();
        remoteViews.setTextViewText(R.id.time, dateText);
      }
      remoteViews.setTextViewText(R.id.exchange_name, C.exchangeName(cfg));
      // set text colors:
      int color = result.getColor();
      remoteViews.setTextColor(R.id.exchange_name, color);
      remoteViews.setTextColor(R.id.priceBTC, color);
      remoteViews.setTextColor(R.id.priceOWC, color);
      remoteViews.setTextColor(R.id.time, color);

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
      appWidgetManager.updateAppWidget(result.getWidgetId(), remoteViews);
    }

  }

  static double convertFromUSD(double priceUSD, String toCurrency) {
    double rate = Downloaders.getCachedExchangeRate(C.USD, toCurrency);
    if (rate == -1) {
      return 0;
    }
    return (priceUSD * rate);
  }

  static String roundBTC(double d) {
    DecimalFormat df = new DecimalFormat("#.0000");
    return df.format(d);
  }

  static String roundBTCX(double d) {
    DecimalFormat df = new DecimalFormat("#.00000");
    return df.format(d);
  }

  static String roundOWC(double d) {
    DecimalFormat df = new DecimalFormat("0.00");
    return df.format(d);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

}
