package com.medroute.emergency;

import com.medroute.selection.PatientProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EmergencyRequest(String details, @NotNull @Valid PatientProfile patient,
                               Double latitude, Double longitude) {
}