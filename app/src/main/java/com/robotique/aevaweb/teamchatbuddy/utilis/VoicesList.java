package com.robotique.aevaweb.teamchatbuddy.utilis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoicesList {

    private final Map<String, List<TtsGoogleC.VoiceSelectionRaw>> data = new HashMap<>();

    public void add(String languageCode, TtsGoogleC.VoiceSelectionRaw voice) {
        data.computeIfAbsent(languageCode, k -> new ArrayList<>()).add(voice);
    }

    public Map<String, java.util.List<TtsGoogleC.VoiceSelectionRaw>> getAll() {
        return data;
    }

    public java.util.List<TtsGoogleC.VoiceSelectionRaw> get(String languageCode) {
        return data.get(languageCode);
    }

    public TtsGoogleC.VoiceSelectionRaw getFirst(String languageCode) {
        java.util.List<TtsGoogleC.VoiceSelectionRaw> voices = data.get(languageCode);
        return (voices == null || voices.isEmpty()) ? null : voices.get(0);
    }

    /** Retourne la liste des noms de voix pour une langue */
    public List<String> getVoiceNames(String languageCode) {
        List<TtsGoogleC.VoiceSelectionRaw> voices = data.get(languageCode);

        if (voices == null) return new ArrayList<>();

        List<String> names = new ArrayList<>();
        for (TtsGoogleC.VoiceSelectionRaw v : voices) {
            names.add(v.name);
        }
        return names;
    }

    /** Retourne toutes les voix toutes langues confondues */
    public List<String> getAllVoiceNames() {
        List<String> all = new ArrayList<>();
        for (Map.Entry<String, List<TtsGoogleC.VoiceSelectionRaw>> entry : data.entrySet()) {
            for (TtsGoogleC.VoiceSelectionRaw v : entry.getValue()) {
                all.add(v.name);
            }
        }
        return all;
    }
}

