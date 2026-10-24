package com.medroute.routing;

import java.util.List;

public record RouteResult(List<Long> nodes, double travelTimeSeconds, int nodesVisited, String algorithm) {
}