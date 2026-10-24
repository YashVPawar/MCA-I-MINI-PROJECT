package com.medroute.hospital;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HospitalService {
    private final List<Hospital> hospitals = List.of(
            new Hospital("sassoon", "Sassoon General Hospital", 18.5196, 73.8712, "ACTIVE", 0, 18, 3, "O+,O-,A+,B+", 1.1, 6),
            new Hospital("ruby", "Ruby Hall Clinic", 18.5332, 73.8767, "ACTIVE", 7, 22, 5, "O+,O-,A+,A-,B+,B-,AB+", 1.5, 9),
            new Hospital("jehangir", "Jehangir Hospital", 18.5293, 73.8761, "ACTIVE", 0, 12, 2, "O+,A+,B+", 2.5, 8),
            new Hospital("poona", "Poona Hospital", 18.5146, 73.8570, "ACTIVE", 3, 14, 2, "O+,A+,B+,AB+", 1.2, 7),
            new Hospital("deenanath", "Deenanath Mangeshkar Hospital", 18.4635, 73.8075, "ACTIVE", 8, 28, 6, "O+,O-,A+,A-,B+,AB+", 1.2, 18),
            new Hospital("kem", "KEM Hospital", 18.5204, 73.8660, "ACTIVE", 2, 10, 1, "O+,A+,B+", 1.3, 5),
            new Hospital("sahyadri", "Sahyadri Hospital", 18.5074, 73.8338, "ACTIVE", 5, 19, 4, "O+,O-,A+,B+,AB+", 1.1, 14),
            new Hospital("aditya-birla", "Aditya Birla Memorial Hospital", 18.6270, 73.7997, "ACTIVE", 6, 26, 5, "O+,A+,B+,AB+", 1.0, 27));

    public List<Hospital> all() {
        return hospitals;
    }
}