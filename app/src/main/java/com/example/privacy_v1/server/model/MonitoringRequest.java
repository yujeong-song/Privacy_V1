package com.example.privacy_v1.server.model;

public class MonitoringRequest {

    private String tablet_id;
    private String current_status;
    private int current_cycle;
    private  int total_cycles;
    private String version;
    private String started_at;
    private String vehicle_number;



    public MonitoringRequest(String tablet_id, String current_status, String vehicle_number, int current_cycle, int total_cycles, String version, String started_at) {
        this.tablet_id = tablet_id;
        this.current_status = current_status;
        this.vehicle_number = vehicle_number;
        this.current_cycle = current_cycle;
        this.total_cycles = total_cycles;
        this.version = version;
        this.started_at = started_at;
    }

    public MonitoringRequest(String tablet_id, String current_status, int current_cycle, int total_cycles, String version) {
        this.tablet_id = tablet_id;
        this.current_status = current_status;
        this.current_cycle = current_cycle;
        this.total_cycles = total_cycles;
        this.version = version;
    }
    public MonitoringRequest(String tablet_id, String current_status,String vehicle_number, int current_cycle, int total_cycles, String version) {
        this.tablet_id = tablet_id;
        this.current_status = current_status;
        this.vehicle_number = vehicle_number;
        this.current_cycle = current_cycle;
        this.total_cycles = total_cycles;
        this.version = version;
    }
}
