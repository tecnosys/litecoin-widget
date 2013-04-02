package org.phauna.litecoinwidget;

public class C {
  public static final String EXCHANGE_VIRCUREX = "vircurex";
  public static final String EXCHANGE_BTCE = "btce";
  public static final String EXCHANGE_BITFLOOR = "bitfloor";
  public static final String EXCHANGE_BTCE_BTC = "btce_btc";

  public static String exchangeName(String exchange) {
    if (exchange.equals(EXCHANGE_VIRCUREX)) {
      return "Vircurex";
    } else if (exchange.equals(EXCHANGE_BTCE)) {
      return "Btc-e";
    } else if (exchange.equals(EXCHANGE_BITFLOOR)) {
      return "Bitfloor";
    } else if (exchange.equals(EXCHANGE_BTCE_BTC)) {
      return "Btc-e";
    } else {
      return "???";
    }
  }

  public static final String pref_key_exchange = "pref_key_exchange";

  public static final String LOG = "org.phauna.litecoinwidget";
}
