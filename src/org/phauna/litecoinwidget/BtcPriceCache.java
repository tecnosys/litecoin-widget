package org.phauna.litecoinwidget;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.content.Context;
import android.os.Build;
import android.os.AsyncTask;

import java.util.Date;

public class BtcPriceCache {

  private float  mPrice;
  private Date   mPriceDate;
  private String mPriceExchange;

  private boolean mHasChanged;

  public BtcPriceCache(Context c) {
    SharedPreferences globalPrefs = c.getSharedPreferences("global", Context.MODE_PRIVATE);
    mPrice = globalPrefs.getFloat(C.pref_key_rate_price, 0);
    mPriceDate = new Date(globalPrefs.getLong(C.pref_key_rate_date, 0));
    mPriceExchange = globalPrefs.getString(C.pref_key_rate_exchange, "");
    mHasChanged = false;
  }

  public void save(Context c) {
    if (mHasChanged) {
      new SaveTask().execute(c);
    }
    mHasChanged = false;
  }

  public class SaveTask extends AsyncTask<Context, Void, Void> {
    @Override protected Void doInBackground(Context... args) {
      Context c = args[0];
      SharedPreferences globalPrefs = c.getSharedPreferences("global", Context.MODE_PRIVATE);
      SharedPreferences.Editor e = globalPrefs.edit();
      e.putFloat(C.pref_key_rate_price, mPrice);
      e.putLong(C.pref_key_rate_date, mPriceDate.getTime());
      e.putString(C.pref_key_rate_exchange, mPriceExchange);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        e.apply();
      }
      else {
        e.commit();
      }
      return null;
    }
    @Override protected void onPostExecute(Void result) {
    }
  }

  public float getPrice() {
    Date now = new Date();
    if (now.getTime() - mPriceDate.getTime() > 1000*60*5) {
      return 0;
    }
    return mPrice;
  }

  public boolean hasRecentPrice() {
    return (getPrice() != 0);
  }

  public static int scoreExchange(String e) {
    if (e.equals(C.EXCH_MGOX)) return 2;
    if (e.equals(C.EXCH_BTCE)) return 1;
    return 0;
  }

  public static boolean betterExchange(String e1, String e2) {
    return (scoreExchange(e1) >= scoreExchange(e2));
  }

  public void updatePrice(String newPriceExchange, float newPrice) {
    assert(newPriceExchange.equals(C.EXCH_BTCE)
        || newPriceExchange.equals(C.EXCH_MGOX));
    Date now = new Date();
    if (now.getTime() - mPriceDate.getTime() > 1000*60*5) {
      // if we're older than five minutes, update no matter what
      mPrice = newPrice;
      mPriceDate = now;
      mPriceExchange = newPriceExchange;
      mHasChanged = true;
    } else {
      // otherwise, only update if the exchange is "better"
      if (betterExchange(newPriceExchange, mPriceExchange)) {
        mPrice = newPrice;
        mPriceDate = now;
        mPriceExchange = newPriceExchange;
        mHasChanged = true;
      }
    }
  }

}
