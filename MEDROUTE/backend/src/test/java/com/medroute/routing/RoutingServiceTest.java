package com.medroute.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoutingServiceTest {
    @Test
    void aStarMatchesDijkstraOnDemoGraph() {
        RoutingService routing = new RoutingService();

        RouteResult aStar = routing.aStar(1, 4);
        RouteResult dijkstra = routing.dijkstra(1, 4);

        assertEquals(dijkstra.travelTimeSeconds(), aStar.travelTimeSeconds(), 1e-9);
        assertEquals(1L, aStar.nodes().getFirst());
        assertEquals(4L, aStar.nodes().getLast());
    }

    @Test
    void blockingRoadForcesAnAlternateRoute() {
        RoutingService routing = new RoutingService();
        RouteResult original = routing.aStar(1, 4);

        routing.blockEdge(1, true);
        RouteResult detour = routing.aStar(1, 4);

        assertNotEquals(original.nodes(), detour.nodes());
        assertTrue(detour.travelTimeSeconds() > 0);
    }
}