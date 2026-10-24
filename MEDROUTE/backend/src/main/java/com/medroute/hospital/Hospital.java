package com.medroute.hospital;

public record Hospital(String id, String name, double latitude, double longitude, String status,
                       int icuBeds, int emergencyBeds, int ventilators, String bloodGroups,
                       double trafficMultiplier, int estimatedEtaMinutes) {
}