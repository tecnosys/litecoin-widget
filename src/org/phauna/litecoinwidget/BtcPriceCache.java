package org.phauna.litecoinwidget;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.content.Context;

import java.util.Date;

public class BtcPriceCache {

  private float  mPrice;
  private Date   mPriceDate;
  private String mPriceExchange;

  public BtcPriceCache(Context c) {
    SharedPreferences globalPrefs = c.getSharedPreferences("global", Context.MODE_PRIVATE);
    mPrice = globalPrefs.getFloat(C.pref_key_rate_price, 0);
    mPriceDate = new Date(globalPrefs.getLong(C.pref_key_rate_date, 0));
    mPriceExchange = globalPrefs.getString(C.pref_key_rate_exchange, "");
  }

  public void save(Context c) {
    SharedPreferences globalPrefs = c.getSharedPreferences("global", Context.MODE_PRIVATE);
    SharedPreferences.Editor e = globalPrefs.edit();
    e.putFloat(C.pref_key_rate_price, mPrice);
    e.putLong(C.pref_key_rate_date, mPriceDate.getTime());
    e.putString(C.pref_key_rate_exchange, mPriceExchange);
    e.apply();
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
    if (e.equals(C.CFG_MGOX_BTC)) return 3;
    if (e.equals(C.CFG_BFLR_BTC)) return 2;
    if (e.equals(C.CFG_BTCE_BTC)) return 1;
    return 0;
  }

  public static boolean betterExchange(String e1, String e2) {
    return (scoreExchange(e1) >= scoreExchange(e2));
  }

  public void updatePrice(String newPriceExchange, float newPrice) {
    assert(newPriceExchange.equals(C.CFG_BTCE_BTC)
        || newPriceExchange.equals(C.CFG_BFLR_BTC)
        || newPriceExchange.equals(C.CFG_MGOX_BTC));
    Date now = new Date();
    if (now.getTime() - mPriceDate.getTime() > 1000*60*5) {
      // if we're older than five minutes, update no matter what
      mPrice = newPrice;
      mPriceDate = now;
      mPriceExchange = newPriceExchange;
    } else {
      // otherwise, only update if the exchange is "better"
      if (betterExchange(newPriceExchange, mPriceExchange)) {
        mPrice = newPrice;
        mPriceDate = now;
        mPriceExchange = newPriceExchange;
      }
    }
  }

}
