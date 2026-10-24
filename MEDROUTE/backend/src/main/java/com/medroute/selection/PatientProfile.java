package com.medroute.selection;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PatientProfile(@NotBlank String severity, @Min(50) @Max(100) int spo2,
                             @NotBlank String bloodGroup) {
}