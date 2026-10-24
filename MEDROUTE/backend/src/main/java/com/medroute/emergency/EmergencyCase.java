package com.medroute.emergency;

import com.medroute.selection.PatientProfile;
import java.time.Instant;

public record EmergencyCase(String id, String details, PatientProfile patient, String status,
                            String hospitalId, String hospitalName, int etaMinutes,
                            double latitude, double longitude, Instant createdAt) {
}