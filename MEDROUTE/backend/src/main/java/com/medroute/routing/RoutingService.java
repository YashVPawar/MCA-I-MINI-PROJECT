package com.medroute.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {
    private static final double MAX_SPEED_METERS_PER_SECOND = 20.0;
    private final RoadGraph graph = createDemoGraph();
    private final Map<Long, Double> trafficMultipliers = new ConcurrentHashMap<>();
    private final Set<Long> blockedEdges = ConcurrentHashMap.newKeySet();

    public RouteResult aStar(long from, long to) {
        return search(from, to, true);
    }

    public RouteResult dijkstra(long from, long to) {
        return search(from, to, false);
    }

    public Map<Long, Double> traffic() {
        return Map.copyOf(trafficMultipliers);
    }

    public void setTraffic(long edgeId, double multiplier) {
        if (!containsEdge(edgeId)) {
            throw new IllegalArgumentException("Unknown road edge: " + edgeId);
        }
        if (multiplier < 1.0 || multiplier > 5.0) {
            throw new IllegalArgumentException("Traffic multiplier must be between 1.0 and 5.0");
        }
        trafficMultipliers.put(edgeId, multiplier);
        blockedEdges.remove(edgeId);
    }

    public void blockEdge(long edgeId, boolean blocked) {
        if (!containsEdge(edgeId)) {
            throw new IllegalArgumentException("Unknown road edge: " + edgeId);
        }
        if (blocked) {
            blockedEdges.add(edgeId);
        } else {
            blockedEdges.remove(edgeId);
        }
    }

    public List<RoadGraph.Coordinate> coordinates(List<Long> path) {
        return path.stream().map(graph.nodes()::get).toList();
    }

    private RouteResult search(long from, long to, boolean useHeuristic) {
        if (!graph.nodes().containsKey(from) || !graph.nodes().containsKey(to)) {
            throw new IllegalArgumentException("Route endpoints must be graph node ids 1 through 10");
        }
        if (from == to) {
            return new RouteResult(List.of(from), 0, 1, useHeuristic ? "A*" : "Dijkstra");
        }
        Map<Long, Double> costs = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<NodeCost> queue = new PriorityQueue<>((left, right) -> Double.compare(left.priority(), right.priority()));
        costs.put(from, 0.0);
        queue.add(new NodeCost(from, heuristic(from, to, useHeuristic)));
        int visited = 0;

        while (!queue.isEmpty()) {
            NodeCost current = queue.poll();
            double currentCost = costs.getOrDefault(current.node(), Double.POSITIVE_INFINITY);
            if (current.priority() > currentCost + heuristic(current.node(), to, useHeuristic) + 1e-9) {
                continue;
            }
            if (current.node() == to) {
                return new RouteResult(reconstruct(previous, to), currentCost, visited, useHeuristic ? "A*" : "Dijkstra");
            }
            visited++;
            for (Edge edge : graph.adjacency().getOrDefault(current.node(), List.of())) {
                if (blockedEdges.contains(edge.id())) {
                    continue;
                }
                double multiplier = trafficMultipliers.getOrDefault(edge.id(), 1.0);
                double nextCost = currentCost + edge.lengthMeters() / edge.speedMetersPerSecond() * multiplier;
                if (nextCost < costs.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
                    costs.put(edge.to(), nextCost);
                    previous.put(edge.to(), current.node());
                    queue.add(new NodeCost(edge.to(), nextCost + heuristic(edge.to(), to, useHeuristic)));
                }
            }
        }
        throw new IllegalStateException("No traversable route exists between the selected nodes");
    }

    private double heuristic(long from, long to, boolean enabled) {
        if (!enabled) return 0;
        RoadGraph.Coordinate start = graph.nodes().get(from);
        RoadGraph.Coordinate goal = graph.nodes().get(to);
        return haversine(start.latitude(), start.longitude(), goal.latitude(), goal.longitude()) / MAX_SPEED_METERS_PER_SECOND;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double latDelta = Math.toRadians(lat2 - lat1);
        double lonDelta = Math.toRadians(lon2 - lon1);
        double value = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }

    private List<Long> reconstruct(Map<Long, Long> previous, long destination) {
        List<Long> path = new ArrayList<>();
        long current = destination;
        path.add(current);
        while (previous.containsKey(current)) {
            current = previous.get(current);
            path.add(current);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private boolean containsEdge(long edgeId) {
        for (List<Edge> edges : graph.adjacency().values()) {
            for (Edge edge : edges) {
                if (edge.id() == edgeId) return true;
            }
        }
        return false;
    }

    private RoadGraph createDemoGraph() {
        Map<Long, RoadGraph.Coordinate> nodes = Map.ofEntries(
                Map.entry(1L, new RoadGraph.Coordinate(18.5204, 73.8567)),
                Map.entry(2L, new RoadGraph.Coordinate(18.5248, 73.8620)),
                Map.entry(3L, new RoadGraph.Coordinate(18.5293, 73.8680)),
                Map.entry(4L, new RoadGraph.Coordinate(18.5332, 73.8767)),
                Map.entry(5L, new RoadGraph.Coordinate(18.5196, 73.8712)),
                Map.entry(6L, new RoadGraph.Coordinate(18.5130, 73.8600)),
                Map.entry(7L, new RoadGraph.Coordinate(18.5060, 73.8500)),
                Map.entry(8L, new RoadGraph.Coordinate(18.5293, 73.8761)),
                Map.entry(9L, new RoadGraph.Coordinate(18.4635, 73.8075)),
                Map.entry(10L, new RoadGraph.Coordinate(18.5145, 73.8430)));
        Map<Long, List<Edge>> adjacency = new HashMap<>();
        AtomicLong edgeId = new AtomicLong(1);
        long[][] links = {{1, 2}, {2, 3}, {3, 4}, {2, 5}, {5, 4}, {1, 5}, {1, 6}, {6, 7}, {7, 10}, {10, 2}, {3, 8}, {8, 4}, {7, 9}, {9, 6}, {5, 8}};
        for (long[] link : links) {
            long start = link[0];
            long end = link[1];
            RoadGraph.Coordinate a = nodes.get(start);
            RoadGraph.Coordinate b = nodes.get(end);
            double distance = haversine(a.latitude(), a.longitude(), b.latitude(), b.longitude()) * 1.12;
            double speed = 9.0 + edgeId.get() % 4;
            long id = edgeId.getAndIncrement();
            adjacency.computeIfAbsent(start, ignored -> new ArrayList<>()).add(new Edge(id, start, end, distance, speed));
            adjacency.computeIfAbsent(end, ignored -> new ArrayList<>()).add(new Edge(id, end, start, distance, speed));
        }
        return new RoadGraph(nodes, adjacency);
    }

    private record NodeCost(long node, double priority) {
    }
}