package com.example.kidshield.models;

import com.google.gson.annotations.SerializedName;

public class ChildProfile {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("device")
    private String device;

    @SerializedName("device_name")
    private String deviceName;

    @SerializedName("dob")
    private String dob;

    @SerializedName("blood_group")
    private String bloodGroup;

    @SerializedName("online")
    private boolean online;

    public ChildProfile(int id, String name, String device, boolean online) {
        this.id = id;
        this.name = name;
        this.device = device;
        this.online = online;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDevice() { return device; }
    public String getDeviceName() { return deviceName; }
    public String getDob() { return dob; }
    public String getBloodGroup() { return bloodGroup; }
    public boolean isOnline() { return online; }
}
