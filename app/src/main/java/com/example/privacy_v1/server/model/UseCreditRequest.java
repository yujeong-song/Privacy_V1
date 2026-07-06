package com.example.privacy_v1.server.model;

import java.sql.Time;
import java.time.LocalDateTime;

public class UseCreditRequest {
    private String device_id;
    private String vehicle_number;
    private String use_result;
    private String odometer;
    private String version;
    private String manufacturer;
    private String avn_version;
    private String country;
    private String started_at;
    private String ended_at;
    public UseCreditRequest(String device_id, String vehicle_number, String use_result, String odometer, String version, String manufacturer, String avn_version, String country, String started_at, String ended_at) {
        this.device_id = device_id;
        this.vehicle_number = vehicle_number;
        this.use_result = use_result;
        this.odometer = odometer;
        this.version = version;
        this.manufacturer = manufacturer;
        this.avn_version = avn_version;
        this.country = country;
        this.started_at = started_at;
        this.ended_at = ended_at;
    }
//    public UseCreditRequest(String device_id, String vehicle_number, String use_result, String started_at, String ended_at) {
//        this.device_id = device_id;
//        this.vehicle_number = vehicle_number;
//        this.use_result = use_result;
//        this.started_at = started_at;
//        this.ended_at = ended_at;
//    }
    public UseCreditRequest(String device_id, String vehicle_number, String use_result, String started_at, String ended_at, String odometer, String version) {
        this.device_id = device_id;
        this.vehicle_number = vehicle_number;
        this.use_result = use_result;
        this.started_at = started_at;
        this.ended_at = ended_at;
        this.odometer = odometer;
        this.version = version;
    }
}
