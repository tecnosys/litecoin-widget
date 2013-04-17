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

  @Override public void onStart(Intent intent, int startId) {

    int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    if (widgetId == -1) {
      stopSelf();
      return;
    }

    boolean isManualUpdate = intent.getBooleanExtra(C.EXTRA_IS_MANUAL_UPDATE, false);

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
      int layout_id = getWidgetLayout();
      RemoteViews remoteViews = new RemoteViews(UpdateWidgetService.this
        .getApplicationContext().getPackageName(),
        layout_id);
      refreshWhenClicked(widgetId, remoteViews);
    } else {
      toastIf("LitecoinWidget: refreshing...", isManualUpdate);
      // it sure would be nice to have an easier way than passing all this shite through here.
      // BUT there appear to be concurrency issues with using fields.
      new GetPriceTask().execute(new PriceTaskArgs(widgetId, exchangeId, oldWorldCurrency, txtColor, bgColor, isManualUpdate));
    }

    stopSelf();

    super.onStart(intent, startId);
  }

  public class PriceTaskArgs {
    public int mWidgetId;
    public String mExchangeId;
    public String mOldWorldCurrency;
    public int mTxtColor;
    public int mBgColor;
    public boolean mIsManualUpdate;
    public PriceTaskArgs(int widgetId, String exchangeId, String oldWorldCurrency, int txtColor, int bgColor, boolean isManualUpdate) {
      mWidgetId = widgetId;
      mExchangeId = exchangeId;
      mOldWorldCurrency = oldWorldCurrency;
      mTxtColor = txtColor;
      mBgColor = bgColor;
      mIsManualUpdate = isManualUpdate;
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
      PriceTaskArgs arg = args[0];
      String eid = arg.mExchangeId;
      int wid = arg.mWidgetId;
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
        if (owc.equals("RUR") || owc.equals("USD")) {
          priceOWC = downloaders.getBtcePrice("ltc", owc.toLowerCase());
        } else {
          priceOWC = downloaders.getBtcePrice("ltc", "usd");
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
      } else if (eid.equals(C.CFG_BFLR_BTC)) {
        priceOWC = downloaders.getBitfloorPriceBTCUSD();
        cache.updatePrice(eid, (float) priceOWC);
        if (!owc.equals("USD")) {
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
      } else if (eid.equals(C.CFG_BTCE_BTC)) {
        if (owc.equals("RUR") || owc.equals("USD")) {
          priceOWC = downloaders.getBtcePrice("btc", owc.toLowerCase());
        } else {
          priceOWC = downloaders.getBtcePrice("btc", "usd");
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
        if (owc.equals("USD")) {
          cache.updatePrice(eid, (float) priceOWC);
        }
      } else if (eid.equals(C.CFG_MGOX_BTC)) {
        priceOWC = downloaders.getMtgoxPrice();
        cache.updatePrice(eid, (float) priceOWC);
        if (!owc.equals("USD")) {
          priceOWC = convertFromUSD(downloaders, priceOWC, owc);
          estimatedPriceOWC = true;
        }
      }
      if (!cache.hasRecentPrice()) {
        cache.updatePrice(C.CFG_MGOX_BTC, (float) downloaders.getMtgoxPrice());
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
      return new PriceInfo(eid, priceBTC, owc, priceOWC, estimatedPriceOWC, wid, arg.mTxtColor, arg.mBgColor);
    }

    @Override protected void onPostExecute(PriceInfo result) {
      RemoteViews remoteViews = new RemoteViews(UpdateWidgetService.this
        .getApplicationContext().getPackageName(), getWidgetLayout());

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
      int color = result.getTxtColor();
      remoteViews.setTextColor(R.id.exchange_name, color);
      remoteViews.setTextColor(R.id.priceBTC, color);
      remoteViews.setTextColor(R.id.priceOWC, color);
      remoteViews.setTextColor(R.id.time, color);

      // set background color and transparency:
      int bgColor = result.getBgColor();
      remoteViews.setImageViewBitmap(R.id.BackgroundImageView,
        getBackground(UpdateWidgetService.this.getApplicationContext(), bgColor)
      );

      refreshWhenClicked(result.getWidgetId(), remoteViews);

    }

  }

  void refreshWhenClicked(int widgetId, RemoteViews remoteViews) {
    Intent clickIntent = new Intent(this.getApplicationContext(),
        UpdateWidgetService.class);
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
