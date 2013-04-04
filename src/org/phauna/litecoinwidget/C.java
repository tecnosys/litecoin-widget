package org.phauna.litecoinwidget;

import java.util.Currency;

public class C {
  public static final String CFG_VREX_LTC = "vircurex";
  public static final String CFG_VREX_NMC = "vircurex_nmc";
  public static final String CFG_VREX_PPC = "vircurex_ppc";
  public static final String CFG_BTCE_LTC = "btce";
  public static final String CFG_BTCE_BTC = "btce_btc";
  public static final String CFG_BFLR_BTC = "bitfloor";
  public static final String CFG_MGOX_BTC = "mtgox";

  public static String exchangeName(String exchange) {
    if (   exchange.equals(CFG_VREX_LTC)
        || exchange.equals(CFG_VREX_NMC)
        || exchange.equals(CFG_VREX_PPC)) {
      return "V'rex";
    } else if (exchange.equals(CFG_BTCE_LTC) || exchange.equals(CFG_BTCE_BTC)) {
      return "Btc-e";
    } else if (exchange.equals(CFG_BFLR_BTC)) {
      return "B'flr";
    } else if (exchange.equals(CFG_MGOX_BTC)) {
      return "Mtgox";
    } else {
      return "???";
    }
  }

  public static final String pref_key_exchange      = "pref_key_exchange";
  public static final String pref_key_rate_price    = "pref_key_rate_price";
  public static final String pref_key_rate_date     = "pref_key_rate_date";
  public static final String pref_key_rate_exchange = "pref_key_rate_exchange";
  public static final String pref_key_owc = "pref_key_owc";
  public static final String pref_key_done = "pref_key_done";

  public static final String LOG = "org.phauna.litecoinwidget";

  public static final String USD = "USD";
  public static final String EUR = "EUR";
  public static final String GBP = "GBP";
  public static final String JPY = "JPY";

  public static String currencySymbol(String c) {
    return Currency.getInstance(c).getSymbol();
  }
}
