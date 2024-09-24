package com.robotique.aevaweb.teamchatbuddy.utilis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface ApiEndpointInterface {

    @POST("v1/chat/completions")
    Call<JsonObject> getChatGPT(

            @Body RequestBody requestBody,
            @Header("Authorization") String apiKey,
            @Header("content-type") String contentType
    );

    @Streaming
    @POST("v1/chat/completions")
    Call<ResponseBody> getStreamChatGPT(

            @Body RequestBody requestBody,
            @Header("Authorization") String apiKey,
            @Header("content-type") String contentType
    );

    @POST("/api/v1/projects/{project_id}/conversations")
    Call<JsonObject> getSessionID(
            @Path(value = "project_id",encoded = true) String projectID,
            @Body RequestBody requestBody,
            @Header("accept")String accept,
            @Header("Authorization") String apiKey,
            @Header("content-type") String contentType
    );
    @POST("/api/v1/projects/{project_id}/conversations/{session_id}/messages")
    Call<ResponseBody> getCustomGPT(
            @Path(value = "project_id",encoded = true) String projectID,
            @Path(value = "session_id",encoded = true) String sessionID,
            @Query("lang") String language,
            @Body RequestBody requestBody,
            @Header("accept")String accept,
            @Header("Authorization") String apiKey,
            @Header("content-type") String contentType
    );

    /**
     *   -------------------------------  Healysa  ---------------------------------------------------------------
     */
    @POST("publicApi/auth/login")
    Call<JsonObject> getTokenHealysa(
            @Body RequestBody jsonList
    );

    @GET("api/users/account")
    Call<JsonObject> getIMEIHealya(
            @Header("Authorization") String token
    );

    @POST("api/deviceMessageProcessor/test/cmd")
    Call<String> runCmdHealysa(
            @Query( "product" ) String product,
            @Query( "imei" ) String imei,
            @Query( "cmd" ) String cmd,
            @Header("Authorization") String token
    );

    @GET("api/devicesData/chart")
    Call<JsonObject> getDataHealysa(
            @Query( "deviceImei" ) String deviceImei,
            @Query( "dateDebut" ) String dateDebut,
            @Query( "dateFin" ) String dateFin,
            @Query( "dataType" ) String dataType,
            @Query( "jsmFiltre" ) String jsmFiltre,
            @Header("Authorization") String token
    );

    @GET("api/beacon/{imei}")
    Call<JsonArray> getLocationBeaconHealysa(
            @Path( value = "imei", encoded = true) String deviceImei,
            @Header("Authorization") String token
    );

    @POST("publicApi/Tuya/{imei}")
    Call<JsonObject> postFeedCat(
            @Path( value = "imei", encoded = true) String deviceImei,
            @Body RequestBody jsonList,
            @Header("Authorization") String token
    );

    @GET("api/deviceParams/device/{imei}")
    Call<JsonArray> getPhoneNumberList(
            @Path( value = "imei", encoded = true) String deviceImei,
            @Header("Authorization") String token,
            @Query("paramsTypes") String paramsTypes
    );

    /**
     *   -------------------------------  Healysa FIN  ---------------------------------------------------------------
     */
    @POST("/v1.0/devices/{id}/commands")
    Call<JsonObject> getSwitchBotResult(
            @Path(value = "id",encoded = true) String id,
            @Header("Authorization") String token,
            @Body RequestBody jsonList
    );

    @GET("/data/2.5/weather")
    Call<JsonObject> getMeteoResult(
            @Query("q") String city,
            @Query("appid") String apiKey,
            @Query("lang") String language,
            @Query("units") String units
    );

    @POST("/Pillow/session/v2/session/create")
    Call<JsonObject> getRadioToken(
            @Body RequestBody jsonList,
            @Header("X-CSRFTOKEN") String csrf
    );
    @GET("/Pillow/search")
    Call<JsonObject> getRadioName(
            @Query("page") int page,
            @Query("pageSize") int pageSize,
            @Query("query") String query,
            @Header("Authorization") String token
    );
    @GET("/Pillow/radios/{radio_name}/play")
    Call<JsonObject> getRadioResponse(
            @Path( value = "radio_name", encoded = true) String deviceImei,
            @Header("Authorization") String token
    );

    /**
     *   -------------------------------  IMAGE  ---------------------------------------------------------------
     */

    @POST
    Call<ResponseBody> generateImage(
            @Url String endpoint,
            @Header("Authorization") String authorization,
            @Body RequestBody requestBody
    );

    /**
     *   -------------------------------  MUSIC  ---------------------------------------------------------------
     */
    @POST
    Call<ResponseBody> generateMusic(
            @Url String endpoint,
            @Body RequestBody requestBody
    );

    /**
     *   -------------------------------  JOKE  ---------------------------------------------------------------
     */

    // API pour récupérer des blagues en français
    @GET
    Call<JsonObject> getJoke(@Url String url, @Query("mode") String mode);

    // API pour récupérer des blagues en d'autres langues
    @GET
    Call<JsonObject> getJoke(@Url String url, @Query("blacklistFlags") String blacklistFlags, @Query("lang") String lang);
}