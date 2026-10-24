package com.medroute.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.medroute.hospital.HospitalService;
import org.junit.jupiter.api.Test;

class HospitalSelectionServiceTest {
    @Test
    void criticalLowOxygenPatientIsDirectedToRubyHall() {
        HospitalSelectionService selection = new HospitalSelectionService(new HospitalService());

        var ranking = selection.rank(new PatientProfile("CRITICAL", 88, "O+"));

        assertEquals("ruby", ranking.getFirst().hospital().id());
        assertFalse(ranking.stream().filter(candidate -> candidate.hospital().id().equals("sassoon"))
                .findFirst().orElseThrow().eligible());
    }
}