package com.medroute.emergency;

import com.medroute.selection.HospitalCandidate;
import com.medroute.selection.HospitalSelectionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class EmergencyService {
    private final AtomicLong sequence = new AtomicLong(2049);
    private final Map<String, EmergencyCase> cases = new ConcurrentHashMap<>();
    private final HospitalSelectionService selectionService;

    public EmergencyService(HospitalSelectionService selectionService) {
        this.selectionService = selectionService;
    }

    public List<EmergencyCase> all() {
        return cases.values().stream().sorted((left, right) -> right.createdAt().compareTo(left.createdAt())).toList();
    }

    public List<HospitalCandidate> rank(EmergencyRequest request) {
        return selectionService.rank(request.patient());
    }

    public EmergencyCase create(EmergencyRequest request) {
        HospitalCandidate selected = selectionService.rank(request.patient()).stream()
                .filter(candidate -> candidate.eligible()).findFirst().orElseThrow();
        String id = "MR-" + sequence.getAndIncrement();
        EmergencyCase emergency = new EmergencyCase(id, request.details(), request.patient(), "ASSIGNED",
                selected.hospital().id(), selected.hospital().name(), selected.hospital().estimatedEtaMinutes(),
                request.latitude() == null ? 18.5204 : request.latitude(),
                request.longitude() == null ? 73.8567 : request.longitude(), Instant.now());
        cases.put(id, emergency);
        return emergency;
    }

    public EmergencyCase advance(String id) {
        return cases.compute(id, (key, current) -> {
            if (current == null) throw new IllegalArgumentException("Emergency not found: " + id);
            String next = switch (current.status()) {
                case "ASSIGNED" -> "EN_ROUTE";
                case "EN_ROUTE" -> "ARRIVED";
                case "ARRIVED" -> "CLOSED";
                case "CLOSED" -> throw new IllegalStateException("Emergency is already closed");
                default -> throw new IllegalStateException("Invalid emergency state: " + current.status());
            };
            return new EmergencyCase(current.id(), current.details(), current.patient(), next,
                    current.hospitalId(), current.hospitalName(), current.etaMinutes(),
                    current.latitude(), current.longitude(), current.createdAt());
        });
    }
}