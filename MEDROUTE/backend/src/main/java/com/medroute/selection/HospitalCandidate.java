package com.medroute.selection;

import com.medroute.hospital.Hospital;

public record HospitalCandidate(Hospital hospital, double score, boolean eligible, String reason) {
}