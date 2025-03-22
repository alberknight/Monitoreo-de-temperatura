package com.example.aplicacion7;

public class Message {
    private String sensorName;
    private String temperature;

    public Message(String sensorName, String temperature) {
        this.sensorName = sensorName;
        this.temperature = temperature;
    }

    public String getSensorName() {
        return sensorName;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }
}
