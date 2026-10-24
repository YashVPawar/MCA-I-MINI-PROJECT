package com.medroute.routing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RoutingService routingService;

    public RouteController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/compare")
    public Map<String, RouteResult> compare(@RequestParam @Min(1) @Max(10) long from,
                                             @RequestParam @Min(1) @Max(10) long to) {
        return Map.of("aStar", routingService.aStar(from, to), "dijkstra", routingService.dijkstra(from, to));
    }

    @GetMapping
    public RouteResult route(@RequestParam @Min(1) @Max(10) long from,
                             @RequestParam @Min(1) @Max(10) long to) {
        return routingService.aStar(from, to);
    }

    @PostMapping("/traffic")
    public Map<String, Object> traffic(@RequestBody TrafficUpdate update) {
        if (update.blocked() != null) {
            routingService.blockEdge(update.edgeId(), update.blocked());
        }
        if (update.multiplier() != null) {
            routingService.setTraffic(update.edgeId(), update.multiplier());
        }
        return Map.of("edgeId", update.edgeId(), "blocked", Boolean.TRUE.equals(update.blocked()),
                "multipliers", routingService.traffic());
    }

    public record TrafficUpdate(long edgeId, Boolean blocked, Double multiplier) {
    }
}