package com.robotique.aevaweb.teamchatbuddy.chatbotresponse;

import android.app.Activity;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Response;

public class CustomGPTStreamMode {
    private static final String TAG = "CustomGPT";
    private String word = "";
    private String phrase = "";
    private String text = "";
    private String errorMsg = "";
    private String phraseToPronounceWhenResumed;
    private String languageToUseWhenResumed;
    private final Queue<String> phrasesQueue = new LinkedList<>();
    private final Handler wordsHandler = new Handler(Looper.getMainLooper());
    private final Handler phrasesHandler = new Handler(Looper.getMainLooper());
    private Runnable wordsRunnable;
    private Runnable phrasesRunnable;
    private String currentDisplayedText = "";
    private final TeamChatBuddyApplication teamChatBuddyApplication;
    private final Activity activity;
    private String mediaType ="application/json";
    public boolean isReadyToSpeak = true;
    private boolean isReset = false;
    public boolean isError = false;
    private boolean isPaused = false;
    private boolean isFullResponseReceived = false;
    private boolean isDisplayFinished = true;
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";
    public static CountDownTimer responseTimeout;

    public CustomGPTStreamMode(Activity activity){
        this.activity = activity;
        this.teamChatBuddyApplication = (TeamChatBuddyApplication) activity.getApplicationContext();

    }

    public void sendRequestToGetSessionID(JSONObject jsonParams, String question) {
        new Thread(() -> {
            try {
                String endPoint = teamChatBuddyApplication.getParamFromFile("CustomGPT_ApiEndpoint_SessionID", "TeamChatBuddy.properties");
                endPoint = endPoint.replace("{project_id}", teamChatBuddyApplication.getparam("CustomGPT_Project_ID"));

                // Préparer l'URL complète
                String urlStr = teamChatBuddyApplication.getParamFromFile("CustomGPT_url", "TeamChatBuddy.properties") + endPoint;

                // Préparer les headers
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getparam("CustomGPT_API_Key"));
                headers.put("content-type", mediaType);

                // Appel via la classe centralisée
                HttpResponse httpResponse = HttpClientUtils.sendPost(urlStr, jsonParams.toString(), headers, 30000);

                if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300) {
                    try {
                        JSONObject jsonObj = new JSONObject(httpResponse.body);
                        JSONObject dataObject = jsonObj.getJSONObject("data");
                        Log.e("MEHDI", "response.isSuccessful() pendant la récupération de la SESSionID : " + httpResponse.body);
                        Log.e("MEHDI", "response.isSuccessful() pendant la récupération de la SESSionID : " + dataObject.getString("session_id"));
                        getResponseFromCustomGPT(question, dataObject.getString("session_id"));
                    } catch (JSONException e) {
                        teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                        teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                        Log.e("MEHDI", "JSONException pendant la récupération de la SESSionID : " + e.getMessage());
                        onErrorGettingSessionID("EXCEPTION", null);
                        e.printStackTrace();
                    }
                } else {
                    teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                    onErrorGettingSessionID("RESPONSE_NOT_SUCCESSFUL", null);
                }
            } catch (Exception e) {
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                Log.e("MEHDI", "Exception pendant la récupération de la SESSionID : " + e);
                onErrorGettingSessionID("EXCEPTION", null);
            }
        }).start();
    }

    private void getResponseFromCustomGPT(String question,String sessionId){
        try {
            List<String> WhisperLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Whisper");
            String codeLanguageWhisper = WhisperLangueCode.get(teamChatBuddyApplication.getLangue().getId()-1);
            JSONObject jsonParams = new JSONObject();
            // define the model
            jsonParams.put("prompt", question);
            jsonParams.put("custom_persona",setRole());
            jsonParams.put("chatbot_model", teamChatBuddyApplication.getParamFromFile("CustomGPT_model","TeamChatBuddy.properties"));
            jsonParams.put("stream",1);
            JSONObject jsonParams2= new JSONObject();
            jsonParams2=jsonParams;
            jsonParams2.put("projectID", teamChatBuddyApplication.getparam("CustomGPT_Project_ID"));
            jsonParams2.put("sessionID",sessionId.trim());
            jsonParams2.put("language", codeLanguageWhisper);
            //Mettre  le dernier fichier json envoyé à l’API
            String fileName = "CustomGPT-sent";
            File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");


            try {
                if (file1.exists() && file1.isFile()) {
                    file1.delete();
                }
                FileWriter fileWriter = new FileWriter(file1);
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String jsonString=gson.toJson(jsonParams2);
                fileWriter.write(jsonString);
                fileWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e("MEHDI","ennvoie requete CustomGPT  sessionID "+sessionId);

            if(responseTimeout!=null){
                responseTimeout.cancel();
                responseTimeout = null;
            }
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
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
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
                                BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
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
                });
            }
            Log.i("responseTimeout", " call CustomGPT API (StreamMode ON) ");
            new Thread(() -> {
                try{
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + teamChatBuddyApplication.getparam("CustomGPT_API_Key"));
                    headers.put("content-type", mediaType);

                    String endpoint = teamChatBuddyApplication.getParamFromFile("CustomGPT_ApiEndpoint","TeamChatBuddy.properties");
                    endpoint =endpoint.replace("{project_id}",teamChatBuddyApplication.getparam("CustomGPT_Project_ID")).replace("{session_id}",sessionId.trim());
                    String fullUrl = teamChatBuddyApplication.getParamFromFile("CustomGPT_url", "TeamChatBuddy.properties")
                            + endpoint + "?lang=" + codeLanguageWhisper;

                    Log.e("MEHDI", "envoie requete CustomGPT sessionID " + sessionId);

                    HttpResponse httpResponse = HttpClientUtils.sendPost(fullUrl, jsonParams.toString(), headers, 50000);
                    if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300) {
                        if (!isReset) {
                            try {
                                new Thread(() -> {
                                    handleStreamingResponse(httpResponse);
                                }).start();
                            } catch (Exception e) {
                                e.printStackTrace();
                                onErrorStreaming("EXCEPTION", null);
                            }
                        } else {
                            Log.w(TAG, "Ignore API response because reset() was called");
                        }
                    } else {
                        onErrorStreaming("RESPONSE_NOT_SUCCESSFUL", httpResponse);
                    }
                } catch (Exception e){
                    teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
                    onErrorStreaming("FAILURE",null);
                    Log.e("MEHDI", "onFailure pendant la récupération de la CustopGPT : " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
            Log.e("MEHDI", "Exception pendant la récupération de la CustopGPT : " + e);
            teamChatBuddyApplication.setGetResponseTime(System.currentTimeMillis());
            onErrorStreaming("EXCEPTION",null);
        }



    }
    private void handleStreamingResponse(HttpResponse httpResponse) {
        try {
            onStartStreaming();
            //get Pattern_Fin_Phrase from config file
            String pattern_fin_phrase = teamChatBuddyApplication.getParamFromFile("Pattern_End_Phrase","TeamChatBuddy.properties");
            if(pattern_fin_phrase == null || pattern_fin_phrase.isEmpty()) {
                pattern_fin_phrase = "[.]";
            }
            Log.w(TAG, "pattern_fin_phrase: " + pattern_fin_phrase);

            // Lecture du body ligne par ligne
            BufferedReader reader = new BufferedReader(new StringReader(httpResponse.body));
            String fileName = "CustomGPT-recv-stream";
            StringBuilder formattedContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && !isReset && !isError ) {
                Log.e("MEHDI", "response.isSuccessful() pendant la récupération de CustomGPT: " + line);
                //Récupération de : word/phrase/text :
                if (line.trim().startsWith("event:")){
                    formattedContent.append(line+";SPLIT;");
                }
                if (line.trim().startsWith("data:")) {
                    formattedContent.append(line);
                    formattedContent.append("\n");
                    String jsonData = line.substring("data:".length()).trim();
                    if (!jsonData.isEmpty()) {
                        JSONObject json = new JSONObject(jsonData);

                        if (!json.isNull("status")) {
                            String statut = json.getString("status");
                            if (statut.trim().equalsIgnoreCase("progress")){
                                String resp =json.getString("message") ;
                                onNewWord(resp);
                                if (resp.length() > 0) {
                                    if (!phrase.isEmpty()){
                                        Matcher matcher = Pattern.compile(pattern_fin_phrase).matcher(String.valueOf(phrase.charAt(phrase.length() - 1)));
                                        if (matcher.find() ){
                                            Log.e("Test_pattern1","ELSE: phrase "+phrase);
                                            onNewPhrase();
                                            phrase = "";
                                        }
                                        else if (phrase.length() >= 2) {
                                            // Extraire les caractères spécifiés dans le pattern
                                            // Split the string by "[" and then by "]"
                                            String[] parts = pattern_fin_phrase.split("\\[");

                                            // Check if the split results contain the needed part
                                            String firstBracketContent = "";
                                            if (parts.length > 1) {
                                                String[] subParts = parts[1].split("\\]");
                                                if (subParts.length > 0) {
                                                    firstBracketContent = "["+subParts[0]+"]";
                                                }
                                            }
                                            Log.e("Test_pattern1","firstBracketContent************** ="+firstBracketContent);
                                            Pattern characterPattern = Pattern.compile(firstBracketContent);
                                            Matcher matcher3 = characterPattern.matcher(pattern_fin_phrase);
                                            StringBuilder regexBuilder = new StringBuilder();
                                            // Construire une expression régulière dynamique pour les caractères de fin de phrase
                                            while (matcher3.find()) {
                                                regexBuilder.append("\\").append(matcher3.group());
                                            }

                                            Pattern searchPattern = Pattern.compile("[" + regexBuilder.toString() + "]");

                                            // Utiliser le pattern de recherche pour détecter la fin de la phrase
                                            Matcher searchMatcher = searchPattern.matcher(phrase);
                                            if (searchMatcher.find()){
                                                String caracFound =searchMatcher.group();
                                                Log.e("Test_pattern1", "Caractère de fin de phrase trouvé: " + caracFound);
                                                int index = phrase.indexOf(caracFound);
                                                if (index != -1 && index < phrase.length() - 1){
                                                    String subString = phrase.substring(index, index + 2); // récupérer deux caractères à partir de l'index trouvé
                                                    Log.e("Test_pattern1","ELSE: phrase2 les deux derniers caractères  "+subString);
                                                    Matcher matcher2 = Pattern.compile(pattern_fin_phrase).matcher(subString);
                                                    if (matcher2.find() ){
                                                        String beforeCharacter = phrase.substring(0, index + 1);
                                                        String afterCharacter = phrase.substring(index + 1);
                                                        phrase =beforeCharacter;
                                                        Log.e("Test_pattern1","ELSE: phrase2 "+phrase);
                                                        onNewPhrase();
                                                        phrase = afterCharacter;
                                                    }
                                                }else {
                                                    Log.e("Test_pattern1","ELSE: phrase2 le point n'est pas suivi d'un caractére  ");
                                                }
                                            }
                                        }
                                    }
                                    phrase += word;
                                    text += word;
                                    word = resp;
                                }
                                else {
                                    word += resp;
                                }
                            }
                            else if (statut.trim().equalsIgnoreCase("finish")){
                                phrase += word;
                                text += word;
                                onNewPhrase();
                                isFullResponseReceived = true;
                                //formattedContent.append(line);
                            }
                            else if (statut.trim().equalsIgnoreCase("error")){
                                phrase += word;
                                text += word;
                                onNewPhrase();
                                isFullResponseReceived = true;
                                //formattedContent.append(line);
                            }

                        }
                    }
                }
            }
            storeStreamResponse(fileName, formattedContent.toString());

        }
        catch (Exception e) {
            e.printStackTrace();
            onErrorStreaming("EXCEPTION",null);
        }
    }

    private void onErrorStreaming(String error,HttpResponse response){
        Log.e(TAG, "------------------ERROR-------------------");

        if(!isReset){

            if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
            phrasesHandler.removeCallbacksAndMessages(null);
            phrasesQueue.clear();

            if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
            wordsHandler.removeCallbacksAndMessages(null);

            teamChatBuddyApplication.stopTTS();
            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            }
            catch (Exception e){
                Log.e(TAG,"BuddySDK Exception  "+e);
            }

            SystemClock.sleep(1000);

            isReadyToSpeak = false;
            isError = true;

            if(error.equals("RESPONSE_NOT_SUCCESSFUL")){

                try {
                    if (response !=null &&response.body!= null){
                        Log.e("MEHDI","response.errorBody()!= null ");
                        JsonObject errorLOG = new JsonObject();

                        JsonObject errorCode = new JsonObject();
                        errorCode.addProperty("ERROR CODE", response.responseCode);

                        Log.e("MEHDI","response.code() "+response.responseCode);

                        // Obtenez la réponse JSON sous forme de chaîne
                        String jsonString = response.body;
                        // Analysez la chaîne JSON en un objet JSON
                        JSONObject jsonErrorContent = new JSONObject(jsonString);
                        Log.e("MEHDI","jsonErrorContent------ "+jsonErrorContent);
                        // Accédez à chaque élément et sa valeur individuellement
                        //JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                        String status = jsonErrorContent.getString("status");
                        String url = jsonErrorContent.getString("url");
                        JSONObject errorObject = jsonErrorContent.getJSONObject("data");
                        String message = errorObject.getString("message");
                        String code = errorObject.getString("code");
                        JsonObject reformErrorJson = new JsonObject();
                        reformErrorJson.addProperty("status",status);
                        reformErrorJson.addProperty("url",url);
                        reformErrorJson.addProperty("message",message);
                        reformErrorJson.addProperty("code",code);

                        errorCode.add("ERROR Body",reformErrorJson);
                        errorLOG.add("CustomGPTERROR",errorCode);
                        Log.e("MEHDI","errorLOG------ "+errorLOG);
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
                        String errorTXT= new Date().toString()+", CustomGPTERROR,ERROR CODE= "+response.responseCode+", ERROR Body{ message= "+message+", status= "+status+", url= "+url+", code= "+code+"}"+System.getProperty("line.separator");
                        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");


                        try {

                            FileWriter fileWriter = new FileWriter(file2,true);
                            fileWriter.write(errorTXT);
                            fileWriter.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        Log.e("MEHDI","response.errorBody()== null ");
                    }
                }


                catch (Exception e){
                    e.printStackTrace();
                    Log.e("MEHDI","jsonErrorContent Exception------ "+e.getMessage());
                }

                if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_fr","TeamChatBuddy.properties");
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties"))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG,"BuddySDK Exception  "+ex);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }
            else if(error.equals("FAILURE")){
                if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBotNoFound_en);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  teamChatBuddyApplication.getString(R.string.chatBotNoFound_fr);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBotNoFound_es);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Allemand")){
                    errorMsg =  teamChatBuddyApplication.getString(R.string.chatBotNoFound_de);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBotNoFound_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBotNoFound_en);
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }
            else{
                if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  teamChatBuddyApplication.getString(R.string.chatBot_ERROR_fr);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBot_ERROR_es);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Allemand")){
                    errorMsg =  teamChatBuddyApplication.getString(R.string.chatBot_ERROR_de);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en);
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG,"BuddySDK Exception  "+ex);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }

        }
    }
    private void onErrorGettingSessionID(String error,Response<JsonObject> response){
        Log.e(TAG, "------------------ERROR-------------------");

        if(!isReset){

            if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
            phrasesHandler.removeCallbacksAndMessages(null);
            phrasesQueue.clear();

            if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
            wordsHandler.removeCallbacksAndMessages(null);

            teamChatBuddyApplication.stopTTS();
            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            }
            catch (Exception e){
                Log.e(TAG,"BuddySDK Exception  "+e);
            }

            SystemClock.sleep(1000);

            isReadyToSpeak = false;
            isError = true;

            if(error.equals("RESPONSE_NOT_SUCCESSFUL")){

                try {
                    if (response !=null &&response.errorBody()!= null){
                        Log.e("MEHDI","response.errorBody()!= null ");
                        JsonObject errorLOG = new JsonObject();

                        JsonObject errorCode = new JsonObject();
                        errorCode.addProperty("ERROR CODE", response.code());

                        Log.e("MEHDI","response.code() "+response.code());

                        // Obtenez la réponse JSON sous forme de chaîne
                        String jsonString = response.errorBody().string();
                        // Analysez la chaîne JSON en un objet JSON
                        JSONObject jsonErrorContent = new JSONObject(jsonString);
                        Log.e("MEHDI","jsonErrorContent------ "+jsonErrorContent);
                        // Accédez à chaque élément et sa valeur individuellement
                        //JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                        String status = jsonErrorContent.getString("status");
                        String url = jsonErrorContent.getString("url");
                        JSONObject errorObject = jsonErrorContent.getJSONObject("data");
                        String message = errorObject.getString("message");
                        String code = errorObject.getString("code");
                        JsonObject reformErrorJson = new JsonObject();
                        reformErrorJson.addProperty("status",status);
                        reformErrorJson.addProperty("url",url);
                        reformErrorJson.addProperty("message",message);
                        reformErrorJson.addProperty("code",code);

                        errorCode.add("ERROR Body",reformErrorJson);
                        errorLOG.add("CustomGPTERROR",errorCode);
                        Log.e("MEHDI","errorLOG------ "+errorLOG);
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
                        String errorTXT= new Date().toString()+", CustomGPTERROR,ERROR CODE= "+response.code()+", ERROR Body{ message= "+message+", status= "+status+", url= "+url+", code= "+code+"}"+System.getProperty("line.separator");
                        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");


                        try {

                            FileWriter fileWriter = new FileWriter(file2,true);
                            fileWriter.write(errorTXT);
                            fileWriter.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        Log.e("MEHDI","response.errorBody()== null ");
                    }
                }


                catch (Exception e){
                    e.printStackTrace();
                    Log.e("MEHDI","jsonErrorContent Exception------ "+e.getMessage());
                }

                if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_fr","TeamChatBuddy.properties");
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties"))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = teamChatBuddyApplication.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG,"BuddySDK Exception  "+ex);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }
            else if(error.equals("FAILURE")){
                if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBotNoFound_en);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  teamChatBuddyApplication.getString(R.string.chatBotNoFound_fr);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBotNoFound_es);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Allemand")){
                    errorMsg =  teamChatBuddyApplication.getString(R.string.chatBotNoFound_de);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBotNoFound_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBotNoFound_en);
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG,"BuddySDK Exception  "+ex);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }
            else{
                if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  teamChatBuddyApplication.getString(R.string.chatBot_ERROR_fr);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBot_ERROR_es);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else if (teamChatBuddyApplication.getLangue().getNom().equals("Allemand")){
                    errorMsg =  teamChatBuddyApplication.getString(R.string.chatBot_ERROR_de);
                    teamChatBuddyApplication.setMessageError(true);
                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                        }
                        catch (Exception e){
                            Log.e(TAG,"BuddySDK Exception  "+e);
                        }
                    }
                    processPhrasesWithDelay();
                    pronouncePhrase(errorMsg);
                }
                else{
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = teamChatBuddyApplication.getString(R.string.chatBot_ERROR_en);
                                    teamChatBuddyApplication.setMessageError(true);
                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                        try {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                        catch (Exception ex){
                                            Log.e(TAG,"BuddySDK Exception  "+ex);
                                        }
                                    }
                                    processPhrasesWithDelay();
                                    pronouncePhrase(errorMsg);
                                }
                            });
                }
            }


        }
    }
    private void pronouncePhrase(String phraseToPronounce){
        if(!isReset){
            if (!teamChatBuddyApplication.isTimeoutExpired()) {
                Log.i(TAG, "TTS : [ " + phraseToPronounce + " ]");
                if (teamChatBuddyApplication.getparam("switch_visibility").contains("yes")) {
                    showPhrase(phraseToPronounce);
                }
                else{
                    isDisplayFinished = true;
                }
                teamChatBuddyApplication.notifyObservers("MODE_STREAM_SPEAK;SPLIT;"+phraseToPronounce);
            }
            else {
                Log.w(TAG, "Pause streaming until TTS is ready again [ " + phraseToPronounce + " ]");
                pauseStreaming(phraseToPronounce);
            }
        }
    }
    public void onTTSEnd(){
        Log.i(TAG,"TTS END");
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        }
        catch (Exception e){
            Log.e(TAG,"BuddySDK Exception  "+e);
        }
        isReadyToSpeak = true;
    }
    private void processPhrasesWithDelay() {
        if (!phrasesQueue.isEmpty() && isDisplayFinished) {
            if(isReadyToSpeak) {
                isReadyToSpeak = false;
                String phraseToPronounce = phrasesQueue.poll();
                if(phraseToPronounce != null){
                    if (teamChatBuddyApplication.getparam("Detection_de_langue").contains("yes") && teamChatBuddyApplication.nombreDeMotsCheck(phraseToPronounce)) {
                        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
                        languageIdentifier.identifyPossibleLanguages(phraseToPronounce)
                                .addOnSuccessListener(
                        new OnSuccessListener<List<IdentifiedLanguage>>() {
                            @Override
                            public void onSuccess(List<IdentifiedLanguage> identifiedLanguages) {
                                if (identifiedLanguages.isEmpty()) {
                                    Log.e("MRA_idetifyLanguage", "languageIdentifier : Can't identify language of : " + phraseToPronounce);
                                    pronouncePhrase(phraseToPronounce);
                                } else {
                                    // Utiliser la première langue identifiée
                                    IdentifiedLanguage language = identifiedLanguages.get(0);
                                    String languageCode = language.getLanguageTag();
                                    float confidence = language.getConfidence();

                                    Log.i("MRA_idetifyLanguage", "Language of : [ " + phraseToPronounce + " ] is : " + languageCode + ", Confidence: " + confidence);
                                    if (teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("")&& !teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("0")) {
                                        if (Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties")) <= (confidence * 100)) {
                                            teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                            pronouncePhrase(phraseToPronounce);
                                        } else {
                                            pronouncePhrase(phraseToPronounce);
                                        }
                                    }
                                    else {
                                        teamChatBuddyApplication.setLanguageDetected(languageCode.trim());
                                        pronouncePhrase(phraseToPronounce);
                                    }
                                }
                            }
                        })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        pronouncePhrase(phraseToPronounce);
                                    }
                                });
                    }
                    else{
                        pronouncePhrase(phraseToPronounce);
                    }
                }
            }
        }
        else{
            if( isDisplayFinished && ((isFullResponseReceived && isReadyToSpeak) || (isError && isReadyToSpeak) )){
                onFinishStreaming();
                teamChatBuddyApplication.notifyObservers("TTS_success");
                reset();
                return;
            }
        }
        phrasesHandler.postDelayed(phrasesRunnable = new Runnable() {
            @Override
            public void run() {
                processPhrasesWithDelay();
            }
        }, 50);
    }
    private void showPhrase(String phrase) {
        Log.e("MEHDI","sshowPhrase----------"+phrase.length());
        Log.e("MEHDI","sshowPhrase----------"+phrase);
        isDisplayFinished = false;
        if(isError) currentDisplayedText = "";
        final int totalLength = currentDisplayedText.length() + phrase.length();
        for (int i = 1; i <= phrase.length(); i++) {
            final String phraseToShow = currentDisplayedText + phrase.substring(0, i);
            wordsHandler.postDelayed(wordsRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.e("MEHDI","send notif----------"+phraseToShow);
                    teamChatBuddyApplication.notifyObservers("MODE_STREAM_TEXT;SPLIT;"+phraseToShow);
                    if (phraseToShow.length() == totalLength) {
                        isDisplayFinished = true;
                    }

                }
            }, i * 50L);
        }
        currentDisplayedText += phrase + " ";
    }
    private void onNewPhrase() {
        Log.w("MEHDI", "Phrase: " + phrase);
        phrasesQueue.add(phrase);
    }

    private void onNewWord(String resp) {
        Log.d("MEHDI", "Word  : " + resp);
    }
    public void resumeStreaming(){
        if(isPaused) {
            isPaused = false;
            pronouncePhrase(phraseToPronounceWhenResumed);
        }
    }

    private void pauseStreaming(String phraseToPronounceWhenResumed){
        isPaused = true;
        this.phraseToPronounceWhenResumed = phraseToPronounceWhenResumed;
    }
    private void onStartStreaming(){
        if(!isReset){
            Log.i(TAG, "------------------START-------------------");
            processPhrasesWithDelay();
        }
    }

    private void onFinishStreaming(){
        Log.i(TAG, "------------------END-------------------");
    }
    private void storeStreamResponse(String fileName, String formattedContent) {
        Log.w(TAG, "storeStreamResponse()");
        try{
            // Split the formattedContent into events based on line breaks
            String[] events = formattedContent.split("\n");
            for (int j=0;j<events.length;j++){
                Log.e(TAG,"events["+j+"] ="+events[j]);
            }


            // Create a JSON array to store the transformed data
            JSONArray jsonOutputArray = new JSONArray();

            // Iterate through each event in the array
            for (String event : events) {
                // Remove leading and trailing whitespaces
                event = event.trim();
                JSONObject currentEvent = new JSONObject();
                // Check if the event is not empty
                if (!event.isEmpty()) {
                    Log.e(TAG,"event ///// "+event);
                    String[] content=event.split(";SPLIT;");
                    Log.e(TAG,"content[0].substring(7))  "+content[0].substring(7));
                    currentEvent.put("event",content[0].substring(7));
                    JSONObject dataObject = new JSONObject(content[1].substring(6));
                    currentEvent.put("data",dataObject);
                }
                jsonOutputArray.put(currentEvent);
            }


            // Convert the JSON array to a formatted string
            // String jsonOutputString = jsonOutputArray.toString(2);

            // Now you can store the jsonOutputString as needed (e.g., in a file)
            File file1 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".json");

            try {
                if (file1.exists() && file1.isFile()) {
                    file1.delete();
                }
                FileWriter fileWriter = new FileWriter(file1);
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String jsonString=gson.toJson(jsonOutputArray);
                fileWriter.write(jsonString);
                fileWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"erreur creation du fichier "+e);
            }
        }
        catch(Exception e){
            Log.e(TAG, "storeStreamResponse() : "+e);
            e.printStackTrace();
        }
    }
    public void reset(){
        Log.i("MEHDI", "------------------reset-------------------");
        isReset = true;
        //reset phrasesQueue:
        if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();
        isReadyToSpeak = true;
        //reset wordsQueue:
        if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
        wordsHandler.removeCallbacksAndMessages(null);
        teamChatBuddyApplication.setCustomGPTStreamMode(null);
    }
    public String setRole(){
        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn) ){
            return teamChatBuddyApplication.getparam("CustomGPT_header_en");
        }
        else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr) ) {
            return teamChatBuddyApplication.getparam("CustomGPT_header_fr");
        }
        else {
            return teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_header_fr");
        }
    }
}