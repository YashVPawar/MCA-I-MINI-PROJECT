package com.medroute.emergency;

import com.medroute.selection.HospitalCandidate;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergencies")
public class EmergencyController {
    private final EmergencyService emergencyService;

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    @GetMapping
    public List<EmergencyCase> all() {
        return emergencyService.all();
    }

    @PostMapping("/rank-hospitals")
    public List<HospitalCandidate> rank(@RequestBody @Valid EmergencyRequest request) {
        return emergencyService.rank(request);
    }

    @PostMapping
    public EmergencyCase create(@RequestBody @Valid EmergencyRequest request) {
        return emergencyService.create(request);
    }

    @PostMapping("/{id}/advance")
    public EmergencyCase advance(@PathVariable String id) {
        return emergencyService.advance(id);
    }
}