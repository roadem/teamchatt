package com.robotique.aevaweb.teamchatbuddy.chatbotresponse;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.HttpResponse;
import com.robotique.aevaweb.teamchatbuddy.utilis.HttpClientUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.FileOutputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ResponseFromChatbot {
    TeamChatBuddyApplication teamChatBuddyApplication;
    String chatBotServerNoResponce_fr;
    String chatBotServerNoResponce_en;
    Activity activity;
    private String historicMessages = "messages";
    private String content= "content";
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";
    private String tag ="ResponceFromChatbotClass";
    private String result = "";
    private JSONArray existingHistoryArray;
    Commande commande;
    private String prompt_cmd;
    public static CountDownTimer responseTimeout;

    public ResponseFromChatbot(TeamChatBuddyApplication context,Activity activity) {
        this.teamChatBuddyApplication = context;
        this.activity=activity;
        chatBotServerNoResponce_fr= teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_fr","TeamChatBuddy.properties");
        chatBotServerNoResponce_en= teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");

    }

    /**
     * -------------------------------- ChatBot ChatGPT Buddy  ------------------------------------
     */
    public void getResponseFromChatGPT(String texte,int numberOfQuestion){

        teamChatBuddyApplication.notifyObservers("hideCameraQr");
        teamChatBuddyApplication.setAlreadyChatting(true);


        int Max_LIMIT = Integer.parseInt(teamChatBuddyApplication.getparam("Max_Tokens_req"));
        String RoleBuddy;
        existingHistoryArray = new JSONArray();

        // vérifie si la langue actuelle de l'application est l'anglais.
        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn) ){
            RoleBuddy = teamChatBuddyApplication.getparam("header");
        }
        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr) ) {
            RoleBuddy = teamChatBuddyApplication.getparam("entete");
        }
        else {
            RoleBuddy = teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete");
        }


        // initialise Retrofit en fournissant l'URL de base de l'API ChatGPT.


        Log.e("MRAA","question "+texte);

        JSONObject jsonParams = new JSONObject();



        try {


            // define the model
            jsonParams.put("model", teamChatBuddyApplication.getparam("model"));
            jsonParams.put("temperature",Double.parseDouble(teamChatBuddyApplication.getparam("Temperature_chatgpt")));
            jsonParams.put("max_tokens",Double.parseDouble(teamChatBuddyApplication.getparam("Max_Tokens_resp")));
            if (teamChatBuddyApplication.getparam("Mode_Stream").contains("yes")) jsonParams.put("stream", true);

            // get the historic messages :
            String jsonArrayString = teamChatBuddyApplication.getparam(historicMessages);
            existingHistoryArray = new JSONArray(jsonArrayString);


            if (existingHistoryArray.length() != 0){
                Log.e("FCH","!!!!!existinghistory 0");


                try {
                    String systemContent = null;
                    // Parcourir les éléments du tableau
                    for (int i = 0; i < existingHistoryArray.length(); i++) {
                        JSONObject messageObject = existingHistoryArray.getJSONObject(i);

                        // Vérifier si le rôle est "system"
                        if (messageObject.getString("role").equals("system")) {
                            // Récupérer le contenu correspondant

                            systemContent = messageObject.getString("content");
                            Log.e("FCH","systemContent list "+systemContent);
                            i=existingHistoryArray.length();
                        }
                    }
                    if (systemContent!=null && !systemContent.equalsIgnoreCase(RoleBuddy)){
                        // Recherche de l'objet "system" dans le JSONArray
                        for (int i = 0; i < existingHistoryArray.length(); i++) {
                            JSONObject roleObj = existingHistoryArray.getJSONObject(i);
                            if (roleObj.has("role") && roleObj.getString("role").equals("system")) {
                                // Mettre à jour la valeur de 'content' dans l'objet "system"
                                roleObj.put(content, RoleBuddy);
                                // Sortie de la boucle après la mise à jour
                                break;
                            }
                        }
                    }else if (systemContent == null){
                        JSONObject Role = new JSONObject();
                        Role.put("role", "system");
                        Role.put(content, RoleBuddy);

                        ArrayList<JSONObject> jsonObjectList = new ArrayList<>();
                        for (int i = 0; i < existingHistoryArray.length(); i++) {
                            jsonObjectList.add(existingHistoryArray.getJSONObject(i));
                        }
                        jsonObjectList.add(0, Role);
                        // Reconvertir en JSONArray
                        JSONArray newJsonArray = new JSONArray(jsonObjectList);
                        existingHistoryArray = newJsonArray;

                    }
                } catch (JSONException e) {
                    Log.e("FCH","exception "+e);
                    e.printStackTrace();
                }
                // define the role
            }
            else {
                Log.e("FCH","existinghistory 0");
                JSONObject Role = new JSONObject();
                Role.put("role", "system");
                Role.put(content, RoleBuddy);
                existingHistoryArray.put(Role);
            }

                JSONObject Question = new JSONObject();
                Question.put("role", "user");
                Question.put(content, texte);
                existingHistoryArray.put(Question);

            jsonParams.put(historicMessages, existingHistoryArray);

            //Mettre  le dernier fichier json envoyé à l’API
            String fileName = "ChatGPT-sent";
            File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");


            try {
                if (file1.exists() && file1.isFile()) {
                    file1.delete();
                }
                FileWriter fileWriter = new FileWriter(file1);
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String jsonString=gson.toJson(jsonParams);
                fileWriter.write(jsonString);
                fileWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            if(responseTimeout!=null){
                responseTimeout.cancel();
                responseTimeout = null;
            }

            if (teamChatBuddyApplication.getparam("Mode_Stream").contains("yes")){
                if(teamChatBuddyApplication.getChatGptStreamMode() != null){
                    if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                        teamChatBuddyApplication.getChatGptStreamMode().reset();
                    }
                }
                teamChatBuddyApplication.setChatGptStreamMode(new ChatGptStreamMode(activity,existingHistoryArray));
                teamChatBuddyApplication.getChatGptStreamMode().sendRequest(jsonParams);
            }
            else{
                if (
                        ( Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Response_Timeout_in_seconds","TeamChatBuddy.properties"))!=0 )
                        && ((teamChatBuddyApplication.getCurrentLanguage().equals("en")
                        && !teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").trim().isEmpty())
                        || (teamChatBuddyApplication.getCurrentLanguage().equals("fr")
                        && !teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_fr","TeamChatBuddy.properties").trim().isEmpty())
                        ||(!teamChatBuddyApplication.getCurrentLanguage().equals("en")
                        && !teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").trim().isEmpty())
                            )
                ) {
                    teamChatBuddyApplication.setAnswerHasExceededTimeOut(false);
                    responseTimeout = new CountDownTimer(Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Response_Timeout_in_seconds", "TeamChatBuddy.properties")) * 1000, 1000) {
                        @Override
                        public void onTick(long l) {
                            Log.d("responseTimeout", "on Tick");
                        }
                        @Override
                        public void onFinish() {
                            if (teamChatBuddyApplication.isAlreadyGetAnswer()) {
                            } else {
                                teamChatBuddyApplication.setAnswerHasExceededTimeOut(true);
                                teamChatBuddyApplication.setTimeoutExpired(true);
                                if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                    try {
                                        BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
                                    }
                                    catch (Exception e){
                                        Log.e("TAG","BuddySDK Exception  "+e);
                                    }
                                }
                                if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                    String[] message_Timeout_NotRespected_en = teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").split("/");
                                    int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                    teamChatBuddyApplication.speakTTS(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en], LabialExpression.SPEAK_NEUTRAL,"timeOutExpired");
                                }
                                else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                    String[] message_Timeout_NotRespected_fr = teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_fr","TeamChatBuddy.properties").split("/");
                                    int randomNumber_message_Timeout_NotRespected_fr = new Random().nextInt(message_Timeout_NotRespected_fr.length);
                                    teamChatBuddyApplication.speakTTS(message_Timeout_NotRespected_fr[randomNumber_message_Timeout_NotRespected_fr], LabialExpression.SPEAK_NEUTRAL,"timeOutExpired");
                                }
                                else {
                                    String[] message_Timeout_NotRespected_en = teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").split("/");
                                    int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en]).addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            teamChatBuddyApplication.speakTTS(translatedText, LabialExpression.SPEAK_NEUTRAL,"timeOutExpired");
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("responseTimeout","translatedText exception  "+e);
                                        }
                                    });
                                }
                            }
                        }
                    };
                    Log.i("responseTimeout", " start responseTimeout ");
                    responseTimeout.start();
                }

                Log.i("responseTimeout", " call ChatGPT API (StreamMode OFF) ");
                new Thread(() -> {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"));
                    headers.put("Content-Type", "application/json");

                    HttpResponse httpResponse = null;
                    try {
                        httpResponse = HttpClientUtils.sendPost(teamChatBuddyApplication.getparam("ChatGPT_url") + "/v1/chat/completions", jsonParams.toString(), headers, 50000);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    if (responseTimeout != null) {
                        responseTimeout.cancel();
                        responseTimeout = null;
                        Log.i("responseTimeout", "réponse ChatGPT --> cancel timeout " + responseTimeout);
                    }
                    Log.i("responseTimeout", " timeout cancelled ");
                    teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                    // Vérifie si la réponse est réussie
                    int responseCode = httpResponse.responseCode;
                    String response = httpResponse.body;
                    if (responseCode >= 200 && responseCode < 300) {

                        if (response.contains("Failed to find bot")) {

                            if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_en) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                            } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_fr) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                            } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_es) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                            } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_de) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                            } else {
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBotNoFound_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {

                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(tag, "translatedText exception  " + e);
                                    }
                                });

                            }

                        } else if (response.contains("NoResponseServer")) {
                            if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_en + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                            } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_fr + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                            } else {
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {

                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(tag, "translatedText exception  " + e);
                                    }
                                });

                            }

                        }
                        // Traitement de la réponse :
                        else {
                            try {
                                Log.i("responseTimeout", " start 5 Traitement de la réponse ");

                                JSONObject jsonObj = new JSONObject(response);
                                //Mettre   le fichier le plus récent reçu
                                String fileName1 = "ChatGPT-recv";
                                File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName1 + ".json");
                                try {
                                    if (file2.exists() && file2.isFile()) {
                                        file2.delete();
                                        Log.v("Json_API", "file deleted");
                                    }
                                    FileWriter fileWriter = new FileWriter(file2);
                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                    String jsonString = gson.toJson(jsonObj);
                                    fileWriter.write(jsonString);
                                    fileWriter.close();
                                    Log.v("Json_API", "new file added");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // récupérer la réponse
                                JSONArray choicesArray = jsonObj.getJSONArray("choices");
                                JSONObject choiceObject = choicesArray.getJSONObject(0);
                                JSONObject messageObject = choiceObject.getJSONObject("message");

                                result = messageObject.getString(content);

                                // Elimination d’une phrase si elle est incomplète :
                                int complete_answer_index = result.lastIndexOf(".");
                                if (complete_answer_index != -1) {
                                    result = result.substring(0, complete_answer_index);
                                    Log.e(tag, "the answer after elimination : " + result);
                                } else {
                                    Log.e(tag, "no period found in the answer : " + result);
                                }
                                Log.e(tag, "the answer with max nb parameter : " + result);

                                if (teamChatBuddyApplication.getParamFromFile("Response_filter", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Response_filter", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")) {
                                    result = teamChatBuddyApplication.applyFilters(teamChatBuddyApplication.getParamFromFile("Response_filter", "TeamChatBuddy.properties"), result);
                                }

                                JSONObject history1 = new JSONObject();
                                history1.put("role", "assistant");
                                history1.put(content, result);

                                existingHistoryArray.put(history1);

                                // récupérer la total_tokens :
                                JSONObject usages = jsonObj.getJSONObject("usage");
                                int total_tokens = Integer.parseInt(usages.getString("total_tokens"));

                                int inputTokens = Integer.parseInt(usages.getString("prompt_tokens"));
                                int outputTokens = Integer.parseInt(usages.getString("completion_tokens"));
                                Log.i("MYA", "traitementResponseChatGpt: totalTokens --- " + total_tokens);
                                Log.i("MYA", "traitementResponseChatGpt: inputTokens --- " + inputTokens);
                                Log.i("MYA", "traitementResponseChatGpt: outputTokens --- " + outputTokens);

                                //-------Calculer la consommation OpenAI

                                teamChatBuddyApplication.calcul_consommation(teamChatBuddyApplication.getparam("model"), inputTokens, outputTokens);

                                // Stocker la nouvelle version de l'historique
                                if (total_tokens > Max_LIMIT) {

                                    existingHistoryArray.remove(1);
                                    existingHistoryArray.remove(1);

                                    teamChatBuddyApplication.setparam(historicMessages, existingHistoryArray.toString());

                                } else {
                                    teamChatBuddyApplication.setparam(historicMessages, existingHistoryArray.toString());
                                }


                                // Géerer le cas où la réponse est vide

                                if (result.equals("")) {
                                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_en + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                    } else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")) {
                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_fr + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                    } else {
                                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(tag, "translatedText exception  " + e);
                                            }
                                        });
                                    }
                                }
                                // Play la suite de la réponse.
                                else {
                                    Log.e("MRAA", "Detection_de_langue  -----" + teamChatBuddyApplication.getparam("Detection_de_langue").contains("yes"));
                                    Log.e("MRAA", "nombreDeMotsCheck(result) -------" + teamChatBuddyApplication.nombreDeMotsCheck(result));
                                    if (teamChatBuddyApplication.getparam("Detection_de_langue").contains("yes") && teamChatBuddyApplication.nombreDeMotsCheck(result)) {

                                        LanguageIdentifier languageIdentifier =
                                                LanguageIdentification.getClient();
                                        languageIdentifier.identifyPossibleLanguages(result)
                                                .addOnSuccessListener(
                                                        new OnSuccessListener<List<IdentifiedLanguage>>() {
                                                            @Override
                                                            public void onSuccess(List<IdentifiedLanguage> identifiedLanguages) {
                                                                if (identifiedLanguages.isEmpty()) {
                                                                    Log.i("MRA_idetifyLanguage", "Can't identify language.");
                                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion));
                                                                } else {
                                                                    // Utiliser la première langue identifiée
                                                                    IdentifiedLanguage language = identifiedLanguages.get(0);
                                                                    String languageCode = language.getLanguageTag();
                                                                    float confidence = language.getConfidence();

                                                                    Log.i("MRA_idetifyLanguage", "Language: " + languageCode + ", Confidence: " + confidence);
                                                                    if (teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties").trim().equals("") && !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties").trim().equals("0")) {
                                                                        if (Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties")) <= (confidence * 100)) {
                                                                            teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;" + languageCode.trim());
                                                                        } else {
                                                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;" + "NotDetected");
                                                                        }
                                                                    } else {
                                                                        teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;" + languageCode.trim());
                                                                    }
                                                                }
                                                            }
                                                        })
                                                .addOnFailureListener(
                                                        new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion));
                                                            }
                                                        });
                                    } else {
                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion));
                                    }

                                }

                            } catch (Exception e) {
                                if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_fr) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_es) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_de) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                } else {
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(tag, "translatedText exception  " + e);
                                        }
                                    });
                                }
                            }
                        }
                    } else if (responseCode >= 400) {
                        try {
                            if (response != null) {

                                JsonObject errorLOG = new JsonObject();
                                JsonObject errorCode = new JsonObject();
                                errorCode.addProperty("ERROR CODE", responseCode);

                                // Obtenez la réponse JSON sous forme de chaîne
                                String jsonString = response;

                                // Analysez la chaîne JSON en un objet JSON
                                JSONObject jsonErrorContent = new JSONObject(jsonString);
                                // Accédez à chaque élément et sa valeur individuellement
                                JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                                String message = errorObject.getString("message");
                                String type = errorObject.getString("type");
                                String param = errorObject.getString("param");
                                String code = errorObject.getString("code");
                                JsonObject reformErrorJson = new JsonObject();
                                reformErrorJson.addProperty("message", message);
                                reformErrorJson.addProperty("type", type);
                                reformErrorJson.addProperty("param", param);
                                reformErrorJson.addProperty("code", code);

                                int checkErrorCode = responseCode;
                                // Calcul de la consommation openai de le cas d'echec
                                if (checkErrorCode == 500 || checkErrorCode == 503 || checkErrorCode == 504) {
                                    int inputTokens = teamChatBuddyApplication.getRequestTotalTokens(requestBody);
                                    teamChatBuddyApplication.calcul_consommation(teamChatBuddyApplication.getparam("model"), inputTokens, 0);
                                }
                                errorCode.add("ERROR Body", reformErrorJson);
                                errorLOG.add("OpenAIERROR", errorCode);
                                //Mettre   le fichier le plus récent reçu
                                String fileName2 = "ERROR-LOG";
                                File file3 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName2 + ".json");
                                try {
                                    if (file3.exists() && file3.isFile()) {
                                        file3.delete();
                                    }
                                    FileWriter fileWriter = new FileWriter(file3);
                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                    String jsonStringF = gson.toJson(errorLOG);
                                    fileWriter.write(jsonStringF);
                                    fileWriter.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                String errorTXT = new Date().toString() + ", OpenAIERROR,ERROR CODE= " + responseCode + ", ERROR Body{ message= " + message + ", type= " + type + ", param= " + param + ", code= " + code + "}" + System.getProperty("line.separator");
                                File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");
                                try {
                                    FileWriter fileWriter = new FileWriter(file2, true);
                                    fileWriter.write(errorTXT);
                                    fileWriter.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_en + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                        } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_fr + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                        } else {
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(tag, "translatedText exception  " + e);
                                }
                            });
                        }
                    }
                }).start();
            }
        }
        catch (Exception e) {
            teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
            teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
            Log.e(tag, "Exception pendant la récupération de la réponse ChatGPT : " + e);
            e.printStackTrace();

            if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_fr)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_es)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_de)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
            }
            else {
                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {

                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+translatedText+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(tag,"translatedText exception  "+e);
                    }
                });

            }

        }


    }

    private void translate(String message, Commande.ITranslationCallback iTranslationCallback){
        if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais") ){
            iTranslationCallback.onTranslated(teamChatBuddyApplication.getParamFromFile(message+"_en", "TeamChatBuddy.properties")+teamChatBuddyApplication.getPromptFromFile("CMD_en_", "TeamChatBuddy.properties"));
        }
        else if (teamChatBuddyApplication.getLangue().getNom().equals("Français") ) {
            iTranslationCallback.onTranslated(teamChatBuddyApplication.getParamFromFile(message+"_fr", "TeamChatBuddy.properties")+teamChatBuddyApplication.getPromptFromFile("CMD_fr_", "TeamChatBuddy.properties"));
        }
        else {
            //use english prompt for other languages (avoid translation because it causes changes in the <CMD_X> as well)
            iTranslationCallback.onTranslated(teamChatBuddyApplication.getParamFromFile(message+"_en", "TeamChatBuddy.properties")+teamChatBuddyApplication.getPromptFromFile("CMD_en_", "TeamChatBuddy.properties"));
            /*teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile(message+"_en", "TeamChatBuddy.properties"))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            iTranslationCallback.onTranslated(translatedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            iTranslationCallback.onTranslated(teamChatBuddyApplication.getParamFromFile(message+"_en", "TeamChatBuddy.properties"));
                        }
                    });*/
        }
    }

    public void getCommandsResponseFromChatGPT(String texte, int numberOfQuestion){

        teamChatBuddyApplication.notifyObservers("hideCameraQr");
        teamChatBuddyApplication.setAlreadyChatting(true);
        Log.i("DLA","Commande active ? " + teamChatBuddyApplication.getparam( "Commands" ));
        if(teamChatBuddyApplication.getparam("Commands").contains("yes")) {
            commande = new Commande( activity );

            translate("COMMAND_Prompt", new Commande.ITranslationCallback() {
                @Override
                public void onTranslated(String translatedText) {
                    prompt_cmd = translatedText;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i( "DLA", "Commande : question " + texte );
                            JSONObject jsonParams = new JSONObject();

                            try {

                                // define the model
                                jsonParams.put( "model", teamChatBuddyApplication.getparam( "COMMAND_Model" ) );
                                jsonParams.put( "temperature", Double.parseDouble( teamChatBuddyApplication.getparam( "COMMAND_Temperature" ) ) );
                                jsonParams.put( "max_tokens", Double.parseDouble( teamChatBuddyApplication.getparam( "Max_Tokens_resp" ) ) );




                                // get the historic commandes :
                                Log.e("DLA","get the historic commandes ");
                                String jsonArrayString = teamChatBuddyApplication.getparam("commandes");
                                Log.e("DLA","get the historic commandes  "+jsonArrayString);
                                Log.e("DLA","get the historic commandes 1");
                                JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                Log.e("DLA","get the historic commandes 2");

                                if (existingHistoryArray.length() != 0){
                                    Log.e("FCH","!!!!!existinghistory 0");


                                    try {
                                        String systemContent = null;
                                        // Parcourir les éléments du tableau
                                        for (int i = 0; i < existingHistoryArray.length(); i++) {
                                            JSONObject messageObject = existingHistoryArray.getJSONObject(i);

                                            // Vérifier si le rôle est "system"
                                            if (messageObject.getString("role").equals("system")) {
                                                // Récupérer le contenu correspondant

                                                systemContent = messageObject.getString("content");
                                                Log.e("FCH","systemContent list "+systemContent);
                                            }
                                        }
                                        if (systemContent!=null && !systemContent.equalsIgnoreCase(prompt_cmd)){
                                            // Recherche de l'objet "system" dans le JSONArray
                                            for (int i = 0; i < existingHistoryArray.length(); i++) {
                                                JSONObject roleObj = existingHistoryArray.getJSONObject(i);
                                                if (roleObj.has("role") && roleObj.getString("role").equals("system")) {
                                                    // Mettre à jour la valeur de 'content' dans l'objet "system"
                                                    roleObj.put(content, prompt_cmd);
                                                    // Sortie de la boucle après la mise à jour
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (JSONException e) {
                                        Log.e("FCH","exception "+e);
                                        e.printStackTrace();
                                    }
                                    // define the role
                                }
                                else {
                                    Log.e("FCH","existinghistory 0");
                                    JSONObject Role = new JSONObject();
                                    Role.put("role", "system");
                                    Role.put(content, prompt_cmd);
                                    existingHistoryArray.put(Role);
                                }

                                JSONObject Question = new JSONObject();
                                Question.put( "role", "user" );
                                Question.put( content, texte );
                                existingHistoryArray.put( Question );
                                jsonParams.put( "messages", existingHistoryArray );

                                //logger CMD-sent.json
                                try {
                                    File file_CMD_SENT = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/CMD-sent.json");
                                    if (file_CMD_SENT.exists() && file_CMD_SENT.isFile()) {
                                        file_CMD_SENT.delete();
                                    }
                                    FileWriter fileWriter = new FileWriter(file_CMD_SENT);
                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                    String jsonString=gson.toJson(jsonParams);
                                    fileWriter.write(jsonString);
                                    fileWriter.close();
                                } catch (Exception e) {
                                    Log.e("DLA","exception create file send cmd "+e);
                                    e.printStackTrace();
                                }
                                new Thread(() -> {
                                    try {
                                        Map<String, String> headers = new HashMap<>();
                                        headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"));
                                        headers.put("Content-Type", "application/json; charset=utf-8");

                                        String apiUrl = teamChatBuddyApplication.getParamFromFile("ChatGPT_url","TeamChatBuddy.properties")+"/v1/chat/completions";

                                        HttpResponse httpResponse = HttpClientUtils.sendPost(
                                                apiUrl,
                                                jsonParams.toString(),
                                                headers,
                                                50000
                                        );

                                        teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                                        Log.e("HHO","response :  "+httpResponse.responseCode+" body : "+httpResponse.body);

                                        if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300 && httpResponse.body != null) {
                                            // logger CMD-recv.json
                                            try {
                                                File file_CMD_RECV = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/CMD-recv.json");
                                                if (file_CMD_RECV.exists() && file_CMD_RECV.isFile()) {
                                                    file_CMD_RECV.delete();
                                                }
                                                FileWriter fileWriter = new FileWriter(file_CMD_RECV);
                                                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                                String jsonString = gson.toJson(new JSONObject(httpResponse.body));
                                                fileWriter.write(jsonString);
                                                fileWriter.close();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            if (httpResponse.body.contains("Failed to find bot")) {
                                                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                                                if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_en) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_fr) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_es) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_de) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                } else {
                                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBotNoFound_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                                                        @Override
                                                        public void onSuccess(String translatedText) {
                                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.e(tag, "translatedText exception  " + e);
                                                        }
                                                    });
                                                }
                                            } else if (httpResponse.body.contains("NoResponseServer")) {
                                                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                                                if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_en + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_fr + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                } else {
                                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
                                                        @Override
                                                        public void onSuccess(String translatedText) {
                                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.e(tag, "translatedText exception  " + e);
                                                        }
                                                    });
                                                }
                                            }
                                            // Traitement de la réponse :
                                            else {
                                                try {
                                                    JSONObject jsonObj = new JSONObject(httpResponse.body);
                                                    JSONArray choicesArray = jsonObj.getJSONArray("choices");
                                                    JSONObject choiceObject = choicesArray.getJSONObject(0);
                                                    JSONObject messageObject = choiceObject.getJSONObject("message");

                                                    result = messageObject.getString(content);

                                                    Log.i("DLA", "Commande : Réponse " + result);
                                                    if (teamChatBuddyApplication.getParamFromFile("COMMAND_histo", "TeamChatBuddy.properties") != null && teamChatBuddyApplication.getParamFromFile("COMMAND_histo", "TeamChatBuddy.properties").trim().equalsIgnoreCase("yes")) {
                                                        JSONObject history1 = new JSONObject();
                                                        history1.put("role", "assistant");
                                                        history1.put(content, result);

                                                        existingHistoryArray.put(history1);
                                                        Log.i("DLA", "Commande : Réponse existingHistoryArray.length()= " + existingHistoryArray.length());
                                                        if (existingHistoryArray.length() > Integer.parseInt(teamChatBuddyApplication.getParamFromFile("COMMAND_maxdialog", "TeamChatBuddy.properties"))) {
                                                            existingHistoryArray.remove(1);
                                                            existingHistoryArray.remove(1);
                                                            teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());
                                                        } else {
                                                            teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());
                                                        }
                                                    }
                                                    if (result.equals("")) {
                                                        activity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                getEmotionResponseFromChatGTP(texte, numberOfQuestion);
                                                                if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                                                    if (teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties").trim())) {
                                                                        getResponseFromChatGPT(texte + " " + teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties"), numberOfQuestion);
                                                                    } else {
                                                                        getResponseFromChatGPT(texte, numberOfQuestion);
                                                                    }
                                                                } else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")) {
                                                                    if (teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties").trim())) {
                                                                        getResponseFromChatGPT(texte + " " + teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties"), numberOfQuestion);
                                                                    } else {
                                                                        getResponseFromChatGPT(texte, numberOfQuestion);
                                                                    }
                                                                } else {
                                                                    if (teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties").trim())) {
                                                                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties")).addOnSuccessListener(new OnSuccessListener<String>() {
                                                                            @Override
                                                                            public void onSuccess(String translatedText) {
                                                                                getResponseFromChatGPT(texte + " " + translatedText, numberOfQuestion);
                                                                            }
                                                                        }).addOnFailureListener(new OnFailureListener() {
                                                                            @Override
                                                                            public void onFailure(@NonNull Exception e) {
                                                                                Log.e(tag, "translatedText exception  " + e);
                                                                            }
                                                                        });
                                                                    } else {
                                                                        getResponseFromChatGPT(texte, numberOfQuestion);
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }
                                                    // Play la suite de la réponse.
                                                    else {
                                                        if (teamChatBuddyApplication.getListOfCommandmustToHavePlayed() != null) {
                                                            teamChatBuddyApplication.getListOfCommandmustToHavePlayed().clear();
                                                        }
                                                        List<String> commandes = commande.regex(result);
                                                        for (String c : commandes) {
                                                            Log.e("Commande", "commande= " + c);
                                                        }
                                                        teamChatBuddyApplication.setListOfCommandmustToHavePlayed(commandes);
                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(false);
                                                        String firstCommandToExecute = commandes.get(0);
                                                        if (commandes.size() > 1) {
                                                            teamChatBuddyApplication.setMultiCommandsDetected(true);
                                                        } else {
                                                            teamChatBuddyApplication.setMultiCommandsDetected(false);
                                                        }
                                                        teamChatBuddyApplication.getListOfCommandmustToHavePlayed().remove(0);
                                                        if (!commande.start_action(firstCommandToExecute, numberOfQuestion, texte)) {
                                                            Log.e(tag, "COMMANDE NON RECONNUE : Start Emotion + ReponseFromChatGPT");
                                                            activity.runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    getEmotionResponseFromChatGTP(texte, numberOfQuestion);
                                                                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                                                        if (teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties").trim())) {
                                                                            getResponseFromChatGPT(texte + " " + teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties"), numberOfQuestion);
                                                                        } else {
                                                                            getResponseFromChatGPT(texte, numberOfQuestion);
                                                                        }
                                                                    } else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")) {
                                                                        if (teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties").trim())) {
                                                                            getResponseFromChatGPT(texte + " " + teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties"), numberOfQuestion);
                                                                        } else {
                                                                            getResponseFromChatGPT(texte, numberOfQuestion);
                                                                        }
                                                                    } else {
                                                                        if (teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties").trim())) {
                                                                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties")).addOnSuccessListener(new OnSuccessListener<String>() {
                                                                                @Override
                                                                                public void onSuccess(String translatedText) {
                                                                                    getResponseFromChatGPT(texte + " " + translatedText, numberOfQuestion);
                                                                                }
                                                                            }).addOnFailureListener(new OnFailureListener() {
                                                                                @Override
                                                                                public void onFailure(@NonNull Exception e) {
                                                                                    Log.e(tag, "translatedText exception  " + e);
                                                                                }
                                                                            });
                                                                        } else {
                                                                            getResponseFromChatGPT(texte, numberOfQuestion);
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                        } else if (!firstCommandToExecute.split(" ")[0].equals("CMD_PROMPT")) {
                                                            String jsonArrayString1 = teamChatBuddyApplication.getparam(historicMessages);
                                                            JSONArray existingHistoryArray1 = new JSONArray(jsonArrayString1);

                                                            JSONObject Question1 = new JSONObject();
                                                            Question1.put("role", "user");
                                                            Question1.put(content, texte);
                                                            existingHistoryArray1.put(Question1);
                                                            teamChatBuddyApplication.setparam(historicMessages, existingHistoryArray1.toString());
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                                                    e.printStackTrace();
                                                    if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                    } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_fr) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                    } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_es) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                    } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_de) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                    } else {
                                                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                                                            @Override
                                                            public void onSuccess(String translatedText) {
                                                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.e(tag, "translatedText exception  " + e);
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        } else {
                                            try {
                                                // logger CMD-ERROR-LOG.json et CMD-ERROR-History.json
                                                if (httpResponse.body != null) {
                                                    JsonObject errorLOG = new JsonObject();
                                                    JsonObject errorCode = new JsonObject();
                                                    JsonObject reformErrorJson = new JsonObject();
                                                    errorCode.addProperty("ERROR CODE", httpResponse.responseCode);
                                                    JSONObject jsonErrorContent = new JSONObject(httpResponse.body);
                                                    JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                                                    reformErrorJson.addProperty("message", errorObject.getString("message"));
                                                    reformErrorJson.addProperty("type", errorObject.getString("type"));
                                                    reformErrorJson.addProperty("param", errorObject.getString("param"));
                                                    reformErrorJson.addProperty("code", errorObject.getString("code"));
                                                    errorCode.add("ERROR Body", reformErrorJson);
                                                    errorLOG.add("OpenAIERROR", errorCode);
                                                    File file_CMD_ERROR_LOG = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/CMD-ERROR-LOG.json");
                                                    if (file_CMD_ERROR_LOG.exists() && file_CMD_ERROR_LOG.isFile()) {
                                                        file_CMD_ERROR_LOG.delete();
                                                    }
                                                    FileWriter fileWriter = new FileWriter(file_CMD_ERROR_LOG);
                                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                                    String jsonStringF = gson.toJson(errorLOG);
                                                    fileWriter.write(jsonStringF);
                                                    fileWriter.close();

                                                    //logger CMD-ERROR-History.json
                                                    File file_CMD_ERROR_History = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/CMD-ERROR-History.txt");
                                                    FileWriter fileWriter2 = new FileWriter(file_CMD_ERROR_History, true);
                                                    fileWriter2.write(new Date().toString() +
                                                            ", OpenAIERROR,ERROR CODE= " + httpResponse.responseCode +
                                                            ", ERROR Body{ message= " + errorObject.getString("message") +
                                                            ", type= " + errorObject.getString("type") +
                                                            ", param= " + errorObject.getString("param") +
                                                            ", code= " + errorObject.getString("code") +
                                                            "}"
                                                            + System.getProperty("line.separator"));
                                                    fileWriter2.close();
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    getEmotionResponseFromChatGTP(texte, numberOfQuestion);
                                                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                                        if (teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties").trim())) {
                                                            getResponseFromChatGPT(texte + " " + teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties"), numberOfQuestion);
                                                        } else {
                                                            getResponseFromChatGPT(texte, numberOfQuestion);
                                                        }
                                                    } else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")) {
                                                        if (teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties").trim())) {
                                                            getResponseFromChatGPT(texte + " " + teamChatBuddyApplication.getParamFromFile("Response_format_fr", "TeamChatBuddy.properties"), numberOfQuestion);
                                                        } else {
                                                            getResponseFromChatGPT(texte, numberOfQuestion);
                                                        }
                                                    } else {
                                                        if (teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties").trim())) {
                                                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Response_format_en", "TeamChatBuddy.properties")).addOnSuccessListener(new OnSuccessListener<String>() {
                                                                @Override
                                                                public void onSuccess(String translatedText) {
                                                                    getResponseFromChatGPT(texte + " " + translatedText, numberOfQuestion);
                                                                }
                                                            }).addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Log.e(tag, "translatedText exception  " + e);
                                                                }
                                                            });
                                                        } else {
                                                            getResponseFromChatGPT(texte, numberOfQuestion);
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    } catch (Exception e) {
                                        teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                                        teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                                        Log.e(tag, "Exception pendant la récupération de la réponse ChatGPT : " + e);
                                        e.printStackTrace();
                                        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                        } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_fr) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                        } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_es) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                        } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBot_ERROR_de) + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                        } else {
                                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                                                @Override
                                                public void onSuccess(String translatedText) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf(numberOfQuestion) + ";SPLIT;onError");
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e(tag, "translatedText exception  " + e);
                                                }
                                            });
                                        }
                                    }
                                }).start();
                            } catch (Exception e) {
                                Log.e("DLA","get the historic commandes exception "+e);
                                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                                teamChatBuddyApplication.setGetResponseTime( System.currentTimeMillis() );
                                Log.e( tag, "Exception pendant la récupération de la réponse ChatGPT : " + e );
                                e.printStackTrace();

                                if (teamChatBuddyApplication.getLangue().getNom().equals( langueEn )) {
                                    teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString( R.string.chatBot_ERROR_en ) + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                } else if (teamChatBuddyApplication.getLangue().getNom().equals( langueFr )) {
                                    teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString( R.string.chatBot_ERROR_fr ) + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                } else if (teamChatBuddyApplication.getLangue().getNom().equals( langueEs )) {
                                    teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString( R.string.chatBot_ERROR_es ) + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                } else if (teamChatBuddyApplication.getLangue().getNom().equals( langueDe )) {
                                    teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString( R.string.chatBot_ERROR_de ) + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                } else {
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate( teamChatBuddyApplication.getString( R.string.chatBot_ERROR_en ) ).addOnSuccessListener( new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {

                                            teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + translatedText + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                        }

                                    } ).addOnFailureListener( new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e( tag, "translatedText exception  " + e );
                                        }
                                    } );

                                }

                            }
                        }
                    });

                }
            });

        }

        else {
            Log.e( tag, "COMMANDE NON ACTIVE : Start Emotion + Reponse" );
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getEmotionResponseFromChatGTP(texte, numberOfQuestion);
                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                        if (teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties").trim())){
                            getResponseFromChatGPT(texte+" "+teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties"),numberOfQuestion);
                        }
                        else {
                            getResponseFromChatGPT(texte, numberOfQuestion);
                        }
                    }
                    else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                        if (teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties").trim())){
                            getResponseFromChatGPT(texte+" "+teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties"),numberOfQuestion);
                        }
                        else {
                            getResponseFromChatGPT(texte, numberOfQuestion);
                        }
                    }
                    else {
                        if (teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties").trim())){
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")).addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {

                                    getResponseFromChatGPT(texte+" "+translatedText,numberOfQuestion);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(tag,"translatedText exception  "+e);
                                }
                            });

                        }
                        else {
                            getResponseFromChatGPT(texte, numberOfQuestion);
                        }
                    }
                }
            });
        }
    }
    public void executeCommand(){
        teamChatBuddyApplication.isListeningHotw = false;
        if (teamChatBuddyApplication.getListOfCommandmustToHavePlayed()!=null){

            teamChatBuddyApplication.notifyObservers("hideCameraQr");
            String cmd = teamChatBuddyApplication.getListOfCommandmustToHavePlayed().get(0);
            if (teamChatBuddyApplication.getListOfCommandmustToHavePlayed().size()==1){
                teamChatBuddyApplication.setMultiCommandsDetected(false);
                teamChatBuddyApplication.getListOfCommandmustToHavePlayed().remove(0);
                commande.start_action(cmd, teamChatBuddyApplication.getQuestionNumber(),"");
            }
            else{
                teamChatBuddyApplication.getListOfCommandmustToHavePlayed().remove(0);
                commande.start_action(cmd, teamChatBuddyApplication.getQuestionNumber(),"");
            }
        }


    }
    public void getQuestionToDescribePicture(Bitmap bitmap,String prompt){

        teamChatBuddyApplication.notifyObservers("hideCameraQr");
        Bitmap bitmapReszeResolution = imageResolution(bitmap);
         if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
            traitementPhotoByChatGpt(bitmapReszeResolution,prompt);
        }
        else{
            teamChatBuddyApplication.getFrenchLanguageSelectedTranslator().translate( prompt).addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String translatedText) {
                    Log.e("MRRM","send photo to chatGPT translated question "+translatedText);
                    traitementPhotoByChatGpt(bitmapReszeResolution,translatedText);
                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("ChatGPT_Picture_Description","translatedText exception  "+e);
                }
            });

        }
    }
    public Bitmap imageResolution(Bitmap bitmap){
        String resolution="";
        if (teamChatBuddyApplication.getParamFromFile("Picture_Description_resolution","TeamChatBuddy.properties")!= null && !teamChatBuddyApplication.getParamFromFile("Picture_Description_resolution","TeamChatBuddy.properties").trim().equals("")){
            if (teamChatBuddyApplication.getParamFromFile("Picture_Description_resolution","TeamChatBuddy.properties").contains("/")){
                resolution = teamChatBuddyApplication.getParamFromFile("Picture_Description_resolution","TeamChatBuddy.properties").split("/")[0].trim().toLowerCase();
            }
        }
        switch (resolution){
            case "low":
                return resizeImage(bitmap,640,480);
            case "medium":
                return resizeImage(bitmap,1280,960);
            default:
                return bitmap;
        }
    }
    public Bitmap resizeImage(Bitmap originalImage, int width, int height) {
        return Bitmap.createScaledBitmap(originalImage, width, height, false);
    }
    public void traitementPhotoByChatGpt(Bitmap bitmap, String question) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    teamChatBuddyApplication.notifyObservers("hideCameraQr");
                    String base64Image = encodeImageToBase64(bitmap);
                    Log.e("ChatGPT_Picture_Description","base64Image length= "+base64Image.length());

                    // Create JSON request body
                    JSONObject jsonParams = new JSONObject();
                    jsonParams.put("model", teamChatBuddyApplication.getParamFromFile("Picture_Description_model","TeamChatBuddy.properties"));
                    jsonParams.put("max_tokens", Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Picture_Description_max_tokens","TeamChatBuddy.properties")));

                    JSONArray requestArray = new JSONArray();

                    // Add user question
                    JSONObject userMessage = new JSONObject();
                    userMessage.put("role", "user");

                    JSONArray contentArray = new JSONArray();

                    JSONObject textContent = new JSONObject();
                    textContent.put("type", "text");
                    textContent.put("text", question);
                    contentArray.put(textContent);

                    JSONObject imageContent = new JSONObject();
                    imageContent.put("type", "image_url");
                    JSONObject imageUrl = new JSONObject();
                    imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
                    imageContent.put("image_url", imageUrl);
                    contentArray.put(imageContent);

                    userMessage.put("content", contentArray);
                    requestArray.put(userMessage);

                    jsonParams.put("messages", requestArray);

                    JSONObject jsonToFile = new JSONObject();

                    jsonToFile.put("model", teamChatBuddyApplication.getParamFromFile("Picture_Description_model","TeamChatBuddy.properties"));
                    jsonToFile.put("max_tokens", Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Picture_Description_max_tokens","TeamChatBuddy.properties")));

                    JSONArray contentAr = new JSONArray();
                    contentAr.put(textContent);

                    JSONObject imageDescription = new JSONObject();
                    imageDescription.put("type", "image_url");
                    JSONObject url = new JSONObject();
                    JSONObject userQst = new JSONObject();
                    userQst.put("role", "user");


                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            saveBitmapToFile(bitmap,"capturedImage.png");
                            try {
                                File capturedImage = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/capturedImage.png");
                                if(capturedImage!=null){
                                    url.put("url", capturedImage.getPath());
                                    imageDescription.put("image_url", url);
                                    contentAr.put(imageDescription);
                                }
                                userQst.put("content", contentAr);
                                JSONArray requestAr = new JSONArray();
                                requestAr.put(userQst);
                                jsonToFile.put("messages", requestAr);

                                File fileImageDescription_sent = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ImageDescription-sent.json");


                                if (fileImageDescription_sent.exists() && fileImageDescription_sent.isFile()) {
                                    fileImageDescription_sent.delete();
                                }
                                FileWriter fileWriter = new FileWriter(fileImageDescription_sent);
                                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                String jsonString=gson.toJson(jsonToFile);
                                fileWriter.write(jsonString);
                                fileWriter.close();

                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }, 3000);
                    new Thread(() -> {
                        try {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"));
                            headers.put("Content-Type", "application/json; charset=utf-8");

                            String apiUrl = teamChatBuddyApplication.getParamFromFile("ChatGPT_url","TeamChatBuddy.properties")+"/v1/chat/completions";

                            HttpResponse httpResponse = HttpClientUtils.sendPost(apiUrl, jsonParams.toString(), headers, 50000);

                            teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                            if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300 && httpResponse.body != null) {
                                File fileImageDescription_recv = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ImageDescription-recv.json");
                                try {
                                    if (fileImageDescription_recv.exists() && fileImageDescription_recv.isFile()) {
                                        fileImageDescription_recv.delete();
                                    }
                                    FileWriter fileWriter = new FileWriter(fileImageDescription_recv);
                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                    String jsonString = gson.toJson(new JSONObject(httpResponse.body));
                                    fileWriter.write(jsonString);
                                    fileWriter.close();
                                } catch (IOException | JSONException e) {
                                    e.printStackTrace();
                                }
                                String chatgptResponse = "";
                                try {
                                    JSONObject responseBody = new JSONObject(httpResponse.body);
                                    chatgptResponse = responseBody.getJSONArray("choices")
                                            .getJSONObject(0)
                                            .getJSONObject("message")
                                            .getString("content");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                if (teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties") != null
                                        && !teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties").trim().equalsIgnoreCase("")) {
                                    chatgptResponse = teamChatBuddyApplication.applyFilters(
                                            teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties"),
                                            chatgptResponse
                                    );
                                }
                                Log.d("CameraX", "Response: " + chatgptResponse);
                                try {
                                    String jsonArrayString = teamChatBuddyApplication.getparam(historicMessages);
                                    JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put(content, chatgptResponse);
                                    existingHistoryArray.put(history1);
                                    teamChatBuddyApplication.setparam(historicMessages, existingHistoryArray.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + chatgptResponse);
                                Log.e("ChatGPT_Picture_Description", "Réponse ChatGPT photoDescription: " + chatgptResponse);
                            } else {
                                if (httpResponse.body != null) {
                                    Log.e("ChatGPT_Picture_Description", "Réponse ChatGPT photoDescription [not successful] ");
                                    try {
                                        JSONObject jsonErrorContent = new JSONObject(httpResponse.body);
                                        String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_TAKE_PHOTO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                        logErrorAPIHealysa("CMD_TAKE_PHOTO", errorTXT, "notOnFailure");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Log.e("ChatGPT_Picture_Description", "Réponse ChatGPT photoDescription [not successful]1 catch" + e);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e("ChatGPT_Picture_Description", "Error: " + e.getMessage(), e);
                            logErrorAPIHealysa("CMD_TAKE_PHOTO", e.getMessage(), "onFailure");
                        }
                    }).start();

                } catch (Exception e) {
                    Log.e("ChatGPT_Picture_Description", "Error creating JSON request", e);
                }
            }
        });

    }
    public void logErrorAPIHealysa(String commande,String message,String type){
        String errorTXT="";
        if (type.equals("onFailure")){
            errorTXT= new Date().toString()+", COMMANDERRORAPI,Commande= "+commande+ " ERROR Body{  message= "+message+"}"+System.getProperty("line.separator");
        }
        else {
            errorTXT= message;
        }

        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");


        try {

            FileWriter fileWriter = new FileWriter(file2,true);
            fileWriter.write(errorTXT);
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String encodeImageToBase64(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d("ChatGPT_Picture_Description", "encodeImageToBase64 Width: " + width + ", Height: " + height);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    public void getEmotionResponseFromChatGTP(String texte,int numberOfQuestion){
        if (teamChatBuddyApplication.getparam("switch_emotion").contains("yes")) {
            teamChatBuddyApplication.setUsingEmotions( true );
            JSONObject jsonParams = new JSONObject();

            try {

                // define the model
                jsonParams.put( "model", teamChatBuddyApplication.getParamFromFile( "emotion_Model", "TeamChatBuddy.properties" ) );
                JSONArray existingHistoryArray = new JSONArray();


                Log.e( "FCH", "existinghistory 0" );
                JSONObject Role = new JSONObject();
                Role.put( "role", "system" );
                Role.put( "content", "" + teamChatBuddyApplication.getParamFromFile( "prompt_fr", "TeamChatBuddy.properties" ) + "" );
                existingHistoryArray.put( Role );

                JSONObject question = new JSONObject();
                question.put( "role", "user" );
                question.put( "content", texte );

                existingHistoryArray.put( question );

                jsonParams.put( "messages", existingHistoryArray );
                jsonParams.put( "max_tokens", Double.parseDouble( teamChatBuddyApplication.getParamFromFile( "emotion_Max_tokens", "TeamChatBuddy.properties" ) ) );
                jsonParams.put( "temperature", Double.parseDouble( teamChatBuddyApplication.getParamFromFile( "emotion_Temperature", "TeamChatBuddy.properties" ) ) );

                jsonParams.put( "top_p", 1.0 );
                jsonParams.put( "frequency_penalty", 0.0 );
                jsonParams.put( "presence_penalty", 0.0 );
                JSONArray stopArray = new JSONArray();
                stopArray.put( "\n" );
                jsonParams.put( "stop", stopArray );
                jsonParams.put( "format", "dict" );
                jsonParams.put( "stream", Boolean.parseBoolean( "False" ) );
                jsonParams.put( "n", Double.parseDouble( "1" ) );
                jsonParams.put( "user", "userID123" );
                //  teamChatTabletteApplication.getparam("Max_Tokens")

                // get the historic messages :


                //Mettre  le dernier fichier json envoyé à l’API
                //Mettre  le dernier fichier json envoyé à l’API
                String fileName = "OpenAI-sent";

                File file1 = new File( Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json" );

                try {
                    if (file1.exists() && file1.isFile()) {
                        file1.delete();
                        Log.v( "Json_API", "file deleted" );
                    }

                    FileWriter fileWriter = new FileWriter( file1 );
                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                    String jsonString = gson.toJson( jsonParams );
                    fileWriter.write( jsonString );
                    fileWriter.close();
                    Log.v( "Json_API", "new file added" );

                } catch (IOException e) {
                    e.printStackTrace();
                }

                new Thread(() -> {
                    try {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"));
                        headers.put("Content-Type", "application/json; charset=utf-8");

                        String apiUrl = teamChatBuddyApplication.getParamFromFile("ChatGPT_url", "TeamChatBuddy.properties") + "/v1/chat/completions";

                        HttpResponse httpResponse = HttpClientUtils.sendPost(apiUrl, jsonParams.toString(), headers, 50000);

                        Log.i("responseTimeout", "Réponse ChatGPT emotion  ");
                        teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                        Log.e("MEHDI", "Réponse ChatGPT code " + httpResponse.responseCode);
                        Log.e("MEHDI", "Réponse ChatGPT body " + httpResponse.body);

                        if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300 && httpResponse.body != null) {
                            JSONObject jsonObj = null;
                            try {
                                jsonObj = new JSONObject(httpResponse.body);
                                JSONArray choicesArray = jsonObj.getJSONArray("choices");
                                JSONObject choiceObject = choicesArray.getJSONObject(0);
                                JSONObject messageObject = choiceObject.getJSONObject("message");
                                Log.e("MEHDI", "Emotion detected------------------- " + messageObject.getString("content"));

                                //Calcul de la consommation d'openai cas des commandes
                                JSONObject usages = jsonObj.getJSONObject("usage");
                                int inputTokens = Integer.parseInt(usages.getString("prompt_tokens"));
                                int outputTokens = Integer.parseInt(usages.getString("completion_tokens"));
                                Log.i("tokens", "result: " + result + "\ninputTokens: " + inputTokens + "\noutputTokens: " + outputTokens);

                                teamChatBuddyApplication.calcul_consommation(teamChatBuddyApplication.getParamFromFile("emotion_Model", "TeamChatBuddy.properties"), inputTokens, outputTokens);

                                if (messageObject.getString("content").contains("(")) {
                                    String content = messageObject.getString("content");
                                    String emotion = null;
                                    int startIndex = content.indexOf("(");
                                    int endIndex = content.indexOf(")");
                                    if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                                        emotion = content.substring(startIndex + 1, endIndex);
                                    }
                                    if (emotion != null) {
                                        Log.e("MEHDI", "Emotion detected after------------------- " + emotion);
                                        teamChatBuddyApplication.notifyObservers("getResponseF;SPLIT;gpt;SPLIT;" + emotion.trim().toLowerCase() + ";SPLIT;" + numberOfQuestion);
                                    } else {
                                        teamChatBuddyApplication.notifyObservers("getResponseF;SPLIT;gpt;SPLIT;" + messageObject.getString("content").trim().toLowerCase() + ";SPLIT;" + numberOfQuestion);
                                    }
                                } else {
                                    teamChatBuddyApplication.notifyObservers("getResponseF;SPLIT;gpt;SPLIT;" + messageObject.getString("content").trim().toLowerCase() + ";SPLIT;" + numberOfQuestion);
                                }

                                //Mettre  le dernier fichier json reçu à l’API
                                File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + "OpenAI-recv" + ".json");
                                try {
                                    if (file2.exists() && file2.isFile()) {
                                        file2.delete();
                                        Log.v("Json_API", "file deleted");
                                    }
                                    FileWriter fileWriter = new FileWriter(file2);
                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                    String jsonString = gson.toJson(jsonObj);
                                    fileWriter.write(jsonString);
                                    fileWriter.close();
                                    Log.v("Json_API", "new file added");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            if (httpResponse.body != null) {
                                int checkErrorCode = httpResponse.responseCode;
                                // Calcul de la consommation openai de le cas d'echec
                                if (checkErrorCode == 500 || checkErrorCode == 503 || checkErrorCode == 504) {
                                    int inputTokens = teamChatBuddyApplication.getRequestTotalTokens(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams.toString()));
                                    teamChatBuddyApplication.calcul_consommation(teamChatBuddyApplication.getParamFromFile("emotion_Model", "TeamChatBuddy.properties"), inputTokens, 0);
                                }
                            }
                        }
                    } catch (Exception e) {
                        teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                        Log.e("MEHDI", "Onfailure pendant la récupération de la réponse dataChatGPT : " + e);
                    }
                }).start();
            } catch (Exception e) {
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                Log.e( "MEHDI", "Exception pendant la récupération de la réponse dataChatGPT : " + e );

            }

        } else teamChatBuddyApplication.setUsingEmotions( false );
    }
    public void getSessionId(String question)  {
        teamChatBuddyApplication.setAlreadyChatting(true);

        JSONObject jsonParams = new JSONObject();


        // define the model
        try {
            jsonParams.put("name", "test");
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                teamChatBuddyApplication.getCustomGPTStreamMode().reset();
            }
            teamChatBuddyApplication.setCustomGPTStreamMode(new CustomGPTStreamMode(activity));
            teamChatBuddyApplication.getCustomGPTStreamMode().sendRequestToGetSessionID(jsonParams,question);
            Log.e("MEHDI","init CustomGPT");
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


    public void getInvitationFromChatGPT(String model,double temperature, int max_token, String prompt){
        teamChatBuddyApplication.notifyObservers("hideCameraQr");

        Log.e("TEAMCHAT_BUDDY_TRACKING","getInvitationFromChatGPT ("+model+" , "+temperature+" , "+max_token+" , "+prompt+")");

        String RoleBuddy;

        // vérifie si la langue actuelle de l'application est l'anglais.
        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn) ){
            RoleBuddy = teamChatBuddyApplication.getparam("header");
        }
        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr) ) {
            RoleBuddy = teamChatBuddyApplication.getparam("entete");
        }
        else {
            RoleBuddy = teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete");
        }

        JSONObject jsonParams = new JSONObject();
        try {

            // define the model
            jsonParams.put( "model", model );
            jsonParams.put( "temperature", temperature );
            jsonParams.put( "max_tokens", max_token );

            JSONArray requestArray = new JSONArray();
            JSONObject Role = new JSONObject();
            Role.put( "role", "system" );
            Role.put( content, RoleBuddy );
            requestArray.put( Role );
            JSONObject Question = new JSONObject();
            Question.put( "role", "user" );
            Question.put( content, prompt );
            requestArray.put( Question );
            jsonParams.put( "messages", requestArray );

            //logger Invit-sent.json
            try {
                File file_INVIT_SENT = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Invit-sent.json");
                if (file_INVIT_SENT.exists() && file_INVIT_SENT.isFile()) {
                    file_INVIT_SENT.delete();
                }
                FileWriter fileWriter = new FileWriter(file_INVIT_SENT);
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String jsonString=gson.toJson(jsonParams);
                fileWriter.write(jsonString);
                fileWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Thread(() -> {
                try {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"));
                    headers.put("Content-Type", "application/json; charset=utf-8");

                    String apiUrl = teamChatBuddyApplication.getParamFromFile("ChatGPT_url", "TeamChatBuddy.properties") + "/v1/chat/completions";

                    HttpResponse httpResponse = HttpClientUtils.sendPost(apiUrl, jsonParams.toString(), headers, 50000);

                    teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());

                    if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300 && httpResponse.body != null) {
                        //logger Invit-recv.json
                        try {
                            File file_INVIT_RECV = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Invit-recv.json");
                            if (file_INVIT_RECV.exists() && file_INVIT_RECV.isFile()) {
                                file_INVIT_RECV.delete();
                            }
                            FileWriter fileWriter = new FileWriter(file_INVIT_RECV);
                            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                            String jsonString = gson.toJson(new JSONObject(httpResponse.body));
                            fileWriter.write(jsonString);
                            fileWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            JSONObject jsonObj = new JSONObject(httpResponse.body);
                            // récupérer la réponse
                            JSONArray choicesArray = jsonObj.getJSONArray("choices");
                            JSONObject choiceObject = choicesArray.getJSONObject(0);
                            JSONObject messageObject = choiceObject.getJSONObject("message");

                            result = messageObject.getString(content);

                            // Gérer le cas où la réponse est vide
                            if (result.equals("")) {
                                Log.e("TEAMCHAT_BUDDY_TRACKING", "Réponse ChatGPT [error] : la réponse est vide");

                                if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hello, how can I help you?");
                                }
                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Bonjour, comment puis-je vous aider?");
                                }
                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hola, como puedo ayudarte?");
                                }
                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hallo, wie kann ich Ihnen helfen?");
                                }
                                else {
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Hello, how can I help you?")
                                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                                @Override
                                                public void onSuccess(String translatedText) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + translatedText);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e("TEAMCHAT_BUDDY_TRACKING", "translatedText exception  " + e);
                                                }
                                            });
                                }
                            }
                            // Play la suite de la réponse.
                            else {
                                Log.e("TEAMCHAT_BUDDY_TRACKING", "Detection_de_langue  -----" + teamChatBuddyApplication.getparam("Detection_de_langue").contains("yes"));
                                Log.e("TEAMCHAT_BUDDY_TRACKING", "nombreDeMotsCheck(result) -------" + teamChatBuddyApplication.nombreDeMotsCheck(result));
                                if (teamChatBuddyApplication.getparam("Detection_de_langue").contains("yes") && teamChatBuddyApplication.nombreDeMotsCheck(result)) {
                                    LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
                                    languageIdentifier.identifyPossibleLanguages(result)
                                            .addOnSuccessListener(
                                                    new OnSuccessListener<List<IdentifiedLanguage>>() {
                                                        @Override
                                                        public void onSuccess(List<IdentifiedLanguage> identifiedLanguages) {
                                                            if (identifiedLanguages.isEmpty()) {
                                                                Log.i("MRA_idetifyLanguage", "Can't identify language.");
                                                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + result);
                                                            } else {
                                                                // Utiliser la première langue identifiée
                                                                IdentifiedLanguage language = identifiedLanguages.get(0);
                                                                String languageCode = language.getLanguageTag();
                                                                float confidence = language.getConfidence();

                                                                Log.i("MRA_idetifyLanguage", "Language: " + languageCode + ", Confidence: " + confidence);
                                                                if (teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties").trim().equals("") && !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties").trim().equals("0")) {
                                                                    if (Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties")) <= (confidence * 100)) {
                                                                        teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + result);
                                                                    } else {
                                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + result);
                                                                    }
                                                                } else {
                                                                    teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + result);
                                                                }
                                                            }
                                                        }
                                                    })
                                            .addOnFailureListener(
                                                    new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + result);
                                                        }
                                                    });
                                } else {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + result);
                                }
                            }

                        } catch (Exception e) {
                            Log.e("TEAMCHAT_BUDDY_TRACKING", "Réponse ChatGPT [Exception] : " + e);

                            if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hello, how can I help you?");
                            }
                            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Bonjour, comment puis-je vous aider?");
                            }
                            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hola, como puedo ayudarte?");
                            }
                            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hallo, wie kann ich Ihnen helfen?");
                            }
                            else {
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Hello, how can I help you?")
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + translatedText);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e("TEAMCHAT_BUDDY_TRACKING", "translatedText exception  " + e);
                                            }
                                        });
                            }
                        }

                    } else {
                        try {
                            //logger Invit-ERROR-LOG.json et Invit-ERROR-History.json
                            if (httpResponse.body != null) {
                                JsonObject errorLOG = new JsonObject();
                                JsonObject errorCode = new JsonObject();
                                JsonObject reformErrorJson = new JsonObject();
                                errorCode.addProperty("ERROR CODE", httpResponse.responseCode);
                                JSONObject jsonErrorContent = new JSONObject(httpResponse.body);
                                JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                                reformErrorJson.addProperty("message", errorObject.getString("message"));
                                reformErrorJson.addProperty("type", errorObject.getString("type"));
                                reformErrorJson.addProperty("param", errorObject.getString("param"));
                                reformErrorJson.addProperty("code", errorObject.getString("code"));
                                errorCode.add("ERROR Body", reformErrorJson);
                                errorLOG.add("OpenAIERROR", errorCode);
                                File file_INVIT_ERROR_LOG = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Invit-ERROR-LOG.json");
                                if (file_INVIT_ERROR_LOG.exists() && file_INVIT_ERROR_LOG.isFile()) {
                                    file_INVIT_ERROR_LOG.delete();
                                }
                                FileWriter fileWriter = new FileWriter(file_INVIT_ERROR_LOG);
                                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                String jsonStringF = gson.toJson(errorLOG);
                                fileWriter.write(jsonStringF);
                                fileWriter.close();

                                //logger Invit-ERROR-History.json
                                File file_INVIT_ERROR_History = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Invit-ERROR-History.txt");
                                FileWriter fileWriter2 = new FileWriter(file_INVIT_ERROR_History, true);
                                fileWriter2.write(new Date().toString() +
                                        ", OpenAIERROR,ERROR CODE= " + httpResponse.responseCode +
                                        ", ERROR Body{ message= " + errorObject.getString("message") +
                                        ", type= " + errorObject.getString("type") +
                                        ", param= " + errorObject.getString("param") +
                                        ", code= " + errorObject.getString("code") +
                                        "}"
                                        + System.getProperty("line.separator"));
                                fileWriter2.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Log.e("TEAMCHAT_BUDDY_TRACKING", "Réponse ChatGPT [ERROR] response is not Successful");

                        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hello, how can I help you?");
                        }
                        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Bonjour, comment puis-je vous aider?");
                        }
                        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hola, como puedo ayudarte?");
                        }
                        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hallo, wie kann ich Ihnen helfen?");
                        }
                        else {
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Hello, how can I help you?")
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + translatedText);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("TEAMCHAT_BUDDY_TRACKING", "translatedText exception  " + e);
                                        }
                                    });
                        }
                    }
                } catch (Exception e) {
                    teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                    Log.e("TEAMCHAT_BUDDY_TRACKING", "Réponse ChatGPT [Exception] : " + e);

                    if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hello, how can I help you?");
                    }
                    else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Bonjour, comment puis-je vous aider?");
                    }
                    else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hola, como puedo ayudarte?");
                    }
                    else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hallo, wie kann ich Ihnen helfen?");
                    }
                    else {
                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Hello, how can I help you?")
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + translatedText);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("TEAMCHAT_BUDDY_TRACKING", "translatedText exception  " + e);
                                    }
                                });
                    }
                }
            }).start();
        } catch (Exception e) {
            teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());

            Log.e("TEAMCHAT_BUDDY_TRACKING", "Réponse ChatGPT [Exception] : " + e);

            if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hello, how can I help you?");
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Bonjour, comment puis-je vous aider?");
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hola, como puedo ayudarte?");
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" + "Hallo, wie kann ich Ihnen helfen?");
            }
            else {
                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Hello, how can I help you?")
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;" +translatedText);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("TEAMCHAT_BUDDY_TRACKING","translatedText exception  "+e);
                            }
                        });
            }
        }

    }


    public void saveBitmapToFile(Bitmap bitmap, String filePath) {
        FileOutputStream outputStream = null;

        try {
            File fileImages = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy");
            if (!fileImages.exists() ) {
                fileImages.mkdirs();
            }
            File file = new File(fileImages, filePath);

            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
