package com.medroute.emergency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.medroute.hospital.HospitalService;
import com.medroute.selection.HospitalSelectionService;
import com.medroute.selection.PatientProfile;
import org.junit.jupiter.api.Test;

class EmergencyServiceTest {
    @Test
    void emergencyAdvancesThroughLifecycleAndCannotAdvanceAfterClosure() {
        EmergencyService service = new EmergencyService(
                new HospitalSelectionService(new HospitalService()));
        EmergencyRequest request = new EmergencyRequest("Test call",
                new PatientProfile("CRITICAL", 88, "O+"), 18.5204, 73.8567);

        EmergencyCase created = service.create(request);

        assertEquals("ASSIGNED", created.status());
        assertEquals("EN_ROUTE", service.advance(created.id()).status());
        assertEquals("ARRIVED", service.advance(created.id()).status());
        assertEquals("CLOSED", service.advance(created.id()).status());
        assertThrows(IllegalStateException.class, () -> service.advance(created.id()));
    }
}