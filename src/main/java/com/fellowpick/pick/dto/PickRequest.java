package com.fellowpick.pick.dto;

import com.fellowpick.pick.PickType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Inbound request DTO for submitting a CUT or ADD vote on a card.
public record PickRequest(
        @NotBlank(message = "Precon ID is required")
        @Size(max = 100, message = "Precon ID must be at most 100 characters")
        String preconId,

        @NotBlank(message = "Card ID is required")
        @Size(max = 20, message = "Card ID must be at most 20 characters")
        String cardId,

        @NotNull(message = "Pick type is required")
        PickType pickType
) {
}
