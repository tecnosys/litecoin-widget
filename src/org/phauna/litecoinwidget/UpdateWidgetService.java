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
import android.widget.Toast;

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
      exchangeId = prefs.getString(C.pref_key_exchange, C.CFG_INVALID);
      if (exchangeId.equals(C.CFG_INVALID)) {
        stopSelf();
        return;
      }
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
    String transparencyLevel = intent.getStringExtra(C.pref_key_trans);
    if (transparencyLevel == null) {
      transparencyLevel = prefs.getString(C.pref_key_trans, C.TRANS_MEDIUM);
    }

    ConnectivityManager connMgr = (ConnectivityManager)
      getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo == null || (!networkInfo.isConnected())) {
      Log.w(C.LOG, "no internet connection");
    } else {
      // it sure would be nice to have an easier way than passing all this shite through here.
      // BUT there appear to be concurrency issues with using fields.
      new GetPriceTask().execute(new PriceTaskArgs(widgetId, exchangeId, oldWorldCurrency, color, transparencyLevel));
    }

    stopSelf();

    super.onStart(intent, startId);
  }

  public class PriceTaskArgs {
    public int mWidgetId;
    public String mExchangeId;
    public String mOldWorldCurrency;
    public int mColor;
    public String mTransparencyLevel;
    public PriceTaskArgs(int widgetId, String exchangeId, String oldWorldCurrency, int color, String transparencyLevel) {
      mWidgetId = widgetId;
      mExchangeId = exchangeId;
      mOldWorldCurrency = oldWorldCurrency;
      mColor = color;
      mTransparencyLevel = transparencyLevel;
    }
  }

  public class GetPriceTask extends AsyncTask<PriceTaskArgs, Toasty, PriceInfo> {

    public class Toaster {
      public void toast(Toasty msg) {
        publishProgress(msg);
      }
    }

    @Override protected void onProgressUpdate(Toasty... msg) {
      Toasty ty = msg[0];
      Toast.makeText(UpdateWidgetService.this.getApplicationContext(), ty.mMsg, ty.mLength).show();
    }

    @Override protected PriceInfo doInBackground(PriceTaskArgs... args) {
      String eid = args[0].mExchangeId;
      int wid = args[0].mWidgetId;
      String owc = args[0].mOldWorldCurrency;
      double priceBTC = 0;
      double priceUSD = 0;
      Downloaders downloaders = new Downloaders(new Toaster());
      BtcPriceCache cache = new BtcPriceCache(UpdateWidgetService.this.getApplicationContext());
      if (eid.equals(C.CFG_VREX_LTC)) {
        priceBTC = downloaders.getVircurexPrice("LTC");
      } else if (eid.equals(C.CFG_VREX_NMC)) {
        priceBTC = downloaders.getVircurexPrice("NMC");
      } else if (eid.equals(C.CFG_VREX_PPC)) {
        priceBTC = downloaders.getVircurexPrice("PPC");
      } else if (eid.equals(C.CFG_BTCE_PPC)) {
        priceBTC = downloaders.getBtcePrice("ppc", "btc");
      } else if (eid.equals(C.CFG_BTCE_NMC)) {
        priceBTC = downloaders.getBtcePrice("nmc", "btc");
      } else if (eid.equals(C.CFG_BTCE_LTC)) {
        priceBTC = downloaders.getBtcePrice("ltc", "btc");
        priceUSD = downloaders.getBtcePrice("ltc", "usd");
      } else if (eid.equals(C.CFG_BFLR_BTC)) {
        priceUSD = downloaders.getBitfloorPriceBTCUSD();
        cache.updatePrice(eid, (float) priceUSD);
      } else if (eid.equals(C.CFG_BTCE_BTC)) {
        priceUSD = downloaders.getBtcePrice("btc", "usd");
        cache.updatePrice(eid, (float) priceUSD);
      } else if (eid.equals(C.CFG_MGOX_BTC)) {
        priceUSD = downloaders.getMtgoxPrice();
        cache.updatePrice(eid, (float) priceUSD);
      }
      if (!cache.hasRecentPrice()) {
        cache.updatePrice(C.CFG_BFLR_BTC, (float) downloaders.getBitfloorPriceBTCUSD());
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
        priceOWC = convertFromUSD(downloaders, priceOWC, owc);
        estimatedPriceOWC = true;
      }
      return new PriceInfo(eid, priceBTC, owc, priceOWC, estimatedPriceOWC, wid, args[0].mColor, args[0].mTransparencyLevel);
    }

    @Override protected void onPostExecute(PriceInfo result) {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
        UpdateWidgetService.this.getApplicationContext()
      );
      int layout_id = R.layout.widget_layout_two;
      String transLevel = result.getTransparencyLevel();
      if (transLevel.equals(C.TRANS_HIGH)) {
        layout_id = R.layout.widget_layout_one;
      } else if (transLevel.equals(C.TRANS_LOW)) {
        layout_id = R.layout.widget_layout_three;
      }
      RemoteViews remoteViews = new RemoteViews(UpdateWidgetService.this
        .getApplicationContext().getPackageName(),
        layout_id);

      String cfg = result.getExchangeConfig();
      if (cfg.equals(C.CFG_VREX_LTC) || cfg.equals(C.CFG_BTCE_LTC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.litecoin);
      } else if (cfg.equals(C.CFG_BFLR_BTC) || cfg.equals(C.CFG_BTCE_BTC) || cfg.equals(C.CFG_MGOX_BTC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.bitcoin);
      } else if (cfg.equals(C.CFG_VREX_NMC) || cfg.equals(C.CFG_BTCE_NMC)) {
        remoteViews.setImageViewResource(R.id.widgetpic, R.drawable.namecoin);
      } else if (cfg.equals(C.CFG_VREX_PPC) || cfg.equals(C.CFG_BTCE_PPC)) {
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
          // to make nice even spacing, put a blank space in for the BTC
          // price when there is none.
          btcString = " ";
        }
        remoteViews.setTextViewText(R.id.priceBTC, btcString);

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

  static double convertFromUSD(Downloaders downloaders, double priceUSD, String toCurrency) {
    double rate = downloaders.getCachedExchangeRate(C.USD, toCurrency);
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
