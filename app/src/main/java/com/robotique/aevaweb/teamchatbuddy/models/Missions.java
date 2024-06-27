package com.robotique.aevaweb.teamchatbuddy.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Missions {
    private String name;
    private String trigger;
    private boolean stackable;
    private String priority;
    private String waitForEndOfPriority;
    private String task;

    public Missions() {
    }

    public Missions(JSONObject jsonObject) {
        try {
            this.name = jsonObject.getString("name");
            this.trigger = jsonObject.getString("trigger");
            this.stackable = jsonObject.getBoolean("stackable");
            this.priority = jsonObject.getString("priority");
            this.waitForEndOfPriority = jsonObject.getString("waitForEndOfPriority");
            this.task = jsonObject.getString("task");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static List<Missions> loadMissionsFromJsonFile() {
        List<Missions> missions = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream("storage/emulated/0/Configs/Users/Default/Companion/Domains/TeamChatBuddyTasks.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            fis.close();
            isr.close();
            bufferedReader.close();

            JSONObject jsonObject = new JSONObject(stringBuilder.toString());
            JSONArray missionsArray = jsonObject.getJSONArray("missions");
            for (int i = 0; i < missionsArray.length(); i++) {
                JSONObject missionJson = missionsArray.getJSONObject(i);
                Missions mission = new Missions(missionJson);
                missions.add(mission);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return missions;
    }

    public static String getTaskForTrigger( String trigger) {
        List<Missions> missions = loadMissionsFromJsonFile();
        for (Missions mission : missions) {
            if (mission.getTrigger().equals(trigger)) {
                String extracted = mission.getTask().substring("runActivity(".length(), mission.getTask().length() - 1);
                return extracted;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public boolean isStackable() {
        return stackable;
    }

    public void setStackable(boolean stackable) {
        this.stackable = stackable;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getWaitForEndOfPriority() {
        return waitForEndOfPriority;
    }

    public void setWaitForEndOfPriority(String waitForEndOfPriority) {
        this.waitForEndOfPriority = waitForEndOfPriority;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }
}
