package org.phauna.litecoinwidget;

import org.json.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.util.HashMap;
import java.util.Date;
import android.util.Log;
import android.widget.Toast;

public class Downloaders {

  private UpdateWidgetService.GetPriceTask.Toaster mToaster;

  public Downloaders(UpdateWidgetService.GetPriceTask.Toaster toaster) {
    mToaster = toaster;
  }

  public void toastLong(String msg) {
    mToaster.toast(new Toasty(msg, Toast.LENGTH_LONG));
  }

  public void toastShort(String msg) {
    mToaster.toast(new Toasty(msg, Toast.LENGTH_SHORT));
  }

  public static HashMap<String, ExchangeRateEntry> currencyExchangeCache = new HashMap();

  public double getCachedExchangeRate(String from, String to) {
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

  public double getCurrencyExchangeRate(String from, String to) {
    from = from.toUpperCase();
    to = to.toUpperCase();
    try {
      URL url = new URL("http://rate-exchange.appspot.com/currency?from=" + from + "&to=" + to);
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        Log.d(C.LOG, "got json: " + j.toString());
        double price = j.getDouble("rate");
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return -1;
  }

  public double getMtgoxPrice() {
    try {
      URL url = new URL("http://data.mtgox.com/api/1/BTCUSD/ticker");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double price = j.getJSONObject("return").getJSONObject("last").getDouble("value");
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getBitfloorPriceBTCUSD() {
    try {
      URL url = new URL("https://api.bitfloor.com/book/L1/1");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double bid = j.getJSONArray("bid").getDouble(0);
        double ask = j.getJSONArray("ask").getDouble(0);
        double price = (bid + ask) / 2;
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getVircurexPrice(String coin) {
    try {
      URL url = new URL("https://vircurex.com/api/get_last_trade.json?base=" + coin + "&alt=BTC");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        String priceString = j.getString("value");
        double price = Double.valueOf(priceString);
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }

    return 0;
  }

  public double getBtcePrice(String coin, String in) {
    try {
      URL url = new URL("https://btc-e.com/api/2/" + coin + "_" + in + "/ticker");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        JSONObject ticker = j.getJSONObject("ticker");
        double last = ticker.getDouble("last");
        return last;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }

    return 0;
  }

  public String downloadReq(URL url) {
    InputStream in = null;
    OutputStream out = null;
    HttpURLConnection conn = null;

    try {

      if (url.getProtocol().toLowerCase().equals("https")) {
        trustAllHosts();
        HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
        https.setHostnameVerifier(DO_NOT_VERIFY);
        conn = https;
      } else {
        conn = (HttpURLConnection) url.openConnection();
      }

      conn.setReadTimeout(10000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);

      conn.connect();

      int response = conn.getResponseCode();
      if (response == 404) {
        toastLong("http 404 (not found) for " + url);
        return null;
      } else if (response == 403) {
        toastLong("http 403 (forbidden) for " + url);
        return "";
      } else if (response == 200) {
        //toastShort("updated!");
        in = new BufferedInputStream(conn.getInputStream());
        String res = convertStreamToString(in);
        return res;
      } else {
        toastLong("unrecognized http code: " + response + " for " + url);
        return null;
      }
    } catch (java.net.ConnectException e) {
      toastLong("exception: " + e.toString() + " for " + url);
      return null;
    } catch (java.io.IOException e) {
      toastLong("exception: " + e.toString() + " for " + url);
      return null;
    } finally {
        try {
          if (conn != null) { conn.disconnect(); };
          if (in != null) { in.close(); };
          if (out != null) { out.close(); };
        } catch (java.io.IOException e) {
          toastLong("exception while closing: " + e.toString() + " for " + url);
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

  // always verify the host - dont check for certificate
  final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  };

  /**
   * Trust every server - dont check for any certificate
   */
  private static void trustAllHosts() {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[] {};
      }

      public void checkClientTrusted(X509Certificate[] chain,
          String authType) throws CertificateException {
      }

      public void checkServerTrusted(X509Certificate[] chain,
          String authType) throws CertificateException {
      }
    } };

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection
          .setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
