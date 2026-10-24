package com.medroute.routing;

public record Edge(long id, long from, long to, double lengthMeters, double speedMetersPerSecond) {
}