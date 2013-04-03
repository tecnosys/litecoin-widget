package org.phauna.litecoinwidget;

public class C {
  public static final String EXCHANGE_VIRCUREX = "vircurex";
  public static final String EXCHANGE_VIRCUREX_NMC = "vircurex_nmc";
  public static final String EXCHANGE_VIRCUREX_PPC = "vircurex_ppc";
  public static final String EXCHANGE_BTCE = "btce";
  public static final String EXCHANGE_BITFLOOR = "bitfloor";
  public static final String EXCHANGE_BTCE_BTC = "btce_btc";
  public static final String EXCHANGE_MTGOX = "mtgox";

  public static String exchangeName(String exchange) {
    if (   exchange.equals(EXCHANGE_VIRCUREX)
        || exchange.equals(EXCHANGE_VIRCUREX_NMC)
        || exchange.equals(EXCHANGE_VIRCUREX_PPC)) {
      return "V'rex";
    } else if (exchange.equals(EXCHANGE_BTCE) | exchange.equals(EXCHANGE_BTCE_BTC)) {
      return "Btc-e";
    } else if (exchange.equals(EXCHANGE_BITFLOOR)) {
      return "B'flr";
    } else if (exchange.equals(EXCHANGE_MTGOX)) {
      return "Mtgox";
    } else {
      return "???";
    }
  }

  public static final String pref_key_exchange = "pref_key_exchange";

  public static final String LOG = "org.phauna.litecoinwidget";
}
