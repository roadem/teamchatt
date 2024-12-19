package com.robotique.aevaweb.teamchatbuddy.chatbotresponse;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
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
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.ModelType;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.utilis.ApiEndpointInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatGptStreamMode {

    private static final String TAG_STREAM = "MODE_STREAM";
    private static final String TAG_STREAM_USAGE = "MODE_STREAM_USAGE";
    private final TeamChatBuddyApplication app;
    private final Activity activity;
    private final JSONArray existingHistoryArray;
    private final Queue<String> phrasesQueue = new LinkedList<>();
    private final Handler wordsHandler = new Handler();
    private final Handler phrasesHandler = new Handler();
    private Runnable wordsRunnable;
    private Runnable phrasesRunnable;
    private String word = "";
    private String phrase = "";
    private String text = "";
    private String phraseToPronounceWhenResumed;
    private String errorMsg = "";
    private String currentDisplayedText = "";
    public boolean isReadyToSpeak = true;
    private boolean isReset = false;
    public boolean isError = false;
    private boolean isPaused = false;
    private boolean isFullResponseReceived = false;
    private boolean isDisplayFinished = true;
    private int requestTotalTokens = 0;


    public ChatGptStreamMode(Activity activity,JSONArray existingHistoryArray){
        this.activity = activity;
        this.app = (TeamChatBuddyApplication) activity.getApplicationContext();
        this.existingHistoryArray = existingHistoryArray;
    }

    public void sendRequest(ApiEndpointInterface api, RequestBody requestBody){
        try{
            //Calculer les tokens de la requête entière (entête + historique de discussion) :
            requestTotalTokens = getRequestTotalTokens(requestBody);
            //Envoyer la requête
            Call<ResponseBody> call_stream = api.getStreamChatGPT( requestBody, "Bearer " + app.getparam("openAI_API_Key"), "application/json");
            call_stream.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    app.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    app.setGetResponseTime(System.currentTimeMillis());
                    Log.i(TAG_STREAM,"onResponse response     : "+response);
                    Log.i(TAG_STREAM,"onResponse isSuccessful : "+response.isSuccessful());
                    Log.i(TAG_STREAM,"onResponse code         : "+response.code());
                    Log.i(TAG_STREAM,"onResponse message      : "+response.message());
                    if (response.isSuccessful() && response.body() != null) {
                        if(!isReset){
                            try {
                                new Thread(() -> {
                                    handleStreamingResponse(response);
                                }).start();
                            } catch (Exception e) {
                                e.printStackTrace();
                                onErrorStreaming("EXCEPTION",null);
                            }
                        }
                        else{
                            Log.w(TAG_STREAM,"Ignore API response because reset() was called");
                        }
                    }

                    else{
                        onErrorStreaming("RESPONSE_NOT_SUCCESSFUL",response);
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    app.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    app.setGetResponseTime(System.currentTimeMillis());
                    Log.e(TAG_STREAM,"onFailure Throwable     : "+t);
                    Log.e(TAG_STREAM,"onFailure message       : "+t.getMessage());
                    Log.e(TAG_STREAM,"onFailure cause         : "+t.getCause());
                    t.printStackTrace();
                    onErrorStreaming("FAILURE",null);
                }
            });
        }
        catch (Exception e){
            app.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
            app.setGetResponseTime(System.currentTimeMillis());
            e.printStackTrace();
            onErrorStreaming("EXCEPTION",null);
        }
    }

    private void handleStreamingResponse(Response<ResponseBody> response) {
        try {
            onStartStreaming();

            //get Pattern_Fin_Phrase from config file
            String pattern_fin_phrase = app.getParamFromFile("Pattern_End_Phrase","TeamChatBuddy.properties");
            if(pattern_fin_phrase == null || pattern_fin_phrase.isEmpty()) {
                pattern_fin_phrase = "[.]";
            }
            Log.w(TAG_STREAM, "pattern_fin_phrase: " + pattern_fin_phrase);

            InputStream inputStream = response.body().byteStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String fileName = "ChatGPT-recv-stream";
            StringBuilder formattedContent = new StringBuilder();
            String line;
            int responseTotalTokens = 0;// responseTotalTokens = number of non empty delta content
            while ((line = reader.readLine()) != null && !isReset && !isError) {
                //Initialiser formattedContent qui sera sauvegardé sur : ChatGPT-recv-stream.txt
                if (line.trim().isEmpty()) {}
                else if (line.contains("[DONE]")) formattedContent.append(line);
                else{
                    JSONObject jsonObject = new JSONObject(line.replace("data:", "").trim());
                    String formattedObject = jsonObject.toString(4);
                    formattedContent.append("data: ").append(formattedObject);
                    formattedContent.append("\n\n");
                }
                //Récupération de : word/phrase/text :
                if (line.trim().startsWith("data:")) {
                    String jsonData = line.substring("data:".length()).trim();
                    if(jsonData.contains("[DONE]")){
                        Log.i(TAG_STREAM, "------------------ FULL RESPONSE RECEIVED -------------------");
                        Log.i(TAG_STREAM, "Text  : " + text);
                        isFullResponseReceived = true;
                    }
                    else if (!jsonData.isEmpty()) {
                        JSONObject json = new JSONObject(jsonData);
                        if (!json.isNull("choices")) {
                            JSONArray choicesArray = json.getJSONArray("choices");
                            for (int j = 0; j < choicesArray.length(); j++) {
                                JSONObject choiceObject = choicesArray.getJSONObject(j);
                                if (choiceObject.has("delta")) {
                                    JSONObject deltaObject = choiceObject.getJSONObject("delta");
                                    if (deltaObject.has("content") && !deltaObject.getString("content").isEmpty()) {
                                        responseTotalTokens++;
                                    }
                                }
                                if (!choiceObject.isNull("finish_reason") && choiceObject.getString("finish_reason").equals("stop")) {
                                    Log.e("MODE_STREAM","IF:");
                                    phrase += word;
                                    text += word;
                                    onNewPhrase();
                                }
                                else {
                                    Log.e("Test_pattern","ELSE:");
                                    String resp = choiceObject.getJSONObject("delta").getString("content");
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
                                                Log.e("Test_pattern1","ContentAfterMatch************** ="+"[" + regexBuilder.toString() + "]");
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
                                        Log.e("Test_pattern","ELSE: else");
                                        word += resp;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stocker la nouvelle version de l'historique
            JSONObject history = new JSONObject();
            history.put("role", "assistant");
            history.put("content", text);
            existingHistoryArray.put(history);

            //vérifier limit tokens
            Log.d(TAG_STREAM_USAGE, "Response Total Tokens : " + responseTotalTokens);
            int totalTokens = requestTotalTokens + responseTotalTokens;
            Log.i(TAG_STREAM_USAGE, "-------> TOTAL Tokens : " + totalTokens);
            verifyLimitTokens(totalTokens);

            //-------Calculer la consommation OpenAI
            app.calcul_consommation(app.getparam("model"),requestTotalTokens,responseTotalTokens);

            // Sauvegarder formattedContent dans ChatGPT-recv-stream.txt
            storeStreamResponse(fileName, formattedContent.toString(), text);

        }
        catch (Exception e) {
            e.printStackTrace();
            onErrorStreaming("EXCEPTION",null);
        }
    }

    private int getRequestTotalTokens(RequestBody requestBody){
        try{
            int requestTotalTokens = 0;
            int totalTokensContent = 0;
            int totalTokensRole = 0;
            Optional<Encoding> encoding = app.getRegistry().getEncodingForModel(app.getparam("model"));
            if (encoding.isPresent()) {
                Encoding actualEncoding = encoding.get();
                Log.i(TAG_STREAM_USAGE, "Encoding is available for the model "+app.getparam("model") + " --> " + actualEncoding.getName());
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                String requestBodyString = buffer.readUtf8();
                Log.d(TAG_STREAM, "requestBody: " + requestBodyString);
                JSONObject json = new JSONObject(requestBodyString);
                if (json.has("messages")) {
                    JSONArray messagesArray = json.getJSONArray("messages");
                    String[] roleArray = new String[messagesArray.length()];
                    String[] contentArray = new String[messagesArray.length()];
                    for (int i = 0; i < messagesArray.length(); i++) {
                        JSONObject message = messagesArray.getJSONObject(i);
                        if (message.has("role") && message.has("content")) {
                            String role = message.getString("role");
                            String content = message.getString("content");
                            roleArray[i] = role;
                            contentArray[i] = content;
                            totalTokensContent += actualEncoding.countTokens(content);
                            totalTokensRole += actualEncoding.countTokens(role);
                        }
                    }
                    //Log.v(TAG_STREAM_USAGE, "contentArray: " + Arrays.toString(contentArray));
                    //Log.v(TAG_STREAM_USAGE, "Total Tokens for Content: " + totalTokensContent);
                    //Log.v(TAG_STREAM_USAGE, "roleArray: " + Arrays.toString(roleArray));
                    //Log.v(TAG_STREAM_USAGE, "Total Tokens for Role: " + totalTokensRole);
                    requestTotalTokens = totalTokensContent + totalTokensRole;
                    Log.d(TAG_STREAM_USAGE, "Request Total Tokens : " + requestTotalTokens);
                    return requestTotalTokens;
                }
                else {
                    Log.e(TAG_STREAM_USAGE, "No 'messages' array found in the JSON.");
                    return 0;
                }
            }
            else {
                Log.e(TAG_STREAM_USAGE, "Encoding is not available for the model "+app.getparam("model"));
                return 0;
            }
        }
        catch(Exception e){
            Log.e(TAG_STREAM_USAGE, "getRequestTotalTokens() ERROR : " + e);
            e.printStackTrace();
            return 0;
        }
    }

    private void verifyLimitTokens(int totalTokens){
        int MaxLimit = Integer.parseInt(app.getparam("Max_Tokens_req"));
        Log.w(TAG_STREAM_USAGE, "verifyLimitTokens() --> MaxLimit=" + MaxLimit + " / totalTokens= "+totalTokens);
        if(totalTokens > MaxLimit){
            Log.e(TAG_STREAM_USAGE, "verifyLimitTokens() : exceeding --> remove the 1st request and its response");
            existingHistoryArray.remove(1);
            existingHistoryArray.remove(1);
        }else {
            Log.i(TAG_STREAM_USAGE, "verifyLimitTokens() : not exceeding");
        }
        app.setparam("messages", existingHistoryArray.toString());
    }

    private void storeStreamResponse(String fileName, String formattedContent, String fullResponse) {
        Log.w(TAG_STREAM, "storeStreamResponse()");
        try{
            File file = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/" + fileName + ".txt");
            if (file.exists() && file.isFile()) {
                boolean result = file.delete();
                Log.i(TAG_STREAM, "storeStreamResponse() : file deleted : "+result);
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(formattedContent);
            fileWriter.write("\n");

            // Ajouter la réponse complète à la fin
            if (fullResponse != null && !fullResponse.isEmpty()) {
                fileWriter.write("\n--- Full Response ---\n");
                fileWriter.write(fullResponse);
            }

            fileWriter.close();
            Log.i(TAG_STREAM, "storeStreamResponse() : new file added");
        }
        catch(Exception e){
            Log.e(TAG_STREAM, "storeStreamResponse() : "+e);
            e.printStackTrace();
        }
    }

    private void showPhrase(String phrase) {
        isDisplayFinished = false;
        if(isError) currentDisplayedText = "";
        final int totalLength = currentDisplayedText.length() + phrase.length();
        for (int i = 1; i <= phrase.length(); i++) {
            final String phraseToShow = currentDisplayedText + phrase.substring(0, i);
            wordsHandler.postDelayed(wordsRunnable = new Runnable() {
                @Override
                public void run() {
                    app.notifyObservers("MODE_STREAM_TEXT;SPLIT;"+phraseToShow);
                    if (phraseToShow.length() == totalLength) {
                        isDisplayFinished = true;
                    }
                }
            }, i * 50L);
        }
        currentDisplayedText += phrase + " ";
    }

    private void processPhrasesWithDelay() {
        if (!phrasesQueue.isEmpty() && isDisplayFinished) {
            if(isReadyToSpeak) {
                isReadyToSpeak = false;
                String phraseToPronounce = phrasesQueue.poll();
                if(phraseToPronounce != null){
                    if (app.getparam("Detection_de_langue").contains("yes") && app.nombreDeMotsCheck(phraseToPronounce)) {
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
                                    if (app.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties")!=null && !app.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("")&& !app.getParamFromFile("Detection_confidence_rate","TeamChatBuddy.properties").trim().equals("0")) {
                                        if (Integer.parseInt(app.getParamFromFile("Detection_confidence_rate", "TeamChatBuddy.properties")) <= (confidence * 100)) {
                                            app.setLanguageDetected(languageCode.trim());
                                            pronouncePhrase(phraseToPronounce);
                                        } else {
                                            pronouncePhrase(phraseToPronounce);
                                        }
                                    }
                                    else {
                                        app.setLanguageDetected(languageCode.trim());
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
                app.notifyObservers("TTS_success");
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

    private void pronouncePhrase(String phraseToPronounce){
        if(!isReset){
            if (!app.isTimeoutExpired()) {
                Log.i(TAG_STREAM, "TTS : [ " + phraseToPronounce + " ]");
                if (app.getparam("switch_visibility").contains("yes")) {
                    showPhrase(phraseToPronounce);
                }
                else{
                    isDisplayFinished = true;
                }
                app.notifyObservers("MODE_STREAM_SPEAK;SPLIT;"+phraseToPronounce);
            }
            else {
                Log.w(TAG_STREAM, "Pause streaming until TTS is ready again [ " + phraseToPronounce + " ]");
                pauseStreaming(phraseToPronounce);
            }
        }
    }

    public void onTTSEnd(){
        Log.i(TAG_STREAM,"TTS END");
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        }
        catch (Exception e){
            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
        }
        isReadyToSpeak = true;
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

    private void onNewPhrase() {
        Log.w(TAG_STREAM, "Phrase: " + phrase);
        if (app.getParamFromFile("Response_filter","TeamChatBuddy.properties")!=null && !app.getParamFromFile("Response_filter","TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
            phrase = app.applyFilters(app.getParamFromFile("Response_filter","TeamChatBuddy.properties"),phrase);
        }
        Log.w(TAG_STREAM, "Phrase after response filter: " + phrase);
        phrasesQueue.add(phrase);
    }

    private void onNewWord(String resp) {
        Log.d(TAG_STREAM, "Word  : " + resp);
    }

    private void onStartStreaming(){
        if(!isReset){
            Log.i(TAG_STREAM, "------------------START-------------------");
            processPhrasesWithDelay();
        }
    }

    private void onFinishStreaming(){
        Log.i(TAG_STREAM, "------------------END-------------------");
    }

    private void onErrorStreaming(String error,Response<ResponseBody> response){
        Log.e(TAG_STREAM, "------------------ERROR-------------------");

        if(!isReset){

            if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
            phrasesHandler.removeCallbacksAndMessages(null);
            phrasesQueue.clear();

            if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
            wordsHandler.removeCallbacksAndMessages(null);

            app.stopTTS();
            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            }
            catch (Exception e){
                Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
            }

            SystemClock.sleep(1000);

            isReadyToSpeak = false;
            isError = true;

            if(error.equals("RESPONSE_NOT_SUCCESSFUL")){

                try {
                    if (response != null && response.errorBody() != null){
                        JsonObject errorLOG = new JsonObject();
                        JsonObject errorCode = new JsonObject();
                        errorCode.addProperty("ERROR CODE", response.code());
                        String jsonString = response.errorBody().string();
                        JSONObject jsonErrorContent = new JSONObject(jsonString);
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
                        errorCode.add("ERROR Body",reformErrorJson);
                        errorLOG.add("OpenAIERROR",errorCode);
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


                        if(code.equals("context_length_exceeded")){
                            //vérifier limit tokens
                            verifyLimitTokens(requestTotalTokens);
                        }

                        int checkErrorCode = response.code();
                        // Calcul de la consommation openai de le cas d'echec
                        if (checkErrorCode==500 ||checkErrorCode==503 || checkErrorCode==504){
                            app.calcul_consommation(app.getparam("model"),requestTotalTokens,0);
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }

                if (app.getLangue().getNom().equals("Anglais")){
                    errorMsg = app.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");
                }
                else if (app.getLangue().getNom().equals("Français")) {
                    errorMsg =  app.getParamFromFile("chatBotServerNoResponce_fr","TeamChatBuddy.properties");
                }
                else{
                    app.getEnglishLanguageSelectedTranslator().translate(app.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties"))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = app.getParamFromFile("chatBotServerNoResponce_en","TeamChatBuddy.properties");
                                }
                            });
                }
            }
            else if(error.equals("FAILURE")){
                if (app.getLangue().getNom().equals("Anglais")){
                    errorMsg = app.getString(R.string.chatBotNoFound_en);
                }
                else if (app.getLangue().getNom().equals("Français")) {
                    errorMsg =  app.getString(R.string.chatBotNoFound_fr);
                }
                else if (app.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = app.getString(R.string.chatBotNoFound_es);
                }
                else if (app.getLangue().getNom().equals("Allemand")){
                    errorMsg =  app.getString(R.string.chatBotNoFound_de);
                }
                else{
                    app.getEnglishLanguageSelectedTranslator().translate(app.getString(R.string.chatBotNoFound_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = app.getString(R.string.chatBotNoFound_en);
                                }
                            });
                }
            }
            else{
                if (app.getLangue().getNom().equals("Anglais")){
                    errorMsg = app.getString(R.string.chatBot_ERROR_en);
                }
                else if (app.getLangue().getNom().equals("Français")) {
                    errorMsg =  app.getString(R.string.chatBot_ERROR_fr);
                }
                else if (app.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = app.getString(R.string.chatBot_ERROR_es);
                }
                else if (app.getLangue().getNom().equals("Allemand")){
                    errorMsg =  app.getString(R.string.chatBot_ERROR_de);
                }
                else{
                    app.getEnglishLanguageSelectedTranslator().translate(app.getString(R.string.chatBot_ERROR_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = app.getString(R.string.chatBot_ERROR_en);
                                }
                            });
                }
            }

            app.setMessageError(true);
            if (!app.isOpenaialreadySwitchEmotion()) {
                try {
                    BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                }
                catch (Exception e){
                    Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                }
            }
            processPhrasesWithDelay();
            pronouncePhrase(errorMsg);
        }
    }

    public void reset(){
        Log.i(TAG_STREAM, "------------------reset-------------------");
        isReset = true;
        //reset phrasesQueue:
        if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();
        isReadyToSpeak = true;
        //reset wordsQueue:
        if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
        wordsHandler.removeCallbacksAndMessages(null);
        app.setChatGptStreamMode(null);
    }

}
