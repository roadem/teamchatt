package com.robotique.aevaweb.teamchatbuddy.models;

import androidx.annotation.Nullable;

public class ChatBot {
    private long id_num;
    private String nom;
    private boolean isChosen;

    public ChatBot(long id_num, String nom,Boolean isChosen) {
        this.id_num=id_num;
        this.nom = nom;
        this.isChosen = isChosen;
    }

    public long getId_num() {
        return id_num;
    }

    public void setId_num(long id_num) {
        this.id_num = id_num;
    }

    public String getNom() {
        return nom;
    }
    public boolean isChosen() {
        return isChosen;
    }

    public void setChosen(boolean chosen) {
        isChosen = chosen;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof ChatBot)) return false;
        return ((ChatBot) obj).id_num == this.id_num;
    }

}
