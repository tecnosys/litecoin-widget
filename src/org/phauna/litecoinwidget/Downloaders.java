package org.phauna.litecoinwidget;

import org.json.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Date;
import android.util.Log;

public class Downloaders {

  public static HashMap<String, ExchangeRateEntry> currencyExchangeCache = new HashMap();

  public static double getCachedExchangeRate(String from, String to) {
    String cPair = from + to;
    ExchangeRateEntry e = currencyExchangeCache.get(cPair);
    Date now = new Date();
    if (e == null) {
      Log.d(C.LOG, "no cached rate found for " + from + to);
    }
    if (e == null || now.getTime() - e.mDate.getTime() > 1000 * 60 * 60) { // 1 hour
      // refresh it
      double newRate = getCurrencyExchangeRate(from, to);
      Log.d(C.LOG, "got rate " + newRate + " for " + cPair);
      if (newRate == -1) {
        // don't save bad result. instead, return old stale result if possible
        if (e == null) return -1;
        return e.mRate;
      }
      Log.d(C.LOG, "caching rate " + newRate + " for " + cPair);
      ExchangeRateEntry newEntry = new ExchangeRateEntry(newRate, now);
      currencyExchangeCache.put(cPair, newEntry);
      return newRate;
    }
    return e.mRate;
  }


  public static class ExchangeRateEntry {
    public double mRate;
    public Date mDate;
    public ExchangeRateEntry(double rate, Date date) {
      mRate = rate;
      mDate = date;
    }
  }

  public static double getCurrencyExchangeRate(String from, String to) {
    from = from.toUpperCase();
    to = to.toUpperCase();
    try {
      URL url = new URL("http://rate-exchange.appspot.com/currency?from=" + from + "&to=" + to);
      String json = downloadReq(url);
      if (json == null) return 0;
      JSONObject j = new JSONObject(json);
      Log.d(C.LOG, "got json: " + j.toString());
      double price = j.getDouble("rate");
      return price;
    } catch (MalformedURLException e) {
      assert false;
    } catch (JSONException e) {
      Log.w(C.LOG, "json exception parsing rate: " + e.toString());
    }
    return -1;
  }

  public static double getMtgoxPrice() {
    try {
      URL url = new URL("http://data.mtgox.com/api/1/BTCUSD/ticker");
      String json = downloadReq(url);
      if (json == null) return 0;
      JSONObject j = new JSONObject(json);
      double price = j.getJSONObject("return").getJSONObject("last").getDouble("value");
      return price;
    } catch (MalformedURLException e) {
      assert false;
    } catch (JSONException e) {
    }
    return 0;
  }

  public static double getBitfloorPriceBTCUSD() {
    try {
      URL url = new URL("https://api.bitfloor.com/book/L1/1");
      String json = downloadReq(url);
      if (json == null) return 0;
      JSONObject j = new JSONObject(json);
      double bid = j.getJSONArray("bid").getDouble(0);
      double ask = j.getJSONArray("ask").getDouble(0);
      double price = (bid + ask) / 2;
      return price;
    } catch (MalformedURLException e) {
      assert false;
    } catch (JSONException e) {
    }
    return 0;
  }

  public static double getVircurexPrice(String coin) {
    try {
      URL url = new URL("https://vircurex.com/api/get_last_trade.json?base=" + coin + "&alt=BTC");
      String json = downloadReq(url);
      if (json == null) return 0;
      JSONObject j = new JSONObject(json);
      String priceString = j.getString("value");
      double price = Double.valueOf(priceString);
      return price;
    } catch (MalformedURLException e) {
      assert false;
    } catch (JSONException e) {
    }

    return 0;
  }

  public static double getBtcePrice(String coin, String in) {
    try {
      URL url = new URL("https://btc-e.com/api/2/" + coin + "_" + in + "/ticker");
      String json = downloadReq(url);
      if (json == null) return 0;
      JSONObject j = new JSONObject(json);
      JSONObject ticker = j.getJSONObject("ticker");
      double last = ticker.getDouble("last");
      return last;
    } catch (MalformedURLException e) {
      assert false;
    } catch (JSONException e) {
    }

    return 0;
  }

  public static String downloadReq(URL url) {
    InputStream in = null;
    OutputStream out = null;
    HttpURLConnection conn = null;

    try {
      conn = (HttpURLConnection) url.openConnection();

      conn.setReadTimeout(10000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);

      conn.connect();

      int response = conn.getResponseCode();
      if (response == 404) {
        Log.d(C.LOG, "404 for " + url);
        return null;
      } else if (response == 403) {
        Log.d(C.LOG, "403 for " + url);
        return "";
      } else if (response == 200) {
        Log.d(C.LOG, "200 for " + url);
        in = new BufferedInputStream(conn.getInputStream());
        String res = convertStreamToString(in);
        return res;
      } else {
        Log.d(C.LOG, "unrecognized http response: " + response + " for " + url);
        return null;
      }
    } catch (java.net.ConnectException e) {
      Log.e(C.LOG, e.toString() + ":" + e.getMessage());
      return null;
    } catch (java.io.IOException e) {
      Log.e(C.LOG, e.toString() + ":" + e.getMessage());
      return null;
    } finally {
        try {
          if (conn != null) { conn.disconnect(); };
          if (in != null) { in.close(); };
          if (out != null) { out.close(); };
        } catch (java.io.IOException e) {
          Log.e(C.LOG, "while closing: " + e.toString());
        }
      }
    }

  public static String convertStreamToString(InputStream is)
      throws java.io.IOException {
    BufferedReader reader =
      new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line = null;

    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }

    is.close();

    return sb.toString();
  }

}
