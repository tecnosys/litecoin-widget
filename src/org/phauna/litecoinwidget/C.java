package org.phauna.litecoinwidget;

import java.util.Currency;

public class C {
  public static final String CFG_VREX_LTC = "vircurex_ltc";
  public static final String CFG_VREX_NMC = "vircurex_nmc";
  public static final String CFG_VREX_PPC = "vircurex_ppc";
  public static final String CFG_BFLR_BTC = "bitfloor_btc";
  public static final String CFG_MGOX_BTC = "mtgox_btc";
  public static final String CFG_BTCE_LTC = "btce_ltc";
  public static final String CFG_BTCE_BTC = "btce_btc";
  public static final String CFG_BTCE_NMC = "btce_nmc";
  public static final String CFG_BTCE_PPC = "btce_ppc";
  public static final String CFG_INVALID  = "invalid";

  public static String exchangeName(String exchange) {
    if (   exchange.equals(CFG_VREX_LTC)
        || exchange.equals(CFG_VREX_NMC)
        || exchange.equals(CFG_VREX_PPC)) {
      return "V'rex";
    } else if (exchange.equals(CFG_BTCE_LTC)
            || exchange.equals(CFG_BTCE_BTC)
            || exchange.equals(CFG_BTCE_NMC)
            || exchange.equals(CFG_BTCE_PPC)) {
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
  public static final String pref_key_transparbar   = "pref_key_transparbar";
  public static final String pref_key_rate_price    = "pref_key_rate_price";
  public static final String pref_key_rate_date     = "pref_key_rate_date";
  public static final String pref_key_rate_exchange = "pref_key_rate_exchange";
  public static final String pref_key_owc           = "pref_key_owc";
  public static final String pref_key_done          = "pref_key_done";
  public static final String pref_key_color         = "pref_key_color";

  public static final String LOG = "org.phauna.litecoinwidget";

  public static final String USD = "USD";
  public static final String EUR = "EUR";
  public static final String GBP = "GBP";
  public static final String JPY = "JPY";

  public static String currencySymbol(String c) {
    return Currency.getInstance(c).getSymbol();
  }

  public static final int COLOR_LTG = 0xFFC9C9C9;
  public static final int COLOR_DKB = 0xFF150038;
  public static final int COLOR_GRN = 0xFF00820B;

  public static int getColor(String colorKey) {
    if (colorKey.equals("light_grey")) {
      return COLOR_LTG;
    } else if (colorKey.equals("dark_blue")) {
      return COLOR_DKB;
    } else if (colorKey.equals("green")) {
      return COLOR_GRN;
    } else {
      return COLOR_LTG;
    }
  }

}
