package com.fellowpick.pick;

public interface PickCountProjection {
    String getCardId();
    PickType getPickType();
    long getCount();
}
