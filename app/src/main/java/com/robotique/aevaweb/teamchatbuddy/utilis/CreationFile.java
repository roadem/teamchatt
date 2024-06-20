package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.Replica;
import com.robotique.aevaweb.teamchatbuddy.models.Session;
import com.robotique.aevaweb.teamchatbuddy.models.Setting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CreationFile {

    private static final String TAG = "TEAMCHATBUDDY_CreationFile";
    String configFilePseudo = "TeamChatBuddy.properties";

    private FileWriter fileWriter;
    private BufferedWriter bufferedWriter;

    public CreationFile() {}

    public File createFile(Setting settingClass, ArrayList<Replica> listRep, String path, String pathLog, TeamChatBuddyApplication teamChatBuddyApplication) {
        Log.w(TAG," --- createFile Log --- " + settingClass);
        File fileG = new File(path, "TeamChatBuddy");
        if (!fileG.exists()) {
            fileG.mkdir();
        }
        JsonObject setting = new JsonObject();
        setting.addProperty("duration", settingClass.getDuration());
        setting.addProperty("attempt", settingClass.getAttempt());
        setting.addProperty("Chatbot", settingClass.getChatbot());
        setting.addProperty("Chatbot_url",teamChatBuddyApplication.getParamFromFile("ChatGPT_url",configFilePseudo));
        setting.addProperty("Chatbot_api",teamChatBuddyApplication.getParamFromFile("ChatGPT_ApiEndpoint",configFilePseudo));
        setting.addProperty("Chatbot_token",teamChatBuddyApplication.getParamFromFile("openAI_API_Key",configFilePseudo));
        setting.addProperty("langue", settingClass.getLangue());
        setting.addProperty("speak speed", settingClass.getVitesse());
        setting.addProperty("speak volume", settingClass.getVolume());
        setting.addProperty("Visibility of lyrics", settingClass.getSwitchVisibility());
        String date = new SimpleDateFormat("dd_MM_YYYY-HH:mm:ss").format(new Date());
        File fileJson = new File(fileG.getPath(), "<"+settingClass.getChatbot()+">_"+date + ".json");
        Gson gson = new Gson();
        String json = gson.toJson(setting);
        try {
            fileJson.createNewFile();
            fileWriter = new FileWriter(fileJson.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(json);
            bufferedWriter.close();
            teamChatBuddyApplication.listSessionClear();
            listRep.clear();
            teamChatBuddyApplication.setFileCreate(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileJson;

    }

    public void updateFile(File fileupdate, Setting settingClass, TeamChatBuddyApplication teamChatBuddyApplication) {
        String json ="{"+System.getProperty("line.separator")+"\"setting\":";
        JsonObject setting = new JsonObject();
        setting.addProperty("duration", settingClass.getDuration());
        setting.addProperty("attempt", settingClass.getAttempt());
        setting.addProperty("Chatbot", settingClass.getChatbot());
        setting.addProperty("Chatbot_url",teamChatBuddyApplication.getParamFromFile("ChatGPT_url",configFilePseudo));
        setting.addProperty("Chatbot_api",teamChatBuddyApplication.getParamFromFile("ChatGPT_ApiEndpoint",configFilePseudo));
        setting.addProperty("Chatbot_token",teamChatBuddyApplication.getParamFromFile("openAI_API_Key",configFilePseudo));
        setting.addProperty("langue", settingClass.getLangue());
        setting.addProperty("speak speed", settingClass.getVitesse());
        setting.addProperty("speak volume", settingClass.getVolume());
        setting.addProperty("Visibility of lyrics", settingClass.getSwitchVisibility());
        json += setting.toString()+"," +System.getProperty("line.separator")+"\"history\": [ "+System.getProperty("line.separator")+"{";

        int x = 1;
        ArrayList<Session> ss = teamChatBuddyApplication.getListSession();
        for (int i = 0; i < ss.size(); i++) {
            //JsonArray interview = new JsonArray();
            ArrayList<Replica> s = ss.get(i).getSession();
            String jsonChild="\"interview"+(i+1)+"\": [ "+System.getProperty("line.separator");
            for (int j = 0; j < s.size(); j++) {
                Replica r = s.get(j);
                if (r.getType().equals("question") ||r.getType().equals("reponse")) {
                    JsonObject question = new JsonObject();
                    question.addProperty("type", r.getType());
                    question.addProperty("time", r.getTime());
                    question.addProperty("contenu", r.getValue());

                    if(j == 0) jsonChild +=question.toString()+","+System.getProperty("line.separator");
                    else jsonChild +=question.toString()+System.getProperty("line.separator");

                }
            }
            json += jsonChild+System.getProperty("line.separator")+"]";
            if (ss.size()>1 && i != (ss.size()-1)) json+=",";
            x++;
        }
        json+= "}]}";
        try {
            fileWriter = new FileWriter(fileupdate.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(json);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

