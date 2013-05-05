package org.phauna.litecoinwidget;

public class PriceInfo {

  private String mExchangeConfig;
  private String mCoin;
  private int mWidgetId;
  private double mPriceBTC;
  private String mOwc;
  private double mPriceOWC;
  private boolean mEstimatedPriceOWC;
  private int mTxtColor;
  private int mBgColor;

  public PriceInfo(String exchangeConfig, String coin, double priceBTC, String owc, double priceOWC, boolean estimatedPriceOWC, int widgetId, int txtColor, int bgColor) {
    mCoin = coin;
    mExchangeConfig = exchangeConfig;
    mPriceBTC = priceBTC;
    mOwc = owc;
    mPriceOWC = priceOWC;
    mEstimatedPriceOWC = estimatedPriceOWC;
    mWidgetId = widgetId;
    mTxtColor = txtColor;
    mBgColor = bgColor;
  }

  public String getExchangeConfig() {
    return mExchangeConfig;
  }

  public String getCoin() {
    return mCoin;
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

  public int getTxtColor() {
    return mTxtColor;
  }

  public int getBgColor() {
    return mBgColor;
  }

}
