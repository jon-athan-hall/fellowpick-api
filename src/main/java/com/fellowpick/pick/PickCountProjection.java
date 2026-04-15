package com.fellowpick.pick;

// Spring Data projection for aggregated pick counts grouped by card and type.
public interface PickCountProjection {
    String getCardId();
    PickType getPickType();
    long getCount();
}
