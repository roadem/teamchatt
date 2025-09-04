package com.robotique.aevaweb.teamchatbuddy.models;

import java.io.InputStream;

public class HttpResponse {

    public int responseCode;
    public String body;
    public InputStream inputStream;

    public HttpResponse(int responseCode, String body, InputStream inputStream) {
        this.responseCode = responseCode;
        this.body = body;
        this.inputStream = inputStream;
    }

}
