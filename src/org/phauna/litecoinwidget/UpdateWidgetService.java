package org.phauna.litecoinwidget;

import android.os.AsyncTask;
import android.os.Build;
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
import java.util.Date;
import android.text.format.DateUtils;
import android.content.Context;
import android.widget.Toast;
import android.app.AlarmManager;

import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.Drawable;

import android.net.Uri;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.net.NetworkInfo;
import android.net.ConnectivityManager;

public class UpdateWidgetService extends Service {

  private int getWidgetLayout() {
    return R.layout.widget_layout;
  }

  private void toastIf(String msg, boolean cond) {
    if (cond) {
      Toast.makeText(this.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
  }

  // there are a number of cases in onStart that we don't want to update
  // the screen, but we still want to install the click handler. That's what
  // this method does.
  private void doNotUpdate(int widgetId, int updateInterval) {
    int layout_id = getWidgetLayout();
    RemoteViews remoteViews = new RemoteViews(UpdateWidgetService.this
      .getApplicationContext().getPackageName(),
      layout_id);
    refreshWhenClicked(widgetId, remoteViews, updateInterval);
    stopSelf();
  }

  @Override public void onStart(Intent intent, int startId) {

    int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    if (widgetId == -1) {
      stopSelf();
      return;
    }

    Log.d(C.LOG, "UpdateWidgetService.onStart " + widgetId);

    boolean isManualUpdate = intent.getBooleanExtra(C.EXTRA_IS_MANUAL_UPDATE, false);

    SharedPreferences prefs =
      this.getApplicationContext().getSharedPreferences("widget" + widgetId, Context.MODE_PRIVATE);

    String updateIntervalString = intent.getStringExtra(C.pref_key_interval);
    if (updateIntervalString == null) {
      updateIntervalString = prefs.getString(C.pref_key_interval, C.DEFAULT_INTERVAL);
    }

    int updateInterval = Integer.parseInt(updateIntervalString);
    String exchangeId = intent.getStringExtra(C.pref_key_exchange);
    if (exchangeId == null) {
      exchangeId = prefs.getString(C.pref_key_exchange, null);
      if (exchangeId == null) {
        doNotUpdate(widgetId, updateInterval);
        return;
      }
    }
    String coin = intent.getStringExtra(C.pref_key_coin);
    if (coin == null) {
      coin = prefs.getString(C.pref_key_coin, null);
      if (coin == null) {
        doNotUpdate(widgetId, updateInterval);
        return;
      }
    }
    String oldWorldCurrency = intent.getStringExtra(C.pref_key_owc);
    if (oldWorldCurrency == null) {
      oldWorldCurrency = prefs.getString(C.pref_key_owc, C.USD);
    }
    int txtColor = intent.getIntExtra(C.pref_key_txtcolor, -1);
    if (txtColor == -1) {
      txtColor = prefs.getInt(C.pref_key_txtcolor, C.DEFAULT_COLOR_TEXT);
    }
    int bgColor = intent.getIntExtra(C.pref_key_bgcolor, -1);
    if (bgColor == -1) {
      bgColor = prefs.getInt(C.pref_key_bgcolor, C.DEFAULT_COLOR_BG);
    }

    ConnectivityManager connMgr = (ConnectivityManager)
      getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo == null || (!networkInfo.isConnected())) {
      toastIf("LitecoinWidget: no network connection", isManualUpdate);
      doNotUpdate(widgetId, updateInterval);
      return;
    }

    toastIf("LitecoinWidget: refreshing...", isManualUpdate);
    // it sure would be nice to have an easier way than passing all this shite through here.
    // BUT there appear to be concurrency issues with using fields.
    new GetPriceTask().execute(new PriceTaskArgs(widgetId, exchangeId, coin, oldWorldCurrency, txtColor, bgColor, isManualUpdate, updateInterval));

    stopSelf();
    super.onStart(intent, startId);
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
      PriceTaskArgs arg = args[0];
      String eid = arg.mExchangeId;
      String coin = arg.mCoin;
      int wid = arg.mWidgetId;
      int updateInterval = arg.mUpdateInterval;
      String owc = arg.mOldWorldCurrency;
      double priceBTC = 0;
      double priceOWC = 0;
      Toaster toaster = null;
      if (arg.mIsManualUpdate) {
        toaster = new Toaster();
      }
      Downloaders downloaders = new Downloaders(toaster);
      BtcPriceCache cache = new BtcPriceCache(UpdateWidgetService.this.getApplicationContext());
      boolean estimatedPriceOWC = false;
      if (eid.equals(C.EXCH_VREX)) {
        priceBTC = downloaders.getVircurexPrice(coin);
      } else if (eid.equals(C.EXCH_BTCE)) {
        if (coin.equals("BTC") || coin.equals("LTC")) {
          if (owc.equals("RUR") || owc.equals("USD")) {
            priceOWC = downloaders.getBtcePrice(coin, owc.toLowerCase());
          } else {
            priceOWC = downloaders.getBtcePrice(coin, "usd");
            priceOWC = convertFromUSD(downloaders, priceOWC, owc);
            estimatedPriceOWC = true;
          }
        }
        if (!coin.equals("BTC")) {
          priceBTC = downloaders.getBtcePrice(coin, "btc");
        }
      } else if (eid.equals(C.EXCH_MGOX)) {
        priceOWC = downloaders.getMtgoxPrice();
        cache.updatePrice(eid, (float) priceOWC);
        if (!owc.equals("USD")) {
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
      } else if (eid.equals(C.EXCH_CBSE)) {
        priceOWC = downloaders.getCoinbasePrice();
        cache.updatePrice(eid, (float) priceOWC);
        if (!owc.equals("USD")) {
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
      } else if (eid.equals(C.EXCH_BSTP)) {
        priceOWC = downloaders.getBitstampPrice();
        cache.updatePrice(eid, (float) priceOWC);
        if (!owc.equals("USD")) {
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
      } else if (eid.equals(C.EXCH_CPBX)) {
        priceOWC = downloaders.getCampBXPrice();
        cache.updatePrice(eid, (float) priceOWC);
        if (!owc.equals("USD")) {
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
      }
      if (!cache.hasRecentPrice()) {
        cache.updatePrice(C.EXCH_MGOX, (float) downloaders.getMtgoxPrice());
      }
      cache.save(UpdateWidgetService.this.getApplicationContext());
      double mostRecentBtcPrice = cache.getPrice();
      if (priceOWC == 0) {
        priceOWC = priceBTC * mostRecentBtcPrice;
        estimatedPriceOWC = true;
        if (!owc.equals(C.USD)) {
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
      }
      return new PriceInfo(arg, priceBTC, priceOWC, estimatedPriceOWC);
    }

    @Override protected void onPostExecute(PriceInfo result) {
      RemoteViews remoteViews = new RemoteViews(UpdateWidgetService.this
        .getApplicationContext().getPackageName(), getWidgetLayout());

      PriceTaskArgs args = result.getPriceTaskArgs();
      String exch = args.mExchangeId;
      String coin = args.mCoin;
      int picId = R.drawable.litecoin;
      if (coin.equals("LTC")) {
        picId = R.drawable.litecoin;
      } else if (coin.equals("BTC")) {
        picId = R.drawable.bitcoin;
      } else if (coin.equals("NMC")) {
        picId = R.drawable.namecoin;
      } else if (coin.equals("PPC")) {
        picId = R.drawable.ppcoin;
      } else if (coin.equals("TRC")) {
        picId = R.drawable.terracoin;
      } else if (coin.equals("FTC")) {
        picId = R.drawable.feathercoin;
      } else if (coin.equals("FRC")) {
        picId = R.drawable.freicoin;
      } else if (coin.equals("NVC")) {
        picId = R.drawable.novacoin;
      } else if (coin.equals("DVC")) {
        picId = R.drawable.devcoin;
      }
      remoteViews.setImageViewResource(R.id.widgetpic, picId);
      double btcDouble = result.getPriceBTC();
      double owcDouble = result.getPriceOWC();
      // only update if we got some nonzero result:
      if (btcDouble != 0 || owcDouble != 0) {
        // generate a pretty BTC string (if any):
        String btcString = "";
        if (btcDouble != 0) {
          // extra precision for these coins:
          if (coin.equals("PPC") || coin.equals("NMC") || coin.equals("FTC") || coin.equals ("FRC") || coin.equals("DVC")) {
            btcString = "B" + roundBTCX(btcDouble);
          } else {
            btcString = "B" + roundBTC(btcDouble);
          }
        }
        // generate a pretty USD string (if any):
        String owcString = "";
        if (owcDouble != 0) {
          owcString = C.currencySymbol(args.mOldWorldCurrency) + roundOWC(owcDouble);
          if (result.isEstimatedPriceOWC()) {
            owcString += "*";
          }
        }

        if (owcString.equals("")) {
          owcString = " ";
        }

        if (btcString.equals("")) {
          // to make nice even spacing, put a blank space in for the BTC
          // price when there is none.
          btcString = " ";
        }

        remoteViews.setTextViewText(R.id.priceOWC, owcString);
        remoteViews.setTextViewText(R.id.priceBTC, btcString);

        long now = new Date().getTime();
        String dateText = DateUtils.formatSameDayTime(
          now, now, java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
        ).toString();
        remoteViews.setTextViewText(R.id.time, dateText);
      }
      remoteViews.setTextViewText(R.id.exchange_name, C.exchangeName(exch));
      // set text colors:
      int color = args.mTxtColor;
      remoteViews.setTextColor(R.id.exchange_name, color);
      remoteViews.setTextColor(R.id.priceBTC, color);
      remoteViews.setTextColor(R.id.priceOWC, color);
      remoteViews.setTextColor(R.id.time, color);

      // set background color and transparency:
      int bgColor = args.mBgColor;
      remoteViews.setImageViewBitmap(R.id.BackgroundImageView,
        getBackground(UpdateWidgetService.this.getApplicationContext(), bgColor)
      );

      refreshWhenClicked(args.mWidgetId, remoteViews, args.mUpdateInterval);

    }

  }

  void refreshWhenClicked(int widgetId, RemoteViews remoteViews, int updateInterval) {
    Intent clickIntent = new Intent(this.getApplicationContext(), UpdateWidgetService.class);
    clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
    clickIntent.putExtra(C.EXTRA_IS_MANUAL_UPDATE, true);
    // to make the intent unique, otherwise they all wind up referencing the same intent:
    clickIntent.setData(Uri.parse("widget:" + widgetId));

    PendingIntent pendingIntent = PendingIntent.getService(
        UpdateWidgetService.this.getApplicationContext(),
        0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    remoteViews.setOnClickPendingIntent(R.id.widgetframe, pendingIntent);
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
      UpdateWidgetService.this.getApplicationContext()
    );
    appWidgetManager.updateAppWidget(widgetId, remoteViews);

    // also set up the alarm:
    if (updateInterval != 1800) {
      AlarmManager am = (AlarmManager) this.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
      Intent intent = new Intent(this.getApplicationContext(), UpdateWidgetService.class);
      intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
      intent.setData(Uri.parse("widget:" + widgetId));
      PendingIntent pi = PendingIntent.getService(
          UpdateWidgetService.this.getApplicationContext(),
          0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * updateInterval, pi);
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

  public static Drawable getDrawable(Context c, int bgcolor) {
    float[] corners = {8,8,8,8,8,8,8,8};
    ShapeDrawable d = new ShapeDrawable(new RoundRectShape(corners,null,corners));
    d.getPaint().setColor(bgcolor);
    //d.getPaint().setAlpha(Color.alpha(bgcolor));
    return d;
  }

  public static Bitmap getBackground(Context c, int bgcolor) {
    try {
      Bitmap.Config config = Bitmap.Config.ARGB_8888;
        // Bitmap.Config.ARGB_8888 Bitmap.Config.ARGB_4444 to be used as
        // these two config constant supports transparency
      Bitmap bitmap = Bitmap.createBitmap(200, 200, config); // Create a Bitmap
      Canvas canvas =  new Canvas(bitmap); // Load the Bitmap to the Canvas
      RectF r = new RectF(0, 0, 200, 200);
      Paint paint = new Paint();
      paint.setColor(bgcolor);
      paint.setAlpha(Color.alpha(bgcolor));
      canvas.drawRoundRect(r, 15, 15, paint);
      return bitmap;
    } catch (Exception e) {
        return null;
    }
  }

}
