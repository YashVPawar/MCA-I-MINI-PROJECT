package com.medroute.routing;

import java.util.List;
import java.util.Map;

public record RoadGraph(Map<Long, Coordinate> nodes, Map<Long, List<Edge>> adjacency) {
    public record Coordinate(double latitude, double longitude) {
    }
}