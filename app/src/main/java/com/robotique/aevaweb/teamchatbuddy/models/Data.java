package com.robotique.aevaweb.teamchatbuddy.models;

import java.util.Map;

public class Data {
    private Map<String,Setting> setting;
    private Map<String,History> history;

    public Data() {
    }

    public Data(Map<String,Setting> setting, Map<String,History> history) {
        this.setting = setting;
        this.history = history;
    }

    public Map<String,Setting> getSetting() {
        return setting;
    }

    public void setSetting(Map<String,Setting> setting) {
        this.setting = setting;
    }

    public Map<String,History> getHistory() {
        return history;
    }

    public void setHistory(Map<String,History> history) {
        this.history = history;
    }

    @Override
    public String toString() {
        return "Data{" +
                "setting=" + setting +
                ", history=" + history +
                '}';
    }
}