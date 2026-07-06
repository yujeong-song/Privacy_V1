package com.example.privacy_v1.server.model;

public class StartedRequest {
    private String device_id;
    private String vehicle_number;
    private String version;
    private String manufacturer;
    private String avn_version;
    private String country;
    private String odometer;
    private String started_at;
    private String result;
    public StartedRequest(String device_id, String vehicle_number, String version, String manufacturer, String avn_version, String country, String odometer) {
        this.device_id = device_id;
        this.vehicle_number = vehicle_number;
        this.version = version;
        this.manufacturer = manufacturer;
        this.avn_version = avn_version;
        this.country = country;
        this.odometer = odometer;
    }
    public StartedRequest(String device_id, String vehicle_number) {
        this.device_id = device_id;
        this.vehicle_number = vehicle_number;
    }
    public StartedRequest(String device_id, String vehicle_number, String version, String started_at, String odometer, String result) {
        this.device_id = device_id;
        this.vehicle_number = vehicle_number;
        this.version = version;
        this.started_at = started_at;
        this.odometer = odometer;
        this.result = result;
    }
}
