package org.phauna.litecoinwidget;

public class PriceInfo {

  private String mExchangeConfig;
  private int mWidgetId;
  private double mPriceBTC; // only for EXCHANGE_VIRCUREX, EXCHANGE_BTCE
  private double mPriceUSD; // EXCHANGE_BTCE, EXCHANGE_BITFLOOR, EXCHANGE_BTCE_BTC

  public PriceInfo(String exchangeConfig, double priceBTC, double priceUSD, int widgetId) {
    mExchangeConfig = exchangeConfig;
    mPriceBTC = priceBTC;
    mPriceUSD = priceUSD;
    mWidgetId = widgetId;
  }

  public String getExchangeConfig() {
    return mExchangeConfig;
  }

  public double getPriceBTC() {
    return mPriceBTC;
  }

  public double getPriceUSD() {
    return mPriceUSD;
  }

  public int getWidgetId() {
    return mWidgetId;
  }

}
