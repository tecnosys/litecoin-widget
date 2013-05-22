package org.phauna.litecoinwidget;

public class PriceInfo {

  private PriceTaskArgs mPriceTaskArgs;

  private double mPriceBTC;
  private double mPriceOWC;
  private boolean mEstimatedPriceOWC;

  public PriceInfo(PriceTaskArgs args, double priceBTC, double priceOWC, boolean estimatedPriceOWC) {
    mPriceTaskArgs = args;
    mPriceBTC = priceBTC;
    mPriceOWC = priceOWC;
    mEstimatedPriceOWC = estimatedPriceOWC;
  }

  public PriceTaskArgs getPriceTaskArgs() {
    return mPriceTaskArgs;
  }

  public double getPriceBTC() {
    return mPriceBTC;
  }

  public double getPriceOWC() {
    return mPriceOWC;
  }

  public boolean isEstimatedPriceOWC() {
    return mEstimatedPriceOWC;
  }

}
