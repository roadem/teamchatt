package com.robotique.aevaweb.teamchatbuddy.models;

public class OpenAiInfo {
    private String modelName;
    private double inputPrice;
    private double outputPrice;
    private String additionalInfo;

    public OpenAiInfo(String modelName, double inputPrice, double outputPrice, String additionalInfo) {
        this.modelName = modelName;
        this.inputPrice = inputPrice;
        this.outputPrice = outputPrice;
        this.additionalInfo = additionalInfo;
    }

    public String getModelName() {
        return modelName;
    }

    public double getInputPrice() {
        return inputPrice;
    }

    public void setInputPrice(double inputPrice) {
        this.inputPrice = inputPrice;
    }

    public double getOutputPrice() {
        return outputPrice;
    }

    public void setOutputPrice(double outputPrice) {
        this.outputPrice = outputPrice;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }
}
