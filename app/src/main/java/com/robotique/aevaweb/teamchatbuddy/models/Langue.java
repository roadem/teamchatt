package com.robotique.aevaweb.teamchatbuddy.models;

import androidx.annotation.Nullable;

public class Langue {
    private int id;
    private String nom;
    private boolean isChosen;
    private String languageCode;

    public Langue(int id, String nom, boolean isChosen) {
        this.id = id;
        this.nom = nom;
        this.isChosen = isChosen;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
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
        if (!(obj instanceof Langue)) return false;
        return ((Langue) obj).id == this.id;
    }

    @Override
    public String toString() {
        return "Langue{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", isChosen=" + isChosen +
                '}';
    }
}
