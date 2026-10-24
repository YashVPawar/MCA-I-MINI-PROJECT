package com.medroute.selection;

import com.medroute.hospital.Hospital;
import com.medroute.hospital.HospitalService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HospitalSelectionService {
    private final HospitalService hospitalService;

    public HospitalSelectionService(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    public List<HospitalCandidate> rank(PatientProfile patient) {
        List<Hospital> eligible = hospitalService.all().stream().filter(hospital -> rejectionReason(hospital, patient) == null).toList();
        if (eligible.isEmpty()) {
            throw new IllegalStateException("No hospital meets the patient's simulated care requirements");
        }
        int maxEtaValue = 1;
        double maxTrafficValue = 1;
        for (Hospital hospital : eligible) {
            maxEtaValue = Math.max(maxEtaValue, hospital.estimatedEtaMinutes());
            maxTrafficValue = Math.max(maxTrafficValue, hospital.trafficMultiplier());
        }
        int maxEta = maxEtaValue;
        double maxTraffic = maxTrafficValue;
        return hospitalService.all().stream().map(hospital -> {
            String rejection = rejectionReason(hospital, patient);
            if (rejection != null) return new HospitalCandidate(hospital, Double.POSITIVE_INFINITY, false, rejection);
            double etaNorm = (double) hospital.estimatedEtaMinutes() / Math.max(maxEta, 1);
            double trafficNorm = hospital.trafficMultiplier() / Math.max(maxTraffic, 1);
            double resourceMatch = Math.min(1.0, (hospital.icuBeds() + hospital.ventilators() + 1.0) / 12.0);
            double score = 0.5 * etaNorm + 0.2 * trafficNorm + 0.3 * (1 - resourceMatch);
            return new HospitalCandidate(hospital, Math.round(score * 1000.0) / 1000.0, true, "Meets simulated resource requirements");
        }).sorted((left, right) -> {
            int eligibility = Boolean.compare(right.eligible(), left.eligible());
            return eligibility != 0 ? eligibility : Double.compare(left.score(), right.score());
        }).toList();
    }

    private String rejectionReason(Hospital hospital, PatientProfile patient) {
        if (!hospital.status().equals("ACTIVE")) return "Hospital is not accepting patients";
        if (hospital.emergencyBeds() < 1) return "No emergency beds available";
        if (patient.severity().equalsIgnoreCase("CRITICAL") && hospital.icuBeds() < 1) return "No ICU bed for critical case";
        if (patient.spo2() < 90 && hospital.ventilators() < 1) return "No ventilator available for low SpO2";
        if (!List.of(hospital.bloodGroups().split(",")).contains(patient.bloodGroup())) return "Required blood group unavailable";
        return null;
    }
}