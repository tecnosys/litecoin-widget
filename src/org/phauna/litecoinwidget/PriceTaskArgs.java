package org.phauna.litecoinwidget;

public class PriceTaskArgs {
  public int mWidgetId;
  public String mExchangeId;
  public String mCoin;
  public String mOldWorldCurrency;
  public int mTxtColor;
  public int mBgColor;
  public boolean mIsManualUpdate;
  public PriceTaskArgs(int widgetId, String exchangeId, String coin, String oldWorldCurrency, int txtColor, int bgColor, boolean isManualUpdate) {
    mWidgetId = widgetId;
    mExchangeId = exchangeId;
    mCoin = coin;
    mOldWorldCurrency = oldWorldCurrency;
    mTxtColor = txtColor;
    mBgColor = bgColor;
    mIsManualUpdate = isManualUpdate;
  }
}
