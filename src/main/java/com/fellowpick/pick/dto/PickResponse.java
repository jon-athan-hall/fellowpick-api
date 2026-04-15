package com.fellowpick.pick.dto;

import com.fellowpick.pick.PickType;

// Response DTO representing a single pick made by a user.
public record PickResponse(
        String id,
        String preconId,
        String cardId,
        PickType pickType
) {
}
