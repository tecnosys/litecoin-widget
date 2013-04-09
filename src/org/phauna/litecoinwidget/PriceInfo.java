package org.phauna.litecoinwidget;

public class PriceInfo {

  private String mExchangeConfig;
  private int mWidgetId;
  private double mPriceBTC;
  private String mOwc;
  private double mPriceOWC;
  private boolean mEstimatedPriceOWC;
  private int mColor;
  private String mTransparencyLevel;

  public PriceInfo(String exchangeConfig, double priceBTC, String owc, double priceOWC, boolean estimatedPriceOWC, int widgetId, int color, String transparencyLevel) {
    mExchangeConfig = exchangeConfig;
    mPriceBTC = priceBTC;
    mOwc = owc;
    mPriceOWC = priceOWC;
    mEstimatedPriceOWC = estimatedPriceOWC;
    mWidgetId = widgetId;
    mColor = color;
    mTransparencyLevel = transparencyLevel;
  }

  public String getExchangeConfig() {
    return mExchangeConfig;
  }

  public String getOWC() {
    return mOwc;
  }

  public double getPriceBTC() {
    return mPriceBTC;
  }

  public double getPriceOWC() {
    return mPriceOWC;
  }

  public int getWidgetId() {
    return mWidgetId;
  }

  public boolean isEstimatedPriceOWC() {
    return mEstimatedPriceOWC;
  }

  public int getColor() {
    return mColor;
  }

  public String getTransparencyLevel() {
    return mTransparencyLevel;
  }

}
