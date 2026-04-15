package com.fellowpick.pick.dto;

import com.fellowpick.pick.PickType;

public record PickCountResponse(
        String cardId,
        PickType pickType,
        long count
) {
}
