package com.robotique.aevaweb.teamchatbuddy.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;


/**
 * La classe BlueMicDevice est un modèle d'objet parcelable pour l'envoi du node via AIDL.
 * il contient :
 *        - Nom du BlueMic      (name)
 *        - Adresse du BlueMic  (tag)
 *        - Type du BlueMic     (type)
 *        - Etat du BlueMic     (state)
 */
public class BlueMicDevice implements Parcelable {


    private String name;
    private String tag;
    private String type;
    private String state;


    /**
     * Constructors
     */

    public BlueMicDevice(String name, String tag, String type, String state) {
        this.name = name;
        this.tag = tag;
        this.type = type;
        this.state = state;
    }


    /**
     * Getters and Setters
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    @Override
    public String toString() {
        return "BlueMicDevice{" +
                "name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", type='" + type + '\'' +
                ", state='" + state + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlueMicDevice that = (BlueMicDevice) o;
        return tag.equals(that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag);
    }

    /**
     * Parcelable implementation
     */

    protected BlueMicDevice(Parcel in) {
        name = in.readString();
        tag = in.readString();
        type = in.readString();
        state = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(tag);
        dest.writeString(type);
        dest.writeString(state);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BlueMicDevice> CREATOR = new Creator<BlueMicDevice>() {
        @Override
        public BlueMicDevice createFromParcel(Parcel in) {
            return new BlueMicDevice(in);
        }

        @Override
        public BlueMicDevice[] newArray(int size) {
            return new BlueMicDevice[size];
        }
    };

}