package com.fellowpick.pick.dto;

import com.fellowpick.pick.PickType;

public record PickResponse(
        String id,
        String preconId,
        String cardId,
        PickType pickType
) {
}
