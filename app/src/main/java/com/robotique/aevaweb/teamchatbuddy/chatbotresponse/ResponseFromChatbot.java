package com.robotique.aevaweb.teamchatbuddy.chatbotresponse;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.ibm.icu.text.BreakIterator;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.utilis.ApiEndpointInterface;
import com.robotique.aevaweb.teamchatbuddy.utilis.NetworkClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileOutputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

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
        Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication, teamChatBuddyApplication.getparam("ChatGPT_url"),50);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
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

            if (teamChatBuddyApplication.getparam("Mode_Stream").contains("yes")){
                if(teamChatBuddyApplication.getChatGptStreamMode() != null){
                    teamChatBuddyApplication.getChatGptStreamMode().reset();
                }
                teamChatBuddyApplication.setChatGptStreamMode(new ChatGptStreamMode(activity,existingHistoryArray));
                teamChatBuddyApplication.getChatGptStreamMode().sendRequest(api,requestBody);
            }
            else{
                Call call = api.getChatGPT( requestBody, "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"), "application/json");

                call.enqueue(new Callback() {
                    @Override
                    public void onResponse(Call call, Response response) {
                        teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                        teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                        // Vérifie si la réponse est réussie
                        if(response.isSuccessful()) {
                            if (response.body().toString().contains("Failed to find bot")) {

                                if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_en)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                }
                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_fr)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                }
                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_es)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                }
                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_de)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                }
                                else {
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBotNoFound_en)).addOnSuccessListener(new OnSuccessListener<String>() {
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
                            else if (response.body().toString().contains("NoResponseServer")) {
                                if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+chatBotServerNoResponce_en+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                }
                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+chatBotServerNoResponce_fr+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                }
                                else {
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
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
                            // Traitement de la réponse :
                            else {
                                try {

                                    JSONObject jsonObj = new JSONObject(response.body().toString());
                                    //Mettre   le fichier le plus récent reçu
                                    String fileName = "ChatGPT-recv";

                                    File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");

                                    try {
                                        if (file1.exists() && file1.isFile()) {
                                            file1.delete();
                                            Log.v("Json_API", "file deleted");
                                        }

                                        FileWriter fileWriter = new FileWriter(file1);
                                        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                        String jsonString=gson.toJson(jsonObj);
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
                                    if(complete_answer_index != -1) {
                                        result = result.substring(0, complete_answer_index);
                                        Log.e(tag,"the answer after elimination : " + result);
                                    }
                                    else {
                                        Log.e(tag,"no period found in the answer : " + result);
                                    }
                                    Log.e(tag,"the answer with max nb parameter : " + result);

                                    if (teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                                        result = teamChatBuddyApplication.applyFilters(teamChatBuddyApplication.getParamFromFile("Response_filter","TeamChatBuddy.properties"),result);
                                    }

                                    JSONObject history1 = new JSONObject();
                                    history1.put("role", "assistant");
                                    history1.put(content, result);

                                    existingHistoryArray.put(history1);

                                    // récupérer la total_tokens :
                                    JSONObject usages = jsonObj.getJSONObject("usage");
                                    int total_tokens  = Integer.parseInt(usages.getString("total_tokens"));

                                    int inputTokens = Integer.parseInt(usages.getString("prompt_tokens"));
                                    int outputTokens = Integer.parseInt(usages.getString("completion_tokens"));
                                    Log.i("MYA", "traitementResponseChatGpt: totalTokens --- "+total_tokens);
                                    Log.i("MYA", "traitementResponseChatGpt: inputTokens --- "+inputTokens);
                                    Log.i("MYA", "traitementResponseChatGpt: outputTokens --- "+outputTokens);

                                    //-------Calculer la consommation OpenAI

                                    teamChatBuddyApplication.calcul_consommation(teamChatBuddyApplication.getparam("model"), inputTokens, outputTokens);

                                    // Stocker la nouvelle version de l'historique
                                    if(total_tokens  > Max_LIMIT ){

                                        existingHistoryArray.remove(1);
                                        existingHistoryArray.remove(1);

                                        teamChatBuddyApplication.setparam(historicMessages, existingHistoryArray.toString());

                                    }else {
                                        teamChatBuddyApplication.setparam(historicMessages, existingHistoryArray.toString());
                                    }


                                    // Géerer le cas où la réponse est vide

                                    if (result.equals("")){


                                        if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_en+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                        }
                                        else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_fr+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                        }
                                        else {
                                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
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
                                    // Play la suite de la réponse.
                                    else{ Log.e("MRAA","Detection_de_langue  -----"+teamChatBuddyApplication.getparam("Detection_de_langue").contains("yes"));
                                        Log.e("MRAA","nombreDeMotsCheck(result) -------"+teamChatBuddyApplication.nombreDeMotsCheck(result));
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
                                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+result+";SPLIT;"+String.valueOf(numberOfQuestion));
                                                                    } else {
                                                                        // Utiliser la première langue identifiée
                                                                        IdentifiedLanguage language = identifiedLanguages.get(0);
                                                                        String languageCode = language.getLanguageTag();
                                                                        float confidence = language.getConfidence();

                                                                        Log.i("MRA_idetifyLanguage", "Language: " + languageCode + ", Confidence: " + confidence);
                                                                        if (teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("")&& !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("0")) {
                                                                            if (Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties")) <= (confidence * 100)) {
                                                                                teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                                                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion));
                                                                            } else {
                                                                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion));
                                                                            }
                                                                        }
                                                                        else {
                                                                            teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion));
                                                                        }
                                                                    }
                                                                }
                                                            })
                                                    .addOnFailureListener(
                                                            new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+result+";SPLIT;"+String.valueOf(numberOfQuestion));
                                                                }
                                                            });
                                        }
                                        else{
                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion));
                                        }

                                    }

                                } catch (Exception e) {

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
                        }
                        else{
                            try {
                                if (response.errorBody()!= null){
                                    JsonObject errorLOG = new JsonObject();

                                    JsonObject errorCode = new JsonObject();
                                    errorCode.addProperty("ERROR CODE", response.code());

                                    // Obtenez la réponse JSON sous forme de chaîne
                                    String jsonString = response.errorBody().string();

                                    // Analysez la chaîne JSON en un objet JSON
                                    JSONObject jsonErrorContent = new JSONObject(jsonString);
                                    // Accédez à chaque élément et sa valeur individuellement
                                    JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                                    String message = errorObject.getString("message");
                                    String type = errorObject.getString("type");
                                    String param = errorObject.getString("param");
                                    String code = errorObject.getString("code");
                                    JsonObject reformErrorJson = new JsonObject();
                                    reformErrorJson.addProperty("message",message);
                                    reformErrorJson.addProperty("type",type);
                                    reformErrorJson.addProperty("param",param);
                                    reformErrorJson.addProperty("code",code);

                                    int checkErrorCode = response.code();
                                    // Calcul de la consommation openai de le cas d'echec
                                    if (checkErrorCode==500 ||checkErrorCode==503 || checkErrorCode==504){
                                        int inputTokens = teamChatBuddyApplication.getRequestTotalTokens(requestBody);
                                        teamChatBuddyApplication.calcul_consommation(teamChatBuddyApplication.getparam("model"),inputTokens,0);
                                    }

                                    errorCode.add("ERROR Body",reformErrorJson);
                                    errorLOG.add("OpenAIERROR",errorCode);
                                    //Mettre   le fichier le plus récent reçu
                                    String fileName = "ERROR-LOG";
                                    File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");


                                    try {
                                        if (file1.exists() && file1.isFile()) {
                                            file1.delete();
                                        }
                                        FileWriter fileWriter = new FileWriter(file1);
                                        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                        String jsonStringF=gson.toJson(errorLOG);
                                        fileWriter.write(jsonStringF);
                                        fileWriter.close();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    String errorTXT= new Date().toString()+", OpenAIERROR,ERROR CODE= "+response.code()+", ERROR Body{ message= "+message+", type= "+type+", param= "+param+", code= "+code+"}"+System.getProperty("line.separator");
                                    File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");


                                    try {

                                        FileWriter fileWriter = new FileWriter(file2,true);
                                        fileWriter.write(errorTXT);
                                        fileWriter.close();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }


                            catch (Exception e){
                                e.printStackTrace();
                            }

                            if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_en+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                            }
                            else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_fr+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                            }
                            else {
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
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
                    @Override
                    public void onFailure(Call call, Throwable t) {
                        teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                        teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                        Log.e(tag, "Réponse ChatGPT [Failure] : " + t);

                        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_en)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                        }
                        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_fr)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                        }
                        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_es)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                        }
                        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_de)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                        }
                        else {
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBotNoFound_en)).addOnSuccessListener(new OnSuccessListener<String>() {
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
                });
            }

        } catch (Exception e) {
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
                            Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication, teamChatBuddyApplication.getparam( "ChatGPT_url" ), 50 );
                            ApiEndpointInterface api = retrofit.create( ApiEndpointInterface.class );
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

                                RequestBody requestBody = RequestBody.create( MediaType.parse( "application/json; charset=utf-8" ), jsonParams.toString() );


                                Call call = api.getChatGPT( requestBody, "Bearer " + teamChatBuddyApplication.getparam( "openAI_API_Key" ), "application/json" );
//                chatGPTRunning = true;
                                call.enqueue( new Callback() {
                                    @Override
                                    public void onResponse(Call call, Response response) {
                                        teamChatBuddyApplication.setGetResponseTime( System.currentTimeMillis() );
                                        // Vérifie si la réponse est réussie
                                        if (response.isSuccessful()) {

                                            //logger CMD-recv.json
                                            try {
                                                File file_CMD_RECV = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/CMD-recv.json");
                                                if (file_CMD_RECV.exists() && file_CMD_RECV.isFile()) {
                                                    file_CMD_RECV.delete();
                                                }
                                                FileWriter fileWriter = new FileWriter(file_CMD_RECV);
                                                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                                String jsonString=gson.toJson(new JSONObject(response.body().toString()));
                                                fileWriter.write(jsonString);
                                                fileWriter.close();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            if (response.body().toString().contains("Failed to find bot")) {
                                                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                                                if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_en)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                                }
                                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_fr)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                                }
                                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueEs)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_es)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                                }
                                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueDe)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString(R.string.chatBotNoFound_de)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                                }
                                                else {
                                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBotNoFound_en)).addOnSuccessListener(new OnSuccessListener<String>() {
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
                                            else if (response.body().toString().contains("NoResponseServer")) {
                                                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                                                if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+chatBotServerNoResponce_en+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                                }
                                                else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+chatBotServerNoResponce_fr+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
                                                }
                                                else {
                                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
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
                                            // Traitement de la réponse :
                                            else {
                                                try {

                                                    JSONObject jsonObj = new JSONObject(response.body().toString());
                                                    // récupérer la réponse
                                                    JSONArray choicesArray = jsonObj.getJSONArray( "choices" );
                                                    JSONObject choiceObject = choicesArray.getJSONObject( 0 );
                                                    JSONObject messageObject = choiceObject.getJSONObject( "message" );

                                                    result = messageObject.getString( content );

                                                    Log.i("DLA", "Commande : Réponse "+ result);
                                                    if (teamChatBuddyApplication.getParamFromFile("COMMAND_histo","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("COMMAND_histo","TeamChatBuddy.properties").trim().equalsIgnoreCase("yes")) {
                                                        // Géerer le cas où la réponse est vide
                                                        JSONObject history1 = new JSONObject();
                                                        history1.put("role", "assistant");
                                                        history1.put(content, result);

                                                        existingHistoryArray.put(history1);
                                                        // Stocker la nouvelle version de l'historique
                                                        Log.i("DLA", "Commande : Réponse existingHistoryArray.length()= " + existingHistoryArray.length());
                                                        if (existingHistoryArray.length() > Integer.parseInt(teamChatBuddyApplication.getParamFromFile("COMMAND_maxdialog", "TeamChatBuddy.properties"))) {

                                                            existingHistoryArray.remove(1);
                                                            existingHistoryArray.remove(1);

                                                            teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());

                                                        } else {
                                                            teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());
                                                        }
                                                    }
                                                    if (result.equals( "" )) {
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
                                                    // Play la suite de la réponse.
                                                    else {
                                                        if(!commande.start_action( commande.regex( result ),numberOfQuestion,texte)) {
//                                                            returnCommand = false;
                                                            Log.e(tag, "COMMANDE NON RECONNUE : Start Emotion + ReponseFromChatGPT");
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
                                                        else if(!commande.regex(result).split( " " )[0].equals("CMD_PROMPT")) {
                                                            // get the historic commandes :

                                                            String jsonArrayString = teamChatBuddyApplication.getparam(historicMessages);

                                                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);

                                                            JSONObject Question = new JSONObject();
                                                            Question.put( "role", "user" );
                                                            Question.put( content, texte );
                                                            existingHistoryArray.put( Question );
                                                            teamChatBuddyApplication.setparam(historicMessages, existingHistoryArray.toString());
                                                        }
                                                    }


                                                } catch (Exception e) {
                                                    teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
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
                                        }


                                        else {

                                            try {

                                                //logger CMD-ERROR-LOG.json et CMD-ERROR-History.json
                                                if (response.errorBody()!= null){

                                                    //logger CMD-ERROR-LOG.json
                                                    JsonObject errorLOG = new JsonObject();
                                                    JsonObject errorCode = new JsonObject();
                                                    JsonObject reformErrorJson = new JsonObject();
                                                    errorCode.addProperty("ERROR CODE", response.code());
                                                    JSONObject jsonErrorContent = new JSONObject(response.errorBody().string());
                                                    JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                                                    reformErrorJson.addProperty("message",errorObject.getString("message"));
                                                    reformErrorJson.addProperty("type",errorObject.getString("type"));
                                                    reformErrorJson.addProperty("param",errorObject.getString("param"));
                                                    reformErrorJson.addProperty("code",errorObject.getString("code"));
                                                    errorCode.add("ERROR Body",reformErrorJson);
                                                    errorLOG.add("OpenAIERROR",errorCode);
                                                    File file_CMD_ERROR_LOG = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/CMD-ERROR-LOG.json");
                                                    if (file_CMD_ERROR_LOG.exists() && file_CMD_ERROR_LOG.isFile()) {
                                                        file_CMD_ERROR_LOG.delete();
                                                    }
                                                    FileWriter fileWriter = new FileWriter(file_CMD_ERROR_LOG);
                                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                                    String jsonStringF=gson.toJson(errorLOG);
                                                    fileWriter.write(jsonStringF);
                                                    fileWriter.close();

                                                    //logger CMD-ERROR-History.json
                                                    File file_CMD_ERROR_History = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/CMD-ERROR-History.txt");
                                                    FileWriter fileWriter2 = new FileWriter(file_CMD_ERROR_History,true);
                                                    fileWriter2.write(new Date().toString()+
                                                            ", OpenAIERROR,ERROR CODE= "+response.code()+
                                                            ", ERROR Body{ message= "+errorObject.getString("message")+
                                                            ", type= "+errorObject.getString("type")+
                                                            ", param= "+errorObject.getString("param")+
                                                            ", code= "+errorObject.getString("code")+
                                                            "}"
                                                            +System.getProperty("line.separator"));
                                                    fileWriter2.close();

                                                }
                                            }
                                            catch (Exception e){
                                                e.printStackTrace();
                                            }

//                            returnCommand = false;
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
//                        chatGPTRunning = false;
                                    }

                                    @Override
                                    public void onFailure(Call call, Throwable t) {
                                        teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                                        logErrorAPIHealysa("OpenAIGetCommandName",t.getMessage(),"onFailure");
                                        teamChatBuddyApplication.setGetResponseTime( System.currentTimeMillis() );
                                        Log.e( tag, "Réponse ChatGPT [Failure] : " + t );

                                        if (teamChatBuddyApplication.getLangue().getNom().equals( langueEn )) {
                                            teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString( R.string.chatBotNoFound_en ) + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                        } else if (teamChatBuddyApplication.getLangue().getNom().equals( langueFr )) {
                                            teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString( R.string.chatBotNoFound_fr ) + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                        } else if (teamChatBuddyApplication.getLangue().getNom().equals( langueEs )) {
                                            teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString( R.string.chatBotNoFound_es ) + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                        } else if (teamChatBuddyApplication.getLangue().getNom().equals( langueDe )) {
                                            teamChatBuddyApplication.notifyObservers( "CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + teamChatBuddyApplication.getString( R.string.chatBotNoFound_de ) + ";SPLIT;" + String.valueOf( numberOfQuestion ) + ";SPLIT;onError" );
                                        } else {
                                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate( teamChatBuddyApplication.getString( R.string.chatBotNoFound_en ) ).addOnSuccessListener( new OnSuccessListener<String>() {
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
//                        chatGPTRunning = false;
                                    }
                                } );
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

    public void getQuestionToDescribePicture(Bitmap bitmap,String prompt){
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
                Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication, teamChatBuddyApplication.getParamFromFile("Picture_Description_URL","TeamChatBuddy.properties"), 50);
                ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);

                try {
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
                                File capturedImage = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Images/capturedImage.png");
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

                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());

                    Call<JsonObject> call = api.getChatGPT(requestBody, "Bearer " + teamChatBuddyApplication.getparam( "openAI_API_Key" ), "application/json");
                    call.enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            teamChatBuddyApplication.setGetResponseTime( System.currentTimeMillis() );
                            if (response.isSuccessful()) {
                                JsonObject responseBody = response.body();
                                File fileImageDescription_recv = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ImageDescription-recv.json");

                                try {
                                    if (fileImageDescription_recv.exists() && fileImageDescription_recv.isFile()) {
                                        fileImageDescription_recv.delete();
                                    }
                                    FileWriter fileWriter = new FileWriter(fileImageDescription_recv);
                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                    String jsonString=gson.toJson(responseBody);
                                    fileWriter.write(jsonString);
                                    fileWriter.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                String chatgptResponse = responseBody.getAsJsonArray("choices")
                                        .get(0).getAsJsonObject()
                                        .getAsJsonObject("message")
                                        .get("content").getAsString();
                                Log.d("CameraX", "Response: " + chatgptResponse);
                                    try {


                                        String jsonArrayString = teamChatBuddyApplication.getparam(historicMessages);

                                        JSONArray existingHistoryArray = new JSONArray(jsonArrayString);

                                        JSONObject history1 = new JSONObject();
                                        history1.put("role", "assistant");
                                        history1.put(content, chatgptResponse);

                                        existingHistoryArray.put(history1);
                                        // Stocker la nouvelle version de l'historique

                                        teamChatBuddyApplication.setparam(historicMessages, existingHistoryArray.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + chatgptResponse);
                                Log.e("ChatGPT_Picture_Description", "Réponse ChatGPT photoDescription: "+chatgptResponse);
                                //Toast.makeText(getApplicationContext(), chatgptResponse, Toast.LENGTH_LONG).show();
                            } else {
                                if (response != null && response.errorBody() != null) {
                                    Log.e("ChatGPT_Picture_Description", "Réponse ChatGPT photoDescription [not successful] ");
                                    String jsonString = null;
                                    try {
                                        jsonString = response.errorBody().string();
                                        JSONObject jsonErrorContent = new JSONObject(jsonString);

                                        String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_TAKE_PHOTO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                        logErrorAPIHealysa("CMD_TAKE_PHOTO", errorTXT, "notOnFailure");
                                    } catch (IOException | JSONException e) {
                                        e.printStackTrace();
                                        Log.e("ChatGPT_Picture_Description", "Réponse ChatGPT photoDescription [not successful]1 catch" + e);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            Log.e("ChatGPT_Picture_Description", "Error: " + t.getMessage(), t);
                            logErrorAPIHealysa("CMD_TAKE_PHOTO",t.getMessage(),"onFailure");
                        }
                    });

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
            Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication, teamChatBuddyApplication.getparam( "ChatGPT_url" ), 50 );
            ApiEndpointInterface api = retrofit.create( ApiEndpointInterface.class );
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

                RequestBody requestBody = RequestBody.create( MediaType.parse( "application/json; charset=utf-8" ), jsonParams.toString() );
                Call<JsonObject> call = api.getChatGPT( requestBody, "Bearer " + teamChatBuddyApplication.getparam( "openAI_API_Key" ), "application/json" );

                call.enqueue( new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                        teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                        // teamChatTabletteApplication.setGetResponseTime(System.currentTimeMillis());
                        // teamChatTabletteApplication.setAlreadyGetAnswer(true);
                        // Vérifie si la réponse est réussie
                        Log.e( "MEHDI", "REponse ChatGPT " + response );
                        Log.e( "MEHDI", "REponse ChatGPT body " + response.body() );
                        Log.e( "MEHDI", "REponse ChatGPT successful " + response.isSuccessful() );
                        if (response.isSuccessful()) {
                            JSONObject jsonObj = null;
                            try {
                                jsonObj = new JSONObject( response.body().toString() );
                                JSONArray choicesArray = jsonObj.getJSONArray( "choices" );
                                JSONObject choiceObject = choicesArray.getJSONObject( 0 );
                                JSONObject messageObject = choiceObject.getJSONObject( "message" );
                                Log.e( "MEHDI", "Emotion detected------------------- " + messageObject.getString( "content" ) );

                                //Calcul de la consommation d'openai cas des commandes
                                JSONObject usages = jsonObj.getJSONObject("usage");
                                int inputTokens = Integer.parseInt(usages.getString("prompt_tokens"));
                                int outputTokens = Integer.parseInt(usages.getString("completion_tokens"));
                                Log.i("tokens", "result: "+result + "\ninputTokens: "+inputTokens + "\noutputTokens: "+outputTokens);

                                teamChatBuddyApplication.calcul_consommation(teamChatBuddyApplication.getParamFromFile("emotion_Model","TeamChatBuddy.properties"),inputTokens,outputTokens);


                                if (messageObject.getString( "content" ).contains( "(" )) {
                                    String content = messageObject.getString( "content" );
                                    String emotion = null;
                                    int startIndex = content.indexOf("(");
                                    int endIndex = content.indexOf(")");
                                    if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                                        emotion = content.substring(startIndex + 1, endIndex);
                                    }
                                    if (emotion != null) {
                                        Log.e( "MEHDI", "Emotion detected after------------------- " + emotion );
                                        teamChatBuddyApplication.notifyObservers( "getResponseF;SPLIT;gpt;SPLIT;" + emotion.trim().toLowerCase() + ";SPLIT;" + numberOfQuestion );
                                    } else {
                                        teamChatBuddyApplication.notifyObservers( "getResponseF;SPLIT;gpt;SPLIT;" + messageObject.getString( "content" ).trim().toLowerCase() + ";SPLIT;" + numberOfQuestion );
                                    }
                                } else {
                                    teamChatBuddyApplication.notifyObservers( "getResponseF;SPLIT;gpt;SPLIT;" + messageObject.getString( "content" ).trim().toLowerCase() + ";SPLIT;" + numberOfQuestion );
                                }

                                //Mettre  le dernier fichier json reçu à l’API


                                File file2 = new File( Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + "OpenAI-recv" + ".json" );

                                try {
                                    if (file2.exists() && file1.isFile()) {
                                        file2.delete();
                                        Log.v( "Json_API", "file deleted" );
                                    }

                                    FileWriter fileWriter = new FileWriter( file2 );
                                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                    String jsonString = gson.toJson( jsonObj );
                                    fileWriter.write( jsonString );
                                    fileWriter.close();
                                    Log.v( "Json_API", "new file added" );

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        else {

                            if (response.errorBody() != null) {
                                int checkErrorCode = response.code();
                                // Calcul de la consommation openai de le cas d'echec
                                if (checkErrorCode == 500 || checkErrorCode == 503 || checkErrorCode == 504) {
                                    int inputTokens = teamChatBuddyApplication.getRequestTotalTokens(requestBody);
                                    teamChatBuddyApplication.calcul_consommation(teamChatBuddyApplication.getParamFromFile("emotion_Model","TeamChatBuddy.properties"),inputTokens,0);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                        Log.e( "MEHDI", "Onfailure pendant la récupération de la réponse dataChatGPT : " + t );
                    }
                } );
            } catch (Exception e) {
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                Log.e( "MEHDI", "Exception pendant la récupération de la réponse dataChatGPT : " + e );

            }

        } else teamChatBuddyApplication.setUsingEmotions( false );
    }
    public void getSessionId(String question)  {
        teamChatBuddyApplication.setAlreadyChatting(true);

        //setLangugeAppAndRole(setting);
        Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("CustomGPT_url","TeamChatBuddy.properties"), 50);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        JSONObject jsonParams = new JSONObject();


        // define the model
        try {
            jsonParams.put("name", "test");
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                teamChatBuddyApplication.getCustomGPTStreamMode().reset();
            }
            teamChatBuddyApplication.setCustomGPTStreamMode(new CustomGPTStreamMode(activity));
            teamChatBuddyApplication.getCustomGPTStreamMode().sendRequestToGetSessionID(api,requestBody,question);
            Log.e("MEHDI","init CustomGPT");
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


    public void getInvitationFromChatGPT(String model,double temperature, int max_token, String prompt){

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

        Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication, teamChatBuddyApplication.getparam("ChatGPT_url"),50);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
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

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            Call call = api.getChatGPT( requestBody, "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"), "application/json");
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());

                    if(response.isSuccessful()) {

                        //logger Invit-recv.json
                        try {
                            File file_INVIT_RECV = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Invit-recv.json");
                            if (file_INVIT_RECV.exists() && file_INVIT_RECV.isFile()) {
                                file_INVIT_RECV.delete();
                            }
                            FileWriter fileWriter = new FileWriter(file_INVIT_RECV);
                            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                            String jsonString=gson.toJson(new JSONObject(response.body().toString()));
                            fileWriter.write(jsonString);
                            fileWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {

                            JSONObject jsonObj = new JSONObject(response.body().toString());
                            // récupérer la réponse
                            JSONArray choicesArray = jsonObj.getJSONArray( "choices" );
                            JSONObject choiceObject = choicesArray.getJSONObject( 0 );
                            JSONObject messageObject = choiceObject.getJSONObject( "message" );

                            result = messageObject.getString( content );

                            // Gérer le cas où la réponse est vide

                            if (result.equals("")){

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
                            // Play la suite de la réponse.
                            else{
                                Log.e("TEAMCHAT_BUDDY_TRACKING","Detection_de_langue  -----"+teamChatBuddyApplication.getparam("Detection_de_langue").contains("yes"));
                                Log.e("TEAMCHAT_BUDDY_TRACKING","nombreDeMotsCheck(result) -------"+teamChatBuddyApplication.nombreDeMotsCheck(result));
                                if (teamChatBuddyApplication.getparam("Detection_de_langue").contains("yes") && teamChatBuddyApplication.nombreDeMotsCheck(result)) {

                                    LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
                                    languageIdentifier.identifyPossibleLanguages(result)
                                            .addOnSuccessListener(
                                    new OnSuccessListener<List<IdentifiedLanguage>>() {
                                        @Override
                                        public void onSuccess(List<IdentifiedLanguage> identifiedLanguages) {
                                            if (identifiedLanguages.isEmpty()) {
                                                Log.i("MRA_idetifyLanguage", "Can't identify language.");
                                                teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;"+result);
                                            } else {
                                                // Utiliser la première langue identifiée
                                                IdentifiedLanguage language = identifiedLanguages.get(0);
                                                String languageCode = language.getLanguageTag();
                                                float confidence = language.getConfidence();

                                                Log.i("MRA_idetifyLanguage", "Language: " + languageCode + ", Confidence: " + confidence);
                                                if (teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("") && !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("0")) {
                                                    if (Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties")) <= (confidence * 100)) {
                                                        teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;"+result);
                                                    } else {
                                                        teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;"+result);
                                                    }
                                                }
                                                else {
                                                    teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;"+result);
                                                }
                                            }
                                        }
                                    })
                                            .addOnFailureListener(
                                                    new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;"+result);
                                                        }
                                                    });
                                }
                                else{
                                    teamChatBuddyApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;INVITATION;SPLIT;"+result);
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
                    else{
                        try {

                            //logger Invit-ERROR-LOG.json et Invit-ERROR-History.json
                            if (response.errorBody()!= null){

                                //logger Invit-ERROR-LOG.json
                                JsonObject errorLOG = new JsonObject();
                                JsonObject errorCode = new JsonObject();
                                JsonObject reformErrorJson = new JsonObject();
                                errorCode.addProperty("ERROR CODE", response.code());
                                JSONObject jsonErrorContent = new JSONObject(response.errorBody().string());
                                JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                                reformErrorJson.addProperty("message",errorObject.getString("message"));
                                reformErrorJson.addProperty("type",errorObject.getString("type"));
                                reformErrorJson.addProperty("param",errorObject.getString("param"));
                                reformErrorJson.addProperty("code",errorObject.getString("code"));
                                errorCode.add("ERROR Body",reformErrorJson);
                                errorLOG.add("OpenAIERROR",errorCode);
                                File file_INVIT_ERROR_LOG = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Invit-ERROR-LOG.json");
                                if (file_INVIT_ERROR_LOG.exists() && file_INVIT_ERROR_LOG.isFile()) {
                                    file_INVIT_ERROR_LOG.delete();
                                }
                                FileWriter fileWriter = new FileWriter(file_INVIT_ERROR_LOG);
                                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                                String jsonStringF=gson.toJson(errorLOG);
                                fileWriter.write(jsonStringF);
                                fileWriter.close();

                                //logger Invit-ERROR-History.json
                                File file_INVIT_ERROR_History = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Invit-ERROR-History.txt");
                                FileWriter fileWriter2 = new FileWriter(file_INVIT_ERROR_History,true);
                                fileWriter2.write(new Date().toString()+
                                        ", OpenAIERROR,ERROR CODE= "+response.code()+
                                        ", ERROR Body{ message= "+errorObject.getString("message")+
                                        ", type= "+errorObject.getString("type")+
                                        ", param= "+errorObject.getString("param")+
                                        ", code= "+errorObject.getString("code")+
                                        "}"
                                        +System.getProperty("line.separator"));
                                fileWriter2.close();

                            }
                        }
                        catch (Exception e){
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
                @Override
                public void onFailure(Call call, Throwable t) {
                    teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());

                    Log.e("TEAMCHAT_BUDDY_TRACKING", "Réponse ChatGPT [Failure] : " + t);

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
            });
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
            File fileImages = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/Images/sent");
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
