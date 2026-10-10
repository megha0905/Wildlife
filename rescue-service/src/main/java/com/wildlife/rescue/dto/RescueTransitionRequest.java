package com.wildlife.rescue.dto;

import com.wildlife.rescue.entity.RescueStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RescueTransitionRequest(
        @NotNull RescueStatus toStatus,
        @Size(max = 2000) String note,
        @Valid TransferInfo transfer,
        @Valid ReleaseInfo release) {
}
