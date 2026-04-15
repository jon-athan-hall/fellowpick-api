package com.fellowpick.pick.dto;

import com.fellowpick.pick.PickType;

// Response DTO carrying the aggregated vote count for a single card and pick type.
public record PickCountResponse(
        String cardId,
        PickType pickType,
        long count
) {
}
